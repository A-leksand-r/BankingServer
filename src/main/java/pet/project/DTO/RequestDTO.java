package pet.project.DTO;

import java.util.HashMap;

public class RequestDTO {
    private String method;
    private String url;
    private HashMap<String, String> headers = new HashMap<>();
    private ClientDTO body;

    public void setMethod(String method) {
        this.method = method;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
    }

    public void setBody(ClientDTO body) {
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public void addHeader(String key, String value) {
        if (key != null && value != null) {
            headers.put(key, value);
        }
    }

    public ClientDTO getBody() {
        return body;
    }
}
