import os
import re
import xml.etree.ElementTree as ET

from loaders.data_loader import DataLoader, EvaluationLevel


class DependencyFinderDataLoader(DataLoader):

    def get_name(self):
        return "Dependency Finder"

    def get_file_name(self):
        return 'dependencyFinder.xml'

    def support_evaluation_level(self, evaluation_level):
        return evaluation_level == EvaluationLevel.CLASS

    def load(self, evaluation_level):
        if evaluation_level == EvaluationLevel.CLASS:
            return self.load_class_data()
        else:
            return None

    def load_xml_file(self):
        # Check if the file exists
        file_path = self.get_file_path()

        if not self.file_exists():
            print(f"File not found: {file_path}")
            return None

        # Load the XML file using Pandas
        tree = ET.parse(file_path)
        root = tree.getroot()

        # Return the DataFrame
        return root

    def extract_class_name_from_feature(self, feature_reference, feature=True):
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

    def load_class_data(self):
        # Construct the full file path

        root = self.load_xml_file()

        class_inbound_relations = []

        for package in root.findall('.//package'):
            # Check if the package is confirmed
            if package.attrib.get('confirmed') != 'yes':
                continue  # Skip unconfirmed packages

            for class_element in package.findall('.//class'):
                # Check if the class is confirmed
                if class_element.attrib.get('confirmed') != 'yes':
                    continue  # Skip unconfirmed classes

                class_name = class_element.find('name').text if class_element.find(
                    'name') is not None else 'Unnamed Class'
                class_name = self.extract_class_name_from_feature(class_name, False)
                inbound_elements = class_element.findall('inbound')
                for inbound in inbound_elements:
                    # Check if the inbound reference is confirmed
                    if inbound.attrib.get('confirmed') != 'yes':
                        continue  # Skip unconfirmed inbound references

                    inbound_reference = inbound.text if inbound.text is not None else 'Unknown Reference'
                    if inbound.attrib.get('type') == 'feature':  # Check if it's a feature
                        inbound_reference = self.extract_class_name_from_feature(inbound_reference)
                    relation_str = f"{inbound_reference}->{class_name}"

                    relation_str = relation_str.replace("$", ".")

                    if not re.search(r'\.\d+?', relation_str):
                        class_inbound_relations.append(relation_str)

        return class_inbound_relations
