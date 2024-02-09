import json
import re

from loaders.data_loader import DataLoader, EvaluationLevel


class JarvizDataLoader(DataLoader):

    def get_name(self):
        return "Jarviz"

    def get_file_name(self):
        return 'jarviz.jsonl'

    def support_evaluation_level(self, evaluation_level: EvaluationLevel):
        return evaluation_level in [EvaluationLevel.CLASS, EvaluationLevel.METHOD]

    def load(self, evaluation_level: EvaluationLevel):
        if evaluation_level == EvaluationLevel.CLASS:
            return self.load_class_data()
        elif evaluation_level == EvaluationLevel.METHOD:
            return self.load_method_data()

        return None

    def load_method_data(self):
        class_data, method_data = self.load_jarviz_data()
        return method_data

    def load_class_data(self):
        class_data, method_data = self.load_jarviz_data()
        return class_data

    def load_jarviz_data(self):
        # Construct the full file path

        class_relations = []
        method_relations = []
        # Load and process the file
        for json_object in self.load_jsonl_file():
            sourceClass, sourceMethod, targetClass, targetMethod = self.extract_parameters(json_object)

            # Generate the formatted strings and add them to the lists
            if sourceClass != targetClass:
                relation = f"{sourceClass}->{targetClass}"
                relation = relation.replace("$", ".")

                # removing the numbers
                if not re.search(r'\.\d+?', relation):
                    class_relations.append(relation)
                # class_relations.append(relation)

            if sourceMethod != targetMethod:
                method_relations.append(f"{sourceClass}:{sourceMethod}->{targetClass}:{targetMethod}")

        return class_relations, method_relations

    def load_jsonl_file(self):
        # Check if the file exists
        file_path = self.get_file_path()
        if not self.file_exists():
            print(f"File not found: {file_path}")
            return None

        # Load the JSONL file
        with open(file_path, 'r') as file:
            for line in file:
                yield json.loads(line)

    def extract_parameters(self, json_object):
        # Extract the required parameters
        sourceClass = json_object.get('sourceClass', 'N/A')
        sourceMethod = json_object.get('sourceMethod', 'N/A')
        targetClass = json_object.get('targetClass', 'N/A')
        targetMethod = json_object.get('targetMethod', 'N/A')

        return sourceClass, sourceMethod, targetClass, targetMethod
