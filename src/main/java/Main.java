import java.io.IOException;
import java.net.ServerSocket;

public class Main {
  public static void main(String[] args) {

    System.out.println("Logs from your program will appear here!");

     try {
       ServerSocket serverSocket = new ServerSocket(4221);

       // Since the tester restarts your program quite often, setting SO_REUSEADDR
       // ensures that we don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);

       var clientSocket = serverSocket.accept(); // Wait for connection from client.
       System.out.println("accepted new connection");
       clientSocket.getOutputStream().write("HTTP/1.1 200 OK\\r\\n\\r\\n".getBytes());
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}