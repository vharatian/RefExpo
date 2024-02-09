#!/bin/bash

# Start in the current directory or specify a path
start_path="."

# Find all 'callgraph.json' files and copy each to 'cleaned_callgraph.json'
find "$start_path" -type f -name 'callgraph.json' -exec sh -c 'cp "$0" "${0%/*}/cleaned_callgraph.json"' {} \;

