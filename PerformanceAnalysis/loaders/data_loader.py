import os
from enum import Enum


class EvaluationLevel(Enum):
    FILE = 1
    CLASS = 2
    METHOD = 3


class DataLoader(object):
    def __init__(self, project, dataFolder='data'):
        self.project = project
        self.dataFolder = dataFolder

    def file_exists(self):
        return os.path.isfile(self.get_file_path())

    def get_file_path(self):
        return os.path.join(self.dataFolder, self.project, self.get_file_name())

    def get_name(self):
        raise NotImplementedError

    def get_file_name(self):
        raise NotImplementedError

    def support_evaluation_level(self, evaluation_level: EvaluationLevel):
        raise NotImplementedError

    def load(self, evaluation_level: EvaluationLevel):
        raise NotImplementedError
