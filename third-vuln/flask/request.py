import requests


def loginrequest(username, password):
    url = 'http://localhost:8080/login'
    values = {'uname': username, 'password': password}
    response = requests.post(url, data=values)
    return response.text


if __name__ == '__main__':
    loginrequest("admin", "password")
