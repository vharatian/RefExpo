import os
import re

from loaders.utils import load_csv_file


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

def load_sonargraph_data(project):
    file_path = os.path.join('data', project, 'sonargraph.csv')

    df = load_csv_file(file_path)
    return df.apply(process_row, axis=1).dropna()
