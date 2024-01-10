import argparse
import json
import os
import pandas as pd
from collections import Counter
import xml.etree.ElementTree as ET
import re


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


# load xml file
def load_xml_file(file_path):
    # Check if the file exists
    if not os.path.isfile(file_path):
        print(f"File not found: {file_path}")
        return None

    # Load the XML file using Pandas
    tree = ET.parse(file_path)
    root = tree.getroot()

    # Return the DataFrame
    return root


def extract_jarviz_parameters(json_object):
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
        sourceClass, sourceMethod, targetClass, targetMethod = extract_jarviz_parameters(json_object)

        # Generate the formatted strings and add them to the lists
        class_relations.append(f"{sourceClass}->{targetClass}")
        method_relations.append(f"{sourceClass}:{sourceMethod}->{targetClass}:{targetMethod}")

    class_relations = count_occurrences(class_relations)
    method_relations = count_occurrences(method_relations)

    return class_relations, method_relations


def load_refexpo_data(project):
    # Construct the full file path
    file_path = os.path.join('data', project, 'refExpo.csv')

    refexpo_df = load_csv_file(file_path)
    if refexpo_df is None:
        return [], []

    # Process RefExpo data
    class_relations = [f"{row['SourceClass']}->{row['TargetClass']}" for _, row in refexpo_df.iterrows()]
    method_relations = [
        f"{row['SourceClass']}:{row['SourceMethod']}->{row['TargetClass']}:{row['TargetMethod']}" for _, row in
        refexpo_df.iterrows()]

    class_relations = count_occurrences(class_relations)
    method_relations = count_occurrences(method_relations)

    return class_relations, method_relations


def extract_class_name_from_feature(feature_reference, feature=True):
    # Find the position of the opening parenthesis
    paren_index = feature_reference.find('(')
    if paren_index != -1:
        feature_reference = feature_reference[:paren_index]  # Truncate at the first parenthesis

    # Split the string by '.' and remove the method or property part
    if feature:
        parts = feature_reference.rsplit('.', 1)
        if len(parts) > 1:
            feature_reference = parts[0]

    # Split the string by '$' and remove feature part
    parts = feature_reference.split('$', 1)
    if len(parts) > 1:
        feature_reference = parts[0]

    return feature_reference


def load_dependencyfinder_data(project):
    # Construct the full file path
    file_path = os.path.join('data', project, 'dependencyFinder.xml')

    root = load_xml_file(file_path)

    class_inbound_relations = []

    for package in root.findall('.//package'):
        # Check if the package is confirmed
        if package.attrib.get('confirmed') != 'yes':
            continue  # Skip unconfirmed packages

        for class_element in package.findall('.//class'):
            # Check if the class is confirmed
            if class_element.attrib.get('confirmed') != 'yes':
                continue  # Skip unconfirmed classes

            class_name = class_element.find('name').text if class_element.find('name') is not None else 'Unnamed Class'
            class_name = extract_class_name_from_feature(class_name, False)
            inbound_elements = class_element.findall('inbound')
            for inbound in inbound_elements:
                # Check if the inbound reference is confirmed
                if inbound.attrib.get('confirmed') != 'yes':
                    continue  # Skip unconfirmed inbound references

                inbound_reference = inbound.text if inbound.text is not None else 'Unknown Reference'
                if inbound.attrib.get('type') == 'feature':  # Check if it's a feature
                    inbound_reference = extract_class_name_from_feature(inbound_reference)
                relation_str = f"{inbound_reference}->{class_name}"
                class_inbound_relations.append(relation_str)

    return count_occurrences(class_inbound_relations)


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


# Function to extract the base path and file extension
def extract_base_path_and_extension(file_column):
    match = re.search(r':\./([^:]+):', file_column)
    base_path = match.group(1) if match else ''
    extension = re.search(r'\.([^.]+)$', file_column)
    extension = extension.group(1) if extension else ''
    return base_path, extension


# Function to extract package and class name
def extract_package_and_class(column, base_path, extension):
    if base_path and extension:
        regex_pattern = rf':\./{base_path}:((?:[^:]+:)*)([^:]+)\.{extension}'
        match = re.search(regex_pattern, column)
        if match:
            package_name = match.group(1).rstrip(':').replace(':', '.')
            class_name = match.group(2)
            return f"{package_name}.{class_name}" if package_name and class_name else None
    return None


def process_row(row):
    from_base_path, from_extension = extract_base_path_and_extension(row['From File'])
    to_base_path, to_extension = extract_base_path_and_extension(row['To File'])
    from_full_name = extract_package_and_class(row['From'], from_base_path, from_extension)
    to_full_name = extract_package_and_class(row['To'], to_base_path, to_extension)

    return f"{from_full_name}->{to_full_name}" \
        if from_full_name and to_full_name and from_full_name != to_full_name \
        else None


# Apply the processing to each row

def load_sonargraph_data(project):
    file_path = os.path.join('data', project, 'sonargraph.csv')

    df = load_csv_file(file_path)
    return count_occurrences(df.apply(process_row, axis=1).dropna())


def main():
    # Set up argument parser
    parser = argparse.ArgumentParser(description='Load a JSONL file from a specified project folder.')
    parser.add_argument('-p', '--project', type=str, required=True,
                        help='The name of the project folder under the data directory')

    # Parse arguments
    args = parser.parse_args()
    project = args.project

    # Load the data
    sonargraph_class_relations = load_sonargraph_data(project)
    # print(sonargraph_class_relations)

    # dependencyfinder_class_relations = load_dependencyfinder_data(project)
    # jarviz_class_relations, jarviz_method_relations = load_jarviz(project)
    refexpo_class_relations, refexpo_method_relations = load_refexpo_data(project)

    # Example usage in your main function
    common_relations, unique_refexpo, unique_jarviz, count_diff = compare_relations(refexpo_class_relations,
                                                                                    sonargraph_class_relations)
    #
    # # print("Common Relations:")
    # # print(common_relations)
    #
    # print("\nUnique to RefExpo:")
    print(len(unique_refexpo))
    #
    # print("\nUnique to Jarviz:")
    print(len(unique_jarviz))
    print(unique_jarviz)

    # print("\nCount Differences in Common Relations:")
    # for relation, counts in count_diff.items():
    #     print(f"{relation} - RefExpo: {counts['refexpo']}, Jarviz: {counts['jarviz']}")


if __name__ == "__main__":
    main()
