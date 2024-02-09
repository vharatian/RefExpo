import json

from loaders.data_loader import DataLoader, EvaluationLevel


class PyCGDataLoader(DataLoader):

    def get_name(self):
        return "PyCG"

    def get_file_name(self):
        return "pycg.json"

    def support_evaluation_level(self, evaluation_level: EvaluationLevel):
        return evaluation_level == EvaluationLevel.METHOD

    def load(self, evaluation_level: EvaluationLevel):
        if evaluation_level == EvaluationLevel.METHOD:
            return self.load_data()

    def load_data(self):
        json_data = self.load_json_data()

        nodes = {}
        for _, file_node in json_data["modules"]["internal"].items():
            for key, namespace in file_node["namespaces"].items():
                name = namespace["namespace"]

                # check if method call
                if "()" in name:
                    name = self.create_method_name(name)
                    nodes[key] = name

        edges = []
        for edge in json_data["graph"]["internalCalls"]:
            if edge[0] in nodes and edge[1] in nodes:
                source = nodes[edge[0]]
                target = nodes[edge[1]]
                if source != target:
                    edges.append(f"{source}->{target}")

        edges = self.filter_python_management_methods(edges)

        return edges

    def filter_python_management_methods(self, edges):
        return [e for e in edges if "__" not in e]

    def create_method_name(self, name):
        if name.startswith("/"):
            name = name[1:]

        name = name.replace("()", "")
        name = name.replace("/", ".")

        # separate the method name from the class name
        # last_dot_index = name.rfind('.')
        # name = name[:last_dot_index] + ':' + name[last_dot_index + 1:]
        return name

    def load_json_data(self):
        file_path = self.get_file_path()
        if not self.file_exists():
            print(f"File not found: {file_path}")
            return None

        with open(file_path, 'r') as file:
            data = json.load(file)

        return data
