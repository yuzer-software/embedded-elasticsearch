package pl.allegro.tech.embeddedelasticsearch;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

import java.io.IOException;
import java.util.Base64;
import java.util.function.Consumer;

class HttpClient {

    private final CloseableHttpClient internalHttpClient = HttpClients.createDefault();

    String token = null;

    public HttpClient() {
    }

    public HttpClient(String username, String password) {
        String decodedToken = username + ":" + password;
        this.token = Base64.getEncoder().encodeToString(decodedToken.getBytes());
    }

    void execute(HttpUriRequestBase request) {
        execute(request, (HttpClientResponseHandler<Void>) response -> null);
    }

    void execute(HttpUriRequestBase request, Consumer<ClassicHttpResponse> block) {
        execute(request, (HttpClientResponseHandler<Void>) response -> {
            block.accept(response);
            return null;
        });
    }

    <T> T execute(HttpUriRequestBase request, HttpClientResponseHandler<T> responseHandler) {
        if (this.token != null) {
            request.addHeader("Authorization", "Basic " + token);
        }
        try {
            return internalHttpClient.execute(request, responseHandler);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        } finally {
            request.reset();
        }
    }

    static class HttpRequestException extends RuntimeException {
        HttpRequestException(IOException cause) {
            super(cause);
        }
    }
}
