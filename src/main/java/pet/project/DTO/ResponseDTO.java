package pet.project.DTO;

import java.util.HashMap;

public class ResponseDTO {
    private String request;
    private HashMap<String, String> headers = new HashMap<>();
    private String data;

    {
        headers.put("Content-Type", "text/plain; charset=utf-8");
    }

    public ResponseDTO(String request, HashMap<String, String> headers, String data) {
        this.request = request;
        this.headers.putAll(headers);
        this.data = data;
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder();
        if (request != null) response.append(request).append("\r\n");
        if (headers != null) {
            headers.forEach((key, value) -> response.append(key).append(": ").append(value).append("\r\n"));
            response.append("\r\n");
        }
        if (data != null) response.append(data);
        return response.toString();
    }
}
