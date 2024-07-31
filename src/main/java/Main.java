import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.ArrayUtils;
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
              writeResponse(clientSocket, new ResponseOutput("HTTP/1.1 404 Not Found", null, null, null));
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
      var key = headerLine.substring(0, headerLine.indexOf(":"));
      var value = headerLine.substring(headerLine.indexOf(":") + 1);
      headers.put(key.trim(), value.trim());
      headerLine = input.readLine();
    }

    // Read request body
    var body = new StringBuilder();
    while (input.ready()) {
      body = body.append((char) input.read());
    }

    return new RequestInput(method, url, headers, body.toString());
  }

  private static ResponseOutput processRequest(RequestInput request, String directory) throws IOException {
    ResponseOutput response = null;

    // Process simple request
    if (request.method().equals("GET") && (request.url().equals("/") || request.url().equals("/index.html")))
      response = new ResponseOutput("HTTP/1.1 200 OK", null, null, null);

    // Read request paramater and write it back to the body
    if (request.method().equals("GET") && (request.url().startsWith("/echo/"))) {
      var str = request.url().split("/");
      if (str.length < 3) {
        response = new ResponseOutput("HTTP/1.1 400 Bad Request", null, null, null);
      } else {
        response = new ResponseOutput(
            "HTTP/1.1 200 OK",
            Map.of("Content-Type", "text/plain", "Content-Length", String.valueOf(str[2].length())),
            str[2],
            null);
      }
    }

    // Read header and write it back to the body
    if (request.method().equals("GET") && request.url().equals("/user-agent")) {
      var header = request.headers().get("User-Agent");
      response = new ResponseOutput(
          "HTTP/1.1 200 OK",
          Map.of("Content-Type", "text/plain", "Content-Length", String.valueOf(header.length())),
          header,
          null);
    }

    // Return a file
    if (request.method().equals("GET") && request.url().startsWith("/files")) {
      var url = request.url().split("/");
      if (url.length < 3) {
        response = new ResponseOutput("HTTP/1.1 400 Bad Request", null, null, null);
      } else {
        var fileName = url[2];
        var file = new File(directory + fileName);
        if (!file.exists()) {
          response = new ResponseOutput("HTTP/1.1 404 Not Found", null, null, null);
        } else {
          var data = Files.readAllLines(file.toPath()).stream().collect(Collectors.joining("\n"));
          response = new ResponseOutput(
              "HTTP/1.1 200 OK",
              Map.of("Content-Type", "application/octet-stream", "Content-Length", String.valueOf(data.length())),
              data,
              null);
        }
      }
    }

    // Create file
    if (request.method().equals("POST") && request.url().startsWith("/files")) {
      var url = request.url().split("/");
      if (url.length < 3) {
        response = new ResponseOutput("HTTP/1.1 400 Bad Request", null, null, null);
      } else {
        var fileName = url[2];
        var file = new File(directory + fileName);
        if (!file.createNewFile()) {
          response = new ResponseOutput("HTTP/1.1 409 Conflict", null, null, null);
        } else {
          var fileWriter = new FileWriter(file);
          fileWriter.write(request.body());
          fileWriter.close();
          response = new ResponseOutput("HTTP/1.1 201 Created", null, null, null);
        }
      }
    }

    // Compression
    if (response != null &&
        request.headers().containsKey("Accept-Encoding") &&
        request.headers().get("Accept-Encoding").contains("gzip")) {
      var body = response.body();
      var bos = new ByteArrayOutputStream(body.length());
      var gzip = new GZIPOutputStream(bos);
      gzip.write(body.getBytes());
      gzip.close();
      var compressedBody = bos.toByteArray();
      bos.close();
      var headers = new HashMap<>(response.headers());
      headers.remove("Content-Length");
      headers.put("Content-Length", String.valueOf(compressedBody.length));
      headers.put("Content-Encoding", "gzip");
      response = new ResponseOutput(
          response.statusLine(),
          headers,
          body,
          compressedBody);
    }

    if (response == null)
      response = new ResponseOutput("HTTP/1.1 404 Not Found", null, null, null);
    return response;
  }

  private static void writeResponse(Socket socket, ResponseOutput response) throws IOException {
    byte[] byteReponse = null;

    // Build headers
    var headers = "";
    if (response.headers() != null) {
      for (var e : response.headers().entrySet()) {
        headers = headers.concat(e.getKey()).concat(": ").concat(e.getValue()).concat("\r\n");
      }
    }

    // Build response string without body
    var res = response.statusLine().concat("\r\n").concat(headers).concat("\r\n");

    // Set body & build byte response
    if (response.compressedBody() != null) {
      byteReponse = ArrayUtils.addAll(res.getBytes(), response.compressedBody());
    } else {
      byteReponse = res.concat(StringUtils.stripToEmpty(response.body())).getBytes();
    }

    socket.getOutputStream().write(byteReponse);
  }
}
