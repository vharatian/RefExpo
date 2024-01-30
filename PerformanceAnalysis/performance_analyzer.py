import argparse
from collections import Counter

from matplotlib import pyplot as plt

from loaders.data_loader import EvaluationLevel
from loaders.dependency_finder import DependencyFinderDataLoader
from loaders.jarviz import JarvizDataLoader
from loaders.refexpo import RefExpoDataLoader
from loaders.snoragraph import SonargraphDataLoader
from loaders.pycg import PyCGDataLoader
from pyvenn import venn


def compare_relations(lists):
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


def draw_venn_diagram(lists, set_labels):
    # Create a list of sets
    data = [set(lst) for lst in lists]

    # Generate labels for the sets (optional)
    labels = venn.get_labels(data, fill=['number', 'logic'])

    # Create a Venn diagram based on the data
    diagrams = {
        2: venn.venn2,
        3: venn.venn3,
        4: venn.venn4,
        5: venn.venn5,
        6: venn.venn6,
    }

    fig, _ = diagrams[len(data)](labels, names=set_labels)

    # Set the title and display the diagram
    fig.show()

def main():
    # Set up argument parser
    parser = argparse.ArgumentParser(description='Load a JSONL file from a specified project folder.')
    parser.add_argument('-p', '--project', type=str, required=True,
                        help='The name of the project folder under the data directory')

    parser.add_argument('-e', '--evaluation', type=str, required=True, choices=['FILE', 'CLASS', 'METHOD'],
                        help='The evaluation level to load data from')

    # Parse arguments
    args = parser.parse_args()
    project = args.project
    evaluation_level = EvaluationLevel[args.evaluation]

    data_loaders = [
        SonargraphDataLoader(project),
        DependencyFinderDataLoader(project),
        JarvizDataLoader(project),
        RefExpoDataLoader(project),
        PyCGDataLoader(project)
    ]

    supporting_loaders = [dl for dl in data_loaders if
                          dl.support_evaluation_level(evaluation_level) and dl.file_exists()]

    data = [dl.load(evaluation_level) for dl in supporting_loaders]

    # Example usage in your main function
    unique_elements_lists, shared_count, grouped_appearances_count = compare_relations(data)

    set_labels = [dl.get_name() for dl in supporting_loaders]

    for i, unique_elements in enumerate(unique_elements_lists):
        print(f"Unique elements in list {set_labels[i]} -> {len(unique_elements)}")

    print(f"Number of shared elements: {shared_count}")
    print(f"Count of elements by number of lists they appear in")
    for key, value in grouped_appearances_count.items():
        print(f"\t{key} -> {value}")

    draw_venn_diagram(data, set_labels)

if __name__ == "__main__":
    main()
