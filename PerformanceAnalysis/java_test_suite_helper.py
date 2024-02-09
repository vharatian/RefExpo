import glob
import os
import re


def list_md_files(path):
    if not path.endswith(os.sep):
        path += os.sep
    md_files = glob.glob(path + '*.md')
    return md_files


def extract_level2_headers_and_java_blocks(content):
    headers = set()
    java_blocks = []  # List of tuples (header, filename, code)
    current_header = None
    code_block = None

    code_block = None  # Use None to indicate we're not currently reading a code block
    for line in content.splitlines():
        if line.startswith('## '):
            current_header = line[3:].strip().lower().replace(' ', '_')
            headers.add(current_header)
        elif line.strip().startswith('```java'):
            code_block = []
            filename = None
        elif line.strip() == '```' and current_header and code_block is not None:
            # Join the code block, removing the package line, and add to java_blocks
            code = '\n'.join(code_block)
            code = re.sub(r'^package\s+.*;$', '', code, flags=re.MULTILINE)  # Remove existing package declaration
            if filename:
                java_blocks.append((current_header, filename, code))
            code_block = None  # Reset for the next block
        elif code_block is not None:
            if line.startswith('//') and 'java' in line:
                filename = line.split('/')[-1].strip()
            code_block.append(line)

    return headers, java_blocks


def create_folder_and_java_files(base_path, file_path, headers, java_blocks):
    main_folder_name = os.path.splitext(os.path.basename(file_path))[0].lower()
    main_folder_path = os.path.join(base_path, main_folder_name)
    os.makedirs(main_folder_path, exist_ok=True)

    for header in headers:
        os.makedirs(os.path.join(main_folder_path, header), exist_ok=True)

    for header, filename, code in java_blocks:
        if header and filename:
            subfolder_path = os.path.join(main_folder_path, header)
            package_name = f"{main_folder_name}.{header}".replace('_', '.')
            # Add the new package declaration at the top of the file content
            code_with_package = f"package {package_name};\n\n{code}"
            java_file_path = os.path.join(subfolder_path, filename)
            with open(java_file_path, 'w', encoding='utf-8') as java_file:
                java_file.write(code_with_package)


def process_md_files(path):
    md_files = list_md_files(path)
    for file_path in md_files:
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read()
        headers, java_blocks = extract_level2_headers_and_java_blocks(content)
        create_folder_and_java_files(path, file_path, headers, java_blocks)


# Example usage
path = '/home/vahid/Desktop/DependencyExtraction/jcg/jcg_testcases/src/main/resources'
process_md_files(path)
