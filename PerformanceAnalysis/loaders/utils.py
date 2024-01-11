import os
from collections import Counter

import pandas as pd


def load_csv_file(file_path):
    # Check if the file exists
    if not os.path.isfile(file_path):
        print(f"File not found: {file_path}")
        return None

    # Load the CSV file using Pandas
    df = pd.read_csv(file_path, dtype=str)

    # Return the DataFrame
    return df


def count_occurrences(refexpo_class_relations):
    return Counter(refexpo_class_relations)
