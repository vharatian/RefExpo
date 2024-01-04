import argparse
import json
import os
import pandas as pd

def load_jsonl_file(file_path):

    # Check if the file exists
    if not os.path.isfile(file_path):
        print(f"File not found: {file_path}")
        return None

    # Load the JSONL file
    with open(file_path, 'r') as file:
        for line in file:
            yield json.loads(line)


def load_csv_file(file_path):

    # Check if the file exists
    if not os.path.isfile(file_path):
        print(f"File not found: {file_path}")
        return None

    # Load the CSV file using Pandas
    df = pd.read_csv(file_path, dtype=str)

    # Return the DataFrame
    return df

def extract_parameters(json_object):
    # Extract the required parameters
    sourceClass = json_object.get('sourceClass', 'N/A')
    sourceMethod = json_object.get('sourceMethod', 'N/A')
    targetClass = json_object.get('targetClass', 'N/A')
    targetMethod = json_object.get('targetMethod', 'N/A')

    return sourceClass, sourceMethod, targetClass, targetMethod

def load_jarviz(project):
    # Initialize lists for storing relations

    # Construct the full file path
    file_path = os.path.join('data', project, 'jarviz.jsonl')

    class_relations = []
    method_relations = []
    # Load and process the file
    for json_object in load_jsonl_file(file_path):
        sourceClass, sourceMethod, targetClass, targetMethod = extract_parameters(json_object)

        # Generate the formatted strings and add them to the lists
        class_relations.append(f"{sourceClass}->{targetClass}")
        method_relations.append(f"{sourceClass}:{sourceMethod}->{targetClass}:{targetMethod}")
    return class_relations, method_relations


def load_refexpo(project):
    # Construct the full file path
    file_path = os.path.join('data', project, 'refExpo.csv')

    refexpo_df = load_csv_file(file_path)

    # Process RefExpo data
    class_relations = [f"{row['SourceClass']}->{row['DestinationClass']}" for _, row in refexpo_df.iterrows()]
    method_relations = [
        f"{row['SourceClass']}:{row['SourceMethod']}->{row['DestinationClass']}:{row['DestinationMethod']}" for _, row in
        refexpo_df.iterrows()]

    return class_relations, method_relations


def main():
    # Set up argument parser
    parser = argparse.ArgumentParser(description='Load a JSONL file from a specified project folder.')
    parser.add_argument('-p', '--project', type=str, required=True, help='The name of the project folder under the data directory')

    # Parse arguments
    args = parser.parse_args()
    project = args.project

    # jarviz_class_relations, jarviz_method_relations = load_jarviz(project)
    refexpo_class_relations, refexpo_method_relations = load_refexpo(project)

    # Print the collected relations
    print("Class Relations:")
    print(refexpo_class_relations)
    print("\nMethod Relations:")
    print(refexpo_method_relations)



if __name__ == "__main__":
    main()
