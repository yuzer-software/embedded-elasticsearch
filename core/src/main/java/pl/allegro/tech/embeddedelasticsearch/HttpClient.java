package pl.allegro.tech.embeddedelasticsearch;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.function.Function;

class HttpClient {

    private static final Consumer<CloseableHttpResponse> noop = x -> {
    };

    private final CloseableHttpClient internalHttpClient = HttpClients.createDefault();

    String token = null;

    public HttpClient() {
    }

    public HttpClient(String username, String password) {
        String decodedToken = username + ":" + password;
        this.token = Base64.getEncoder().encodeToString(decodedToken.getBytes());
    }

    void execute(HttpRequestBase request) {
        execute(request, noop);
    }

    void execute(HttpRequestBase request, Consumer<CloseableHttpResponse> block) {
        execute(request, (Function<CloseableHttpResponse, Void>) response -> {
            block.accept(response);
            return null;
        });
    }

    <T> T execute(HttpRequestBase request, Function<CloseableHttpResponse, T> block) {
        if (this.token != null) {
            request.addHeader("Authorization", "Basic " + token);
        }
        try (CloseableHttpResponse response = internalHttpClient.execute(request)) {
            return block.apply(response);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        } finally {
            request.releaseConnection();
        }
    }

    static class HttpRequestException extends RuntimeException {
        HttpRequestException(IOException cause) {
            super(cause);
        }
    }
}
