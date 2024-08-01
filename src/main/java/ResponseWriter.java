import java.io.IOException;
import java.net.Socket;

public class ResponseWriter {

    public void write(Socket socket, ResponseOutput response) throws IOException {
        var headers = buildHeadersLine(response);
        writeStatusLine(socket, response);
        writeHeadersLine(socket, headers);
        writeBody(socket, response);
    }

    private void writeStatusLine(Socket socket, ResponseOutput response) throws IOException {
        socket.getOutputStream().write(response.statusLine().concat("\r\n").getBytes());
    }

    private void writeHeadersLine(Socket socket, String headers) throws IOException {
        socket.getOutputStream().write(headers.concat("\r\n").getBytes());
    }

    private void writeBody(Socket socket, ResponseOutput response) throws IOException {
        if (response.compressedBody() != null) {
            socket.getOutputStream().write(response.compressedBody());
        } else {
            socket.getOutputStream().write(response.body().getBytes());
        }
    }

    private String buildHeadersLine(ResponseOutput response) {
        var headers = "";
        if (response.headers() != null) {
            for (var e : response.headers().entrySet()) {
                headers = headers.concat(e.getKey()).concat(": ").concat(e.getValue()).concat("\r\n");
            }
        }
        return headers;
    }
}
