import java.util.Map;

public record ResponseOutput(
    String statusLine,
    Map<String, String> headers,
    String body,
    byte[] compressedBody
) {
}
