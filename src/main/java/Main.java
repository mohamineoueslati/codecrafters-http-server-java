import java.io.*;
import java.net.ServerSocket;

public class Main {
  public static void main(String[] args) {

    System.out.println("Logs from your program will appear here!");

     try(ServerSocket serverSocket = new ServerSocket(4221)) {
       // Since the tester restarts your program quite often, setting SO_REUSEADDR
       // ensures that we don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);

       var clientSocket = serverSocket.accept(); // Wait for connection from client.
       System.out.println("accepted new connection");
       var input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
       var request = input.readLine().split(" ");
       var method = request[0];
       var url = request[1];
       if (method.equals("GET") && (url.equals("/") || url.equals("/index.html"))) {
         clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
       } else {
         clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
       }
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
