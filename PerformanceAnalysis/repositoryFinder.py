import requests
import json
import argparse


def load_query(filename):
    with open(filename, 'r') as file:
        return file.read()


def get_repositories(token, languages, threshold, max_repos, query_file="query.graphql"):
    repositories = []
    end_cursor = None  # Start with no cursor
    headers = {"Authorization": f"Bearer {token}"}
    query_template = load_query(query_file)

    while len(repositories) < max_repos:
        query = query_template.replace("AFTER_CURSOR", f'"{end_cursor}"' if end_cursor else "null")
        request = requests.post('https://api.github.com/graphql', json={'query': query}, headers=headers)

        if request.status_code != 200:
            raise Exception(f"Query failed with code {request.status_code}. {query}")

        result = request.json()
        edges = result['data']['search']['edges']
        pageInfo = result['data']['search']['pageInfo']

        for edge in edges:
            repo = edge['node']
            if meets_language_criteria(repo['languages'], languages, threshold):
                repositories.append(repo)
                if len(repositories) >= max_repos:
                    break

        if not pageInfo['hasNextPage']:
            break

        end_cursor = pageInfo['endCursor']

    return repositories[:max_repos]


def meets_language_criteria(languages_data, specified_languages, threshold):
    total_size = languages_data['totalSize']
    specified_languages_size = sum(
        edge['size'] for edge in languages_data['edges'] if edge['node']['name'] in specified_languages)
    percentage = (specified_languages_size / total_size) * 100 if total_size > 0 else 0
    return percentage >= threshold


def calculate_language_percentages(languages_data):
    total_size = languages_data['totalSize']
    language_percentages = {}
    for edge in languages_data['edges']:
        language = edge['node']['name']
        size = edge['size']
        percentage = (size / total_size) * 100 if total_size > 0 else 0
        language_percentages[language] = percentage
    return language_percentages


def main():
    parser = argparse.ArgumentParser(description="Fetch GitHub repositories based on language usage.")
    parser.add_argument("token", type=str, help="GitHub Personal Access Token")
    parser.add_argument("--languages", nargs='+', default=['Python'], help="List of languages to filter")
    parser.add_argument("--threshold", type=float, default=90.0, help="Minimum percentage threshold for specified languages")
    parser.add_argument("--max_repos", type=int, default=10, help="Maximum number of repositories to fetch")
    args = parser.parse_args()

    try:
        repositories = get_repositories(args.token, args.languages, args.threshold, args.max_repos)
        for repo in repositories:
            languages_data = repo['languages']
            languages_percentages = calculate_language_percentages(languages_data)
            languages_str = ', '.join([f"{lang}: {percent:.2f}%" for lang, percent in languages_percentages.items()])
            print(
                f"Name: {repo['name']}, Stars: {repo['stargazers']['totalCount']}, URL: {repo['url']}, Languages: {languages_str if languages_percentages else 'None'}")
    except Exception as e:
        print(str(e))


if __name__ == "__main__":
    main()
