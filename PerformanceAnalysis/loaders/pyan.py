import re

from loaders.data_loader import DataLoader, EvaluationLevel


class PyanDataLoader(DataLoader):

    def get_name(self):
        return "Pyan"

    def get_file_name(self):
        return "pyan.dot"

    def support_evaluation_level(self, evaluation_level: EvaluationLevel):
        return evaluation_level == EvaluationLevel.METHOD

    def load(self, evaluation_level: EvaluationLevel):
        if evaluation_level == EvaluationLevel.METHOD:
            return self.load_data()

    def load_data(self):
        edges = self.get_edges_from__dot_file(self.get_file_path())
        return edges

    def get_edges_from__dot_file(self, file_path):
        extracted_lines = []
        with open(file_path, 'r') as file:
            for line in file:
                if "->" in line:
                    processed_line = line[:line.find("[")]
                    processed_line = processed_line.strip()

                    source, target = processed_line.split("->")
                    source = self.convert_locator(source)
                    target = self.convert_locator(target)
                    if source == target:
                        continue

                    if ".set" in target or ".print" in target:
                        continue

                    extracted_lines.append(f"{source}->{target}")

        return extracted_lines

    def get_path_mapping(self):
        paths = self.get_paths()
        mapping = {}
        for path in paths:
            if path.endswith(".py"):
                path = path.replace(".py", "")
                key = path
                if "/" in key:
                    key = key[key.find("/") + 1:]
                value = path.replace("/", ".")
                mapping[key] = value

        return mapping

    def convert_locator(self, locator):
        locator = locator.strip()

        locator = re.sub(r'____(\w+)__', r'.##\1##', locator)
        locator = re.sub(r'____(\w+)____', r'.##\1##.', locator)
        locator = re.sub(r'__(\w+)____', r'##\1##.', locator)
        locator = locator.replace("__", ".")
        locator = locator.replace("##", "__")

        return locator
