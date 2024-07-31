import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

public class Main {

  public static void main(String[] args) {

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
              writeResponse(clientSocket, processRequest(request));
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

    var requestLine = input.readLine();
    if (StringUtils.isBlank(requestLine))
      return null;
    var requestLineArr = requestLine.split(" ");
    var method = requestLineArr[0];
    var url = requestLineArr[1];

    String headerLine = input.readLine();
    var headers = new HashMap<String, String>();
    while (StringUtils.isNotBlank(headerLine)) {
      var headerLineArr = headerLine.split(" ");
      headers.put(headerLineArr[0].substring(0, headerLineArr[0].length() - 1), headerLineArr[1]);
      headerLine = input.readLine();
    }

    return new RequestInput(method, url, headers);
  }

  private static String processRequest(RequestInput request) throws IOException {
    if (request.method().equals("GET") && (request.url().equals("/") || request.url().equals("/index.html"))) {
      return "HTTP/1.1 200 OK\r\n\r\n";
    } else if (request.method().equals("GET") && (request.url().startsWith("/echo/"))) {
      var str = request.url().split("/");
      if (str.length >= 3) {
        return "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + str[2].length() + "\r\n\r\n"
            + str[2];
      } else {
        return "HTTP/1.1 404 Not Found\r\n\r\n";
      }
    } else if (request.method().equals("GET") && request.url().equals("/user-agent")) {
      var header = request.headers().get("User-Agent");
      return "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + header.length() + "\r\n\r\n" + header;
    } else {
      return "HTTP/1.1 404 Not Found\r\n\r\n";
    }
  }

  private static void writeResponse(Socket socket, String response) throws IOException {
    socket.getOutputStream().write(response.getBytes());
  }
}
