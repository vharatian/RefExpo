# Performance Analysis
This is the helper project we have used to evaluate performance of RefExpo.

## Repository Finder
This project contains a functionality to query and filter repositories from GitHub. 
It is used to find repositories for RefExpo.
To find out about parameters and usage, run the following command:
```bash
python repository_finder.py
```

## Performance Analyzer
This project contains a functionality to analyze performance of RefExpo.
This evaluation is capable to evaluate RefExpo outputs against [Jarviz](https://github.com/ExpediaGroup/jarviz), [DependencyFinder](https://depfind.sourceforge.io/), [SonarGraph](https://www.hello2morrow.com/products/sonargraph).
Data related to the evaluations should be placed in the `data` folder.
To run the evaluation, run the following command:
```bash
python performance_analyzer.py <project_name>
```