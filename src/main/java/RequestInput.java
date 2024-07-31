import java.util.Map;

public record RequestInput(
        String method,
        String url,
        Map<String, String> headers,
        String body
) {
}
