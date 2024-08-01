import java.io.IOException;
import java.net.ServerSocket;

public class Server implements AutoCloseable {

    private final ServerSocket serverSocket;

    public Server(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.serverSocket.setReuseAddress(true);
    }

    public void start() throws IOException {
        while (true) {
            var clientSocket = serverSocket.accept(); // Wait for connection from client.
            System.out.println("accepted new connection");
            var handler = new RequestHandler(clientSocket);
            var thread = new Thread(handler);
            thread.start();
        }
    }

    @Override
    public void close() throws IOException {
        this.serverSocket.close();
    }

}
