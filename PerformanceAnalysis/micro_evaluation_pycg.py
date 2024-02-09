import pandas as pd
import os
import json
import csv
from loaders.utils import load_csv_file

# Assuming df is your DataFrame

# Initialize an empty dictionary to store the grouped references
references = {}


# Function to parse and extract the required parts of the location strings
def parse_location(location):
    parts = location.split('.')
    feature = parts[0]
    feature_category = parts[1]
    remainder = '.'.join(parts[2:])
    return feature, feature_category, remainder


# Function to format file paths
def format_file_path(file_path):
    # Remove file extension
    file_path_no_ext = '.'.join(file_path.split('.')[:-1])
    # Convert "/" to "."
    formatted_path = file_path_no_ext.replace('/', '.')
    return formatted_path


# Function to update the references dictionary without repetitions
def update_references(feature, feature_category, remainder_source, remainder_target):
    if feature not in references:
        references[feature] = {}
    if feature_category not in references[feature]:
        references[feature][feature_category] = []
    reference = (remainder_source, remainder_target)
    # Check for duplicate before appending
    if reference not in references[feature][feature_category]:
        references[feature][feature_category].append(reference)


def read_callgraph_references(callgraph_path):
    with open(callgraph_path, 'r') as json_file:
        callgraph = json.load(json_file)
        # Extract and return (source, target) references

        return [(source, target) for source, targets in callgraph.items() for target in targets if
                "builtin" not in source and "builtin" not in target]


def get_feature_categories(base_path, feature):
    feature_path = os.path.join(base_path, feature)
    categories = [fc for fc in os.listdir(feature_path) if os.path.isdir(os.path.join(feature_path, fc))]
    return feature_path, categories


def compare_and_record_mismatches(feature, feature_category, callgraph_references, existing_references, mismatches):
    for reference in callgraph_references:
        if reference not in existing_references:
            mismatches.setdefault(feature, {}).setdefault(feature_category, []).append(reference)


def find_mismatches(base_path, references):
    mismatches = {}
    features = [f for f in os.listdir(base_path) if os.path.isdir(os.path.join(base_path, f))]
    for feature in features:
        feature_path, feature_categories = get_feature_categories(base_path, feature)
        for feature_category in feature_categories:
            callgraph_file = os.path.join(feature_path, feature_category, 'cleaned_callgraph.json')
            if os.path.isfile(callgraph_file):
                callgraph_references = read_callgraph_references(callgraph_file)
                existing_references = references.get(feature, {}).get(feature_category, [])
                compare_and_record_mismatches(feature, feature_category, callgraph_references, existing_references,
                                              mismatches)
    return mismatches


def print_prettified_mismatches(mismatches):
    if not mismatches:
        print("No mismatches found.")
        return

    for feature, feature_cats in mismatches.items():
        print(f"Feature: {feature}")
        for feature_category, refs in feature_cats.items():
            print(f"  Feature Category: {feature_category}")
            for ref in refs:
                print(f"    Mismatch: Source -> '{ref[0]}' | Target -> '{ref[1]}'")
        print("-" * 60)  # Separator for readability


def analyze_coverage_detailed(base_path, references):
    coverage_summary = {
        'total_feature_categories': 0,
        'covered_feature_categories': 0,
        'not_covered_feature_categories': 0,
        'details_per_feature': {}
    }

    features = [f for f in os.listdir(base_path) if os.path.isdir(os.path.join(base_path, f))]
    for feature in features:
        feature_path = os.path.join(base_path, feature)
        _, feature_categories = get_feature_categories(base_path, feature)
        feature_summary = {
            'total': 0,
            'covered': 0,
            'not_covered': 0
        }

        for feature_category in feature_categories:
            coverage_summary['total_feature_categories'] += 1
            feature_summary['total'] += 1

            category_path = os.path.join(feature_path, feature_category)
            callgraph_file = os.path.join(category_path, 'cleaned_callgraph.json')

            if os.path.isfile(callgraph_file):
                callgraph_references = read_callgraph_references(callgraph_file)
                existing_references = references.get(feature, {}).get(feature_category, [])

                # Check for coverage
                is_covered = any(ref in existing_references for ref in callgraph_references)
                if is_covered:
                    coverage_summary['covered_feature_categories'] += 1
                    feature_summary['covered'] += 1
                else:
                    coverage_summary['not_covered_feature_categories'] += 1
                    feature_summary['not_covered'] += 1

        coverage_summary['details_per_feature'][feature] = feature_summary

    return coverage_summary


def get_method_name_from_csv_row(row, tag):
    if pd.notna(row[f'{tag}MethodFull']):
        return row[f'{tag}MethodFull']
    else:
        name = format_file_path(row[f'{tag}Path'])
        if pd.notna(row[f'{tag}Structure']):
            name += f".{row[f'{tag}Structure']}"
        elif pd.notna(row[f'{tag}Method']):
            name += f".{row[f'{tag}Method']}"

        return name


snippets_base = '../MicroSuite-Python-PyCG'
df = load_csv_file('data/micro-pycg/refExpo.csv')

for index, row in df.iterrows():
    source = get_method_name_from_csv_row(row, 'source')
    target = get_method_name_from_csv_row(row, 'target')

    source_feature, source_feature_category, source_remainder = parse_location(source)
    target_feature, target_feature_category, target_remainder = parse_location(target)

    update_references(source_feature, source_feature_category, source_remainder, target_remainder)

print(references)

mismatches = find_mismatches(snippets_base, references)

print_prettified_mismatches(mismatches)

coverage_summary = analyze_coverage_detailed(snippets_base, references)
print("Overall Coverage Summary:")
print(
    f"Total Feature Categories: {coverage_summary['covered_feature_categories']}/{coverage_summary['total_feature_categories']}")
# print(f"Covered Feature Categories: {coverage_summary['covered_feature_categories']}")
# print(f"Not Covered Feature Categories: {coverage_summary['not_covered_feature_categories']}")
print("\nCoverage Details Per Feature:")
for feature, detail in coverage_summary['details_per_feature'].items():
    print(f"Feature: {feature} -> {detail['covered']}/{detail['total']}")
    # print(f"  Total Categories: {detail['total']}")
    # print(f"  Covered Categories: {detail['covered']}")
    # print(f"  Not Covered Categories: {detail['not_covered']}")


def analyze_coverage_with_total_precision_recall(base_path, references):
    # Initialize counters for total analysis
    total_tp = 0
    total_fp = 0
    total_fn = 0
    feature_analysis = {}

    features = [f for f in os.listdir(base_path) if os.path.isdir(os.path.join(base_path, f))]
    for feature in features:
        feature_path = os.path.join(base_path, feature)
        _, feature_categories = get_feature_categories(base_path, feature)

        # Initialize counters for the feature
        feature_tp = 0
        feature_fp = 0
        feature_fn = 0

        for feature_category in feature_categories:
            category_path = os.path.join(feature_path, feature_category)
            callgraph_file = os.path.join(category_path, 'cleaned_callgraph.json')

            if os.path.isfile(callgraph_file):
                callgraph_references = set(read_callgraph_references(callgraph_file))
                existing_references = set(references.get(feature, {}).get(feature_category, []))

                # Calculate TP, FP, FN for this feature_category
                tp = len(callgraph_references.intersection(existing_references))
                fp = len(existing_references.difference(callgraph_references))
                fn = len(callgraph_references.difference(existing_references))

                # Update feature-level counters
                feature_tp += tp
                feature_fp += fp
                feature_fn += fn

        # Calculate precision and recall for the feature
        precision = feature_tp / (feature_tp + feature_fp) if (feature_tp + feature_fp) > 0 else -1
        recall = feature_tp / (feature_tp + feature_fn) if (feature_tp + feature_fn) > 0 else -1

        # Update total counters
        total_tp += feature_tp
        total_fp += feature_fp
        total_fn += feature_fn

        # Store the analysis data for the feature
        feature_analysis[feature] = {'precision': precision, 'recall': recall}

    # Calculate total precision and recall across all features
    total_precision = total_tp / (total_tp + total_fp) if (total_tp + total_fp) > 0 else -1
    total_recall = total_tp / (total_tp + total_fn) if (total_tp + total_fn) > 0 else -1

    return feature_analysis, total_precision, total_recall


# Perform the analysis
feature_analysis, total_precision, total_recall = analyze_coverage_with_total_precision_recall(snippets_base,
                                                                                               references)

# Print total precision and recall
print(f"\nTotal -> P: {total_precision:.2f}, R: {total_recall:.2f}")

for feature, metrics in feature_analysis.items():
    print(f"Feature: {feature} -> P: {metrics['precision']:.2f}, Recall: {metrics['recall']:.2f}")


def analyze_coverage_with_averaged_precision_recall(base_path, references):
    # Initialize variables for calculating averages across all features
    overall_feature_precisions = []
    overall_feature_recalls = []

    feature_analysis = {}

    features = [f for f in os.listdir(base_path) if os.path.isdir(os.path.join(base_path, f))]
    for feature in features:
        feature_path = os.path.join(base_path, feature)
        _, feature_categories = get_feature_categories(base_path, feature)

        # Lists to store precision and recall for each feature_category
        feature_category_precisions = []
        feature_category_recalls = []

        for feature_category in feature_categories:
            category_path = os.path.join(feature_path, feature_category)
            callgraph_file = os.path.join(category_path, 'cleaned_callgraph.json')

            if os.path.isfile(callgraph_file):
                callgraph_references = set(read_callgraph_references(callgraph_file))
                existing_references = set(references.get(feature, {}).get(feature_category, []))

                # Calculate TP, FP, and FN
                tp = len(callgraph_references.intersection(existing_references))
                fp = len(existing_references.difference(callgraph_references))
                fn = len(callgraph_references.difference(existing_references))

                # Calculate precision and recall for this feature_category
                precision = tp / (tp + fp) if (tp + fp) > 0 else -1
                recall = tp / (tp + fn) if (tp + fn) > 0 else -1

                feature_category_precisions.append(precision)
                feature_category_recalls.append(recall)

        # Calculate the average precision and recall for the feature
        avg_feature_precision = sum(feature_category_precisions) / len(
            feature_category_precisions) if feature_category_precisions else -1
        avg_feature_recall = sum(feature_category_recalls) / len(
            feature_category_recalls) if feature_category_recalls else -1

        # Store the averaged precision and recall for this feature
        feature_analysis[feature] = {'precision': avg_feature_precision, 'recall': avg_feature_recall}

        # Add to overall calculations
        overall_feature_precisions.append(avg_feature_precision)
        overall_feature_recalls.append(avg_feature_recall)

    # Calculate total averaged precision and recall across all features
    total_avg_precision = sum(overall_feature_precisions) / len(
        overall_feature_precisions) if overall_feature_precisions else 0
    total_avg_recall = sum(overall_feature_recalls) / len(overall_feature_recalls) if overall_feature_recalls else 0

    return feature_analysis, total_avg_precision, total_avg_recall


# Perform the analysis
feature_analysis, total_avg_precision, total_avg_recall = analyze_coverage_with_averaged_precision_recall(snippets_base,
                                                                                                          references)

# Print total precision and recall
print(f"\nTotal -> P: {total_avg_precision:.2f}, R: {total_avg_recall:.2f}")

# Print the results
for feature, metrics in feature_analysis.items():
    print(f"Feature: {feature} -> P: {metrics['precision']:.2f}, Recall: {metrics['recall']:.2f}")




# def export_analysis_to_csv(base_path, references, output_file):
#     with open(output_file, mode='w', newline='') as file:
#         writer = csv.writer(file)
#         # Write the header row
#         writer.writerow(['Feature', 'Feature Category', 'Number of Edges', 'Number of Correct Identifications'])
#
#         features = [f for f in os.listdir(base_path) if os.path.isdir(os.path.join(base_path, f))]
#         for feature in features:
#             feature_path = os.path.join(base_path, feature)
#             _, feature_categories = get_feature_categories(base_path, feature)
#
#             for feature_category in feature_categories:
#                 category_path = os.path.join(feature_path, feature_category)
#                 callgraph_file = os.path.join(category_path, 'callgraph.json')
#
#                 if os.path.isfile(callgraph_file):
#                     callgraph_references = read_callgraph_references(callgraph_file)
#                     existing_references = references.get(feature, {}).get(feature_category, [])
#
#                     # Calculate the number of edges and correct identifications
#                     num_edges = len(callgraph_references)
#                     num_correct_identifications = sum(1 for ref in callgraph_references if ref in existing_references)
#
#                     # Write data row for this feature category
#                     writer.writerow([feature, feature_category, num_edges, num_correct_identifications])
#
#
# # Define the base path, references dictionary, and the desired output file path
# output_file = 'data/pycg-micro-report.csv'
#
# # Assume `references` is already defined as per previous discussions
# export_analysis_to_csv(snippets_base, references, output_file)
