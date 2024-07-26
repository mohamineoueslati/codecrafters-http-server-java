import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

  private static Socket clientSocket;

  public static void main(String[] args) {

    System.out.println("Logs from your program will appear here!");

     try(ServerSocket serverSocket = new ServerSocket(4221)) {
       // Since the tester restarts your program quite often, setting SO_REUSEADDR
       // ensures that we don't run into 'Address already in use' errors

       serverSocket.setReuseAddress(true);
       clientSocket = serverSocket.accept(); // Wait for connection from client.
       System.out.println("accepted new connection");

       var request = parseRequest();
       if (request.length >= 2) {
         var method = request[0];
         var url = request[1];
         writeResponse(processRequest(method, url));
       } else {
         writeResponse("HTTP/1.1 404 Not Found\r\n\r\n");
       }

     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }

  private static String[] parseRequest() throws IOException {
    var input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    return input.readLine().split(" ");
  }

  private static String processRequest(String method, String url) throws IOException {
    if (method.equals("GET") && (url.equals("/") || url.equals("/index.html"))) {
      return "HTTP/1.1 200 OK\r\n\r\n";
    } else if (method.equals("GET") && (url.startsWith("/echo/"))) {
      var str = url.split("/");
      if (str.length >= 3) {
        return "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: " + str[2].length() + "\r\n\r\n" + str[2];
      } else {
        return "HTTP/1.1 404 Not Found\r\n\r\n";
      }
    } else {
      return "HTTP/1.1 404 Not Found\r\n\r\n";
    }
  }

  private static void writeResponse(String response) throws IOException {
    clientSocket.getOutputStream().write(response.getBytes());
  }
}
