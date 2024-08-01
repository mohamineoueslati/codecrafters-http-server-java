import java.io.IOException;
import java.net.Socket;

public class RequestHandler implements Runnable {

    private final Socket socket;
    private final RequestParser requestParser;
    private final RequestProcessor requestProcessor;
    private final ResponseWriter responseWriter;

    public RequestHandler(Socket socket) {
        this.socket = socket;
        this.requestParser = new RequestParser();
        this.requestProcessor = new RequestProcessor();
        this.responseWriter = new ResponseWriter();
    }

    @Override
    public void run() {
        try {
            var request = this.requestParser.parse(this.socket);
            var response = this.requestProcessor.process(request);
            this.responseWriter.write(this.socket, response);
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("IOException: " + e.getMessage());
                }
            }
        }
    }

}
