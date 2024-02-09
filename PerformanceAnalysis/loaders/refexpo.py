import os

from loaders.data_loader import DataLoader, EvaluationLevel
import pandas as pd

from loaders.utils import load_csv_file


class RefExpoDataLoader(DataLoader):

    def get_name(self):
        return "RefExpo"

    def get_file_name(self):
        return 'refExpo.csv'

    def support_evaluation_level(self, evaluation_level: EvaluationLevel):
        return evaluation_level in [EvaluationLevel.CLASS, EvaluationLevel.METHOD]

    def load(self, evaluation_level: EvaluationLevel):
        if evaluation_level == EvaluationLevel.CLASS:
            return self.load_class_data()
        elif evaluation_level == EvaluationLevel.METHOD:
            return self.load_method_data()
        else:
            return None

    def load_class_data(self):
        class_data, _ = self.load_refexpo_data()
        return class_data

    def load_method_data(self):
        _, method_data = self.load_refexpo_data()
        return method_data

    def load_refexpo_data(self):
        refexpo_df = load_csv_file(self.get_file_path())
        if refexpo_df is None:
            return [], []

        # Process RefExpo data
        class_relations = []
        method_relations = []
        for _, row in refexpo_df.iterrows():
            source_method = self.get_structure(row, True, False)
            target_method = self.get_structure(row, False,False)
            if source_method is not None and target_method is not None and source_method != target_method:
                method_relations.append(f"{source_method}->{target_method}")

            source_class = self.get_class(row, True)
            target_class = self.get_class(row, False)
            if source_method != None and target_class != None and source_class != target_class:
                class_relations.append(f"{source_class}->{target_class}")

        class_relations = self.filter_nans(class_relations)
        method_relations = self.filter_nans(method_relations)

        # method_relations = self.filter_python_management_methods(method_relations)
        class_relations = [cr for cr in class_relations if 'None' not in cr]

        return class_relations, method_relations

    def filter_python_management_methods(self, method_relations):
        return [e for e in method_relations if "__" not in e]

    def get_class(self, row, source=True, ignore_nan=True):
        indicator_tag = self.get_indicator_tag(source)

        class_name = row[f'{indicator_tag}ClassFull']
        if f"{class_name}" == 'nan' and ignore_nan:
            return None

        return class_name

    def get_structure(self, row, source=True, ignore_nan=True):
        indicator_tag = self.get_indicator_tag(source)
        package_name = self.extract_package_or_module_name(row[f'{indicator_tag}Path'])

        structure = row[f'{indicator_tag}Structure']
        if f"{structure}" == 'nan' or structure == '':
            structure = self.get_method(row, source)

        if structure is None:
            if ignore_nan:
                return None

            return package_name

        return f"{package_name}.{structure}"

    def get_method(self, row, source=True):
        indicator_tag = self.get_indicator_tag(source)

        method_name = row[f'{indicator_tag}Method']
        if f"{method_name}" == 'nan':
            return None

        class_name = self.get_class(row, source, ignore_nan=False)

        if class_name is not None:
            return f"{class_name}.{method_name}"

        return method_name

    def get_indicator_tag(self, source):
        return 'source' if source else 'target'

    def filter_nans(self, relations):
        return [relation for relation in relations if 'nan' not in relation]

    def extract_package_or_module_name(self, relative_path):
        # Determine the file type (Java or Python)
        if relative_path.endswith(".java"):
            return self.extrac_java_package_name(relative_path)
        elif relative_path.endswith(".py"):
            return self.extrac_python_module_name(relative_path)
        else:
            return None

    def extrac_java_package_name(self, relative_path):
        file_extension = ".java"
        base_directory = "src/main/java/"

        # Remove the base directory and file extension
        if relative_path.startswith(base_directory):
            relative_path = relative_path[len(base_directory):]
        if relative_path.endswith(file_extension):
            relative_path = relative_path[:-len(file_extension)]

            # Replace path separators with dots
        package_or_module_name = relative_path.replace("/", ".").replace("\\", ".")

        # Remove the class or module name (last segment after the last dot)
        package_or_module_name = ".".join(package_or_module_name.split(".")[:-1])

        return package_or_module_name

    def extrac_python_module_name(self, relative_path):
        file_extension = ".py"

        if relative_path.endswith(file_extension):
            relative_path = relative_path[:-len(file_extension)]

            # Replace path separators with dots
        package_or_module_name = relative_path.replace("/", ".").replace("\\", ".")

        return package_or_module_name
