#!/bin/bash

# Check if an argument is provided
if [ $# -eq 0 ]; then
  echo "Usage: $0 /path/to/your/repository"
  exit 1
fi

# Use the first argument as the starting directory
START_DIR=$1

# Find all subdirectories within the specified path, excluding the root and directories starting with a dot
find "$START_DIR" -mindepth 1 -type d ! -path '*/.*' | while read -r dir; do
  # Check if "__init__.py" file does not exist in the directory
  if [ ! -f "$dir/__init__.py" ]; then
    # Create an empty "__init__.py" file
    echo "Creating __init__.py in $dir"
    touch "$dir/__init__.py"
  fi
done

echo "All subdirectories checked and necessary __init__.py files created, excluding directories starting with ."

