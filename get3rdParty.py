import requests
from bs4 import BeautifulSoup

# input and output
web_url = 'https://www.bd.com/en-us/about-bd/cybersecurity/bulletin/apache-log4j-vulnerability-bd-third-party-components-impacted'
output_file = 'third_party_libs.txt'

def fetch_js_urls(url):
    # send http
    response = requests.get(url)
    soup = BeautifulSoup(response.text, 'html.parser')
    # find all <script> tags
    script_tags = soup.find_all('script')

    urls = []
    # go through script tags
    for script in script_tags:
        src = script.get('src')
        if src:
            # get all urls
            if "http://" in src or "https://" in src:
                urls.append(src)

    return urls


def save_urls_to_file(urls, filename):
    # write to txt
    with open(filename, 'w') as file:
        for url in urls:
            file.write(url + '\n')




# get urls
urls = fetch_js_urls(web_url)
# save to file
save_urls_to_file(urls, output_file)

print("URLs have been written to", output_file)
