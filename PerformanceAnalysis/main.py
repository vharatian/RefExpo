import argparse
import json
import os
import pandas as pd
from collections import Counter


def count_occurrences(refexpo_class_relations):
    return Counter(refexpo_class_relations)


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

    class_relations = count_occurrences(class_relations)
    method_relations = count_occurrences(method_relations)

    return class_relations, method_relations


def load_refexpo(project):
    # Construct the full file path
    file_path = os.path.join('data', project, 'refExpo.csv')

    refexpo_df = load_csv_file(file_path)

    # Process RefExpo data
    class_relations = [f"{row['SourceClass']}->{row['TargetClass']}" for _, row in refexpo_df.iterrows()]
    method_relations = [
        f"{row['SourceClass']}:{row['SourceMethod']}->{row['TargetClass']}:{row['TargetMethod']}" for _, row in
        refexpo_df.iterrows()]

    class_relations = count_occurrences(class_relations)
    method_relations = count_occurrences(method_relations)

    return class_relations, method_relations


def filter_nans(relations):
    return [relation for relation in relations if 'nan' not in relation]

def compare_relations(refexpo_relations, jarviz_relations):
    # Convert Counter objects to sets for easy comparison
    refexpo_set = set(filter_nans(refexpo_relations.keys()))
    jarviz_set = set(filter_nans(jarviz_relations.keys()))

    # Common elements
    common = refexpo_set & jarviz_set

    # Unique to RefExpo
    unique_refexpo = refexpo_set - jarviz_set

    # Unique to Jarviz
    unique_jarviz = jarviz_set - refexpo_set

    # Comparing counts
    count_differences = {}
    for relation in common:
        count_differences[relation] = {
            'refexpo': refexpo_relations[relation],
            'jarviz': jarviz_relations[relation]
        }

    return common, unique_refexpo, unique_jarviz, count_differences

def main():
    # Set up argument parser
    parser = argparse.ArgumentParser(description='Load a JSONL file from a specified project folder.')
    parser.add_argument('-p', '--project', type=str, required=True,
                        help='The name of the project folder under the data directory')

    # Parse arguments
    args = parser.parse_args()
    project = args.project

    jarviz_class_relations, jarviz_method_relations = load_jarviz(project)
    refexpo_class_relations, refexpo_method_relations = load_refexpo(project)

    # Print the collected relations
    # print("Class Relations:")
    # print(refexpo_class_relations)
    # print("\nMethod Relations:")
    # print(refexpo_method_relations)

    # Example usage in your main function
    common_relations, unique_refexpo, unique_jarviz, count_diff = compare_relations(refexpo_class_relations,
                                                                                    jarviz_class_relations)

    # print("Common Relations:")
    # print(common_relations)

    print("\nUnique to RefExpo:")
    print(len(unique_refexpo))

    print("\nUnique to Jarviz:")
    print(len(unique_jarviz))

    # print("\nCount Differences in Common Relations:")
    # for relation, counts in count_diff.items():
    #     print(f"{relation} - RefExpo: {counts['refexpo']}, Jarviz: {counts['jarviz']}")


if __name__ == "__main__":
    main()
