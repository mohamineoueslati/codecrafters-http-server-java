import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

public class RequestParser {

    public RequestInput parse(Socket socket) throws IOException {
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

}
