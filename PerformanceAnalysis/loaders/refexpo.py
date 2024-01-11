import os

from loaders.utils import load_csv_file


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

    class_relations = filter_nans(class_relations)
    method_relations = filter_nans(method_relations)

    return class_relations, method_relations


def filter_nans(relations):
    return [relation for relation in relations if 'nan' not in relation]
