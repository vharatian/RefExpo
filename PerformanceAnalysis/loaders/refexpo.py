import os

from loaders.data_loader import DataLoader, EvaluationLevel
import pandas as pd

from loaders.utils import load_csv_file


class RefExpoDataLoader(DataLoader):

    def get_name(self):
        return "RefExpo"

    def get_file_name(self):
        return 'refExpo.csv'

    def support_evaluation_level(self, evaluation_level: EvaluationLevel):
        return evaluation_level in [EvaluationLevel.CLASS, EvaluationLevel.METHOD]

    def load(self, evaluation_level: EvaluationLevel):
        if evaluation_level == EvaluationLevel.CLASS:
            return self.load_class_data()
        elif evaluation_level == EvaluationLevel.METHOD:
            return self.load_method_data()
        else:
            return None

    def load_class_data(self):
        class_data, _ = self.load_refexpo_data()
        return class_data

    def load_method_data(self):
        _, method_data = self.load_refexpo_data()
        return method_data

    def load_refexpo_data(self):
        refexpo_df = load_csv_file(self.get_file_path())
        if refexpo_df is None:
            return [], []

        # Process RefExpo data
        class_relations = [f"{row['SourceClass']}->{row['TargetClass']}" for _, row in refexpo_df.iterrows()]
        method_relations = [
            f"{row['SourceClass']}:{row['SourceMethod']}->{row['TargetClass']}:{row['TargetMethod']}" for _, row in
            refexpo_df.iterrows()]

        class_relations = self.filter_nans(class_relations)
        method_relations = self.filter_nans(method_relations)

        return class_relations, method_relations

    def filter_nans(self, relations):
        return [relation for relation in relations if 'nan' not in relation]
