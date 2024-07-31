import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public class Main {

  public static void main(String[] args) {

    final var directory = args.length >= 2 && args[0].equals("--directory") ? args[1] : "";

    System.out.println("Logs from your program will appear here!");

    try (ServerSocket serverSocket = new ServerSocket(4221)) {
      // Since the tester restarts your program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);

      while (true) {
        var clientSocket = serverSocket.accept(); // Wait for connection from client.
        System.out.println("accepted new connection");

        Runnable process = () -> {
          try {
            var request = parseRequest(clientSocket);
            if (request != null) {
              writeResponse(clientSocket, processRequest(request, directory));
            } else {
              writeResponse(clientSocket, "HTTP/1.1 404 Not Found\r\n\r\n");
            }
          } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
          }
        };

        var thread = new Thread(process);
        thread.start();
      }

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  private static RequestInput parseRequest(Socket socket) throws IOException {
    var input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    // Read request line
    var requestLine = input.readLine();
    if (StringUtils.isBlank(requestLine))
      return null;
    var requestLineArr = requestLine.split(" ");
    var method = requestLineArr[0];
    var url = requestLineArr[1];

    // Read headers
    String headerLine = input.readLine();
    var headers = new HashMap<String, String>();
    while (StringUtils.isNotBlank(headerLine)) {
      var headerLineArr = headerLine.split(" ");
      headers.put(headerLineArr[0].substring(0, headerLineArr[0].length() - 1), headerLineArr[1]);
      headerLine = input.readLine();
    }

    // Read request body
    var body = new StringBuilder();
    while (input.ready()) {
      body = body.append((char) input.read());
    }

    return new RequestInput(method, url, headers, body.toString());
  }

  private static String processRequest(RequestInput request, String directory) throws IOException {
    var response = "";

    // Process simple request
    if (request.method().equals("GET") && (request.url().equals("/") || request.url().equals("/index.html")))
      response = "HTTP/1.1 200 OK\r\n\r\n";

    // Read request paramater and write it back to the body
    if (request.method().equals("GET") && (request.url().startsWith("/echo/"))) {
      var str = request.url().split("/");
      if (str.length < 3) {
        response = "HTTP/1.1 400 Bad Request\r\n\r\n";
      } else {
        response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + str[2].length() + "\r\n\r\n"
            + str[2];
      }
    }

    // Read header and write it back to the body
    if (request.method().equals("GET") && request.url().equals("/user-agent")) {
      var header = request.headers().get("User-Agent");
      response = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + header.length() + "\r\n\r\n"
          + header;
    }

    // Return a file
    if (request.method().equals("GET") && request.url().startsWith("/files")) {
      var url = request.url().split("/");
      if (url.length < 3) {
        response = "HTTP/1.1 400 Bad Request\r\n\r\n";
      } else {
        var fileName = url[2];
        var file = new File(directory + fileName);
        if (!file.exists()) {
          response = "HTTP/1.1 404 Not Found\r\n\r\n";
        } else {
          var data = Files.readAllLines(file.toPath()).stream().collect(Collectors.joining("\n"));
          response = "HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: " + data.length()
              + "\r\n\r\n" + data;
        }
      }
    }

    // Create file
    if (request.method().equals("POST") && request.url().startsWith("/files")) {
      var url = request.url().split("/");
      if (url.length < 3) {
        response = "HTTP/1.1 400 Bad Request\r\n\r\n";
      } else {
        var fileName = url[2];
        var file = new File(directory + fileName);
        if (!file.createNewFile()) {
          response = "HTTP/1.1 409 Conflict\r\n\r\n";
        } else {
          var fileWriter = new FileWriter(file);
          fileWriter.write(request.body());
          fileWriter.close();
          response = "HTTP/1.1 201 Created\r\n\r\n";
        }
      }
    }

    // Compression
    if (request.headers().containsKey("Accept-Encoding") && request.headers().get("Accept-Encoding").equals("gzip")) {
      var idx = response.indexOf("\r\n") + 2;
      response = response.substring(0, idx).concat("Content-Encoding: gzip\r\n").concat(response.substring(idx));
    }

    if (StringUtils.isEmpty(response))
      response = "HTTP/1.1 404 Not Found\r\n\r\n";
    return response;
  }

  private static void writeResponse(Socket socket, String response) throws IOException {
    socket.getOutputStream().write(response.getBytes());
  }
}
