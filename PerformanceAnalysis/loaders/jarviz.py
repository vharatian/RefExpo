import json
import os


def load_jarviz(project):
    # Construct the full file path
    file_path = os.path.join('data', project, 'jarviz.jsonl')

    class_relations = []
    method_relations = []
    # Load and process the file
    for json_object in load_jsonl_file(file_path):
        sourceClass, sourceMethod, targetClass, targetMethod = extract_parameters(json_object)

        # Generate the formatted strings and add them to the lists
        if sourceClass != targetClass:
            class_relations.append(f"{sourceClass}->{targetClass}")

        if sourceMethod != targetMethod:
            method_relations.append(f"{sourceClass}:{sourceMethod}->{targetClass}:{targetMethod}")

    return class_relations, method_relations


def load_jsonl_file(file_path):
    # Check if the file exists
    if not os.path.isfile(file_path):
        print(f"File not found: {file_path}")
        return None

    # Load the JSONL file
    with open(file_path, 'r') as file:
        for line in file:
            yield json.loads(line)


def extract_parameters(json_object):
    # Extract the required parameters
    sourceClass = json_object.get('sourceClass', 'N/A')
    sourceMethod = json_object.get('sourceMethod', 'N/A')
    targetClass = json_object.get('targetClass', 'N/A')
    targetMethod = json_object.get('targetMethod', 'N/A')

    return sourceClass, sourceMethod, targetClass, targetMethod
