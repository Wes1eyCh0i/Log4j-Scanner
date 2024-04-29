import http.server
import socketserver

# Set the port number
PORT = 8082

# Set up the request handler
Handler = http.server.SimpleHTTPRequestHandler

# Create the server object
with socketserver.TCPServer(("", PORT), Handler) as httpd:
    print("Server started at localhost:" + str(PORT))
    # Start the server
    httpd.serve_forever()
