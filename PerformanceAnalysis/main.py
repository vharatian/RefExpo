import argparse
from collections import Counter

from loaders.dependency_finder import load_dependencyfinder_data
from loaders.jarviz import load_jarviz
from loaders.refexpo import load_refexpo_data
from loaders.snoragraph import load_sonargraph_data


def compare_relations(*lists):
    unique_sets = [set(lst) for lst in lists]
    num_lists = len(unique_sets)

    # Check if the number of all elements is equal to sum of results
    # print(len(set(itertools.chain.from_iterable(lists))))

    # Count appearances across lists
    appearances = Counter(string for lst in unique_sets for string in lst)

    # Elements that appear in all lists
    shared_elements = set.intersection(*unique_sets)
    shared_count = len(shared_elements)

    # Remove elements that appear in all lists
    for element in shared_elements:
        del appearances[element]

    # Count of elements grouped by the number of lists they appear in
    grouped_appearances_count = Counter(appearances.values())

    # Remove count of elements that appear in only one list
    if 1 in grouped_appearances_count:
        del grouped_appearances_count[1]

    # Collect unique elements (not in other lists) for each list
    unique_elements_lists = []
    for current_set in unique_sets:
        other_sets = unique_sets.copy()
        other_sets.remove(current_set)
        unique_elements = current_set - set().union(*other_sets)
        unique_elements_lists.append(unique_elements)

    return unique_elements_lists, shared_count, grouped_appearances_count

# Function to extract the base path and file extension


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

    dependencyfinder_class_relations = load_dependencyfinder_data(project)
    jarviz_class_relations, jarviz_method_relations = load_jarviz(project)
    refexpo_class_relations, refexpo_method_relations = load_refexpo_data(project)

    # Example usage in your main function
    unique_elements_lists, shared_count, grouped_appearances_count = compare_relations(refexpo_class_relations,
                                                                               sonargraph_class_relations,
                                                                               jarviz_class_relations,
                                                                               dependencyfinder_class_relations)

    for i, unique_elements in enumerate(unique_elements_lists, start=1):
        print(f"Unique elements in list {i} -> {len(unique_elements)}")

    print(f"Number of shared elements: {shared_count}")
    print(
        f"Count of elements by number of lists they appear in (excluding those in all lists and unique ones): {grouped_appearances_count}")


if __name__ == "__main__":
    main()
