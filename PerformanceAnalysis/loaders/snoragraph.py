import os
import re

from loaders.data_loader import DataLoader, EvaluationLevel
from loaders.utils import load_csv_file


class SonargraphDataLoader(DataLoader):

    def get_name(self):
        return "Sonargraph"

    def get_file_name(self):
        return 'sonargraph.csv'

    def support_evaluation_level(self, evaluation_level: EvaluationLevel):
        return evaluation_level == EvaluationLevel.CLASS

    def load(self, evaluation_level: EvaluationLevel):
        if evaluation_level == EvaluationLevel.CLASS:
            return self.load_class_data()

        return None

    def extract_base_path_and_extension(self, file_column):
        match = re.search(r':\./([^:]+):', file_column)
        base_path = match.group(1) if match else ''
        extension = re.search(r'\.([^.]+)$', file_column)
        extension = extension.group(1) if extension else ''
        return base_path, extension

    # Function to extract package and class name
    def extract_package_and_class(self, column, base_path, extension):
        if base_path and extension:
            regex_pattern = rf':\./{base_path}:((?:[^:]+:)*)([^:]+)\.{extension}'
            match = re.search(regex_pattern, column)
            if match:
                package_name = match.group(1).rstrip(':').replace(':', '.')
                class_name = match.group(2)
                return f"{package_name}.{class_name}" if package_name and class_name else None
        return None

    def process_row(self, row):
        from_base_path, from_extension = self.extract_base_path_and_extension(row['From File'])
        to_base_path, to_extension = self.extract_base_path_and_extension(row['To File'])
        from_full_name = self.extract_package_and_class(row['From'], from_base_path, from_extension)
        to_full_name = self.extract_package_and_class(row['To'], to_base_path, to_extension)

        return f"{from_full_name}->{to_full_name}" \
            if from_full_name and to_full_name and from_full_name != to_full_name \
            else None

    def load_class_data(self):
        file_path = self.get_file_path()

        df = load_csv_file(file_path)
        return df.apply(self.process_row, axis=1).dropna()
