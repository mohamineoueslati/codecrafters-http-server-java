import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

public class RequestProcessor {

    public ResponseOutput process(RequestInput request) throws IOException {
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
                var file = new File(Properties.DIR_PATH + fileName);
                if (!file.exists()) {
                    response = new ResponseOutput("HTTP/1.1 404 Not Found", null, null, null);
                } else {
                    var data = Files.readAllLines(file.toPath()).stream().collect(Collectors.joining("\n"));
                    response = new ResponseOutput(
                            "HTTP/1.1 200 OK",
                            Map.of("Content-Type", "application/octet-stream", "Content-Length",
                                    String.valueOf(data.length())),
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
                var file = new File(Properties.DIR_PATH + fileName);
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

}
