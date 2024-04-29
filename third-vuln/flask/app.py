# import the Flask class from the flask module
from flask import Flask, render_template, redirect, url_for, request
from request import *

# create the application object
app = Flask(__name__)

# use decorators to link the function to a url


@app.route('/')
def home():
    return "Hello, World!"  # return a string


@app.route('/welcome')
def welcome():
    return render_template('welcome.html')  # render a template

# Route for handling the login page logic


@app.route('/login', methods=['GET', 'POST'])
def login():
    error = None
    if request.method == 'POST':
        var1 = request.form['username']
        var2 = request.form['password']
        var1 = var1.replace("$", "")
        result = loginrequest(var1, var2)
        print(result)
        filter = result.replace("\n", "")
        print(filter)
        if filter == "<html><body>Welcome Back Admin":
            error = "hi"
        else:
            error = "bye"
    return render_template('login.html', error=error)


# start the server with the 'run()' method
if __name__ == '__main__':
    app.run(debug=True)
