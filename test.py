import requests
import re
from bs4 import BeautifulSoup

web_url = 'http://localhost:8083/login.html'
domain = "http://localhost:8083/"
output_file = 'third_party_libs.txt'


def extract_urls(long_string):
    # Define the regular expression pattern
    pattern = r'\b(?:https?://)\S+\b'

    # Find all matches in the long string
    matches = re.findall(pattern, long_string)

    return matches


def fetch_js_urls(url):
    # send http
    response = requests.get(url)
    soup = BeautifulSoup(response.content)
    l = [i.get('src') for i in soup.find_all('script') if i.get('src')]
    return l


def combine_domain(endpoints):
    for i in range(len(endpoints)):
        if "http" not in endpoints[i] or "https" not in endpoints[i]:
            endpoints[i] = domain + endpoints[i]
            endpoints[i] = endpoints[i] .replace('//', '/')
            endpoints[i] = endpoints[i] .replace('/', '//', 1)
    print(endpoints)
    return endpoints


def fetch_api_urls(urls):
    result = []
    # send http
    for url in urls:
        if "http" not in url and "https" not in url:
            continue
        response = requests.get(url)
        soup = BeautifulSoup(response.content)
        soup_string = str(soup)
        result = result + extract_urls(soup_string)
    result = list(set(result))
    return result


def save_urls_to_file(urls, filename):
    # write to txt
    with open(filename, 'w') as file:
        for url in urls:
            file.write(url + '\n')


# print("Enter the url you want to test:")
# web_url = input()
# print("Enter the domain of the url:")
# domain = input()
# print(domain)
urls = (fetch_js_urls(web_url))
third_parties = (fetch_api_urls(combine_domain(urls)))
save_urls_to_file(third_parties, output_file)
