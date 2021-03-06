package no.difi.pipeline.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static org.springframework.http.HttpMethod.POST;

public class CallbackClient {

    private RestTemplate httpClient;

    public CallbackClient(RestTemplate httpClient) {
        this.httpClient = httpClient;
    }

    public CallbackClient.Response callback(URL address) {
        final long t0 = currentTimeMillis();
        try {
            ResponseEntity<String> httpResponse = httpClient.exchange(
                    requestUri(address),
                    POST,
                    new HttpEntity<>(""),
                    String.class
            );
            return new Response(httpResponse, t0);
        } catch (Exception e) {
            return new Response(e, t0);
        }
    }

    private URI requestUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI: " + url, e);
        }
    }

    public static class Response {

        private ResponseEntity<String> httpResponse;
        private Exception exception;
        private long requestDuration;

        Response(ResponseEntity<String> httpResponse, long requestTime) {
            this.httpResponse = httpResponse;
            this.requestDuration = System.currentTimeMillis() - requestTime;
        }

        Response(Exception exception, long requestTime) {
            this.exception = exception;
            this.requestDuration = System.currentTimeMillis() - requestTime;
        }

        public boolean ok() {
            return httpResponse != null && httpResponse.getStatusCode().is2xxSuccessful();
        }

        public long requestDuration() {
            return requestDuration;
        }

        public String errorDetails() {
            if (exception != null) {
                return format("Exception: %s [%d]", exception.toString(), requestDuration);
            } else if (httpResponse != null) {
                return format("HTTP status %s [%d]", httpResponse.getStatusCodeValue(), requestDuration);
            } else {
                return null;
            }
        }

        public boolean notFound() {
            return exception != null &&
                    exception instanceof HttpClientErrorException &&
                    ((HttpClientErrorException)exception).getRawStatusCode() == 404;
        }

    }
}
