package util;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;

import java.io.IOException;

/**
 * The type Network.
 */
public class network {
    /**
     * Gets response.
     *
     * @param url the url
     * @return the response
     * @throws IOException the io exception
     */
    public static HttpResponse getResponse(String url) throws IOException {
        HttpClient client = HttpClientBuilder.create()
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setUserAgent("Feedfetcher-Google; (+http://www.google.com/feedfetcher.html; feed-id=0)")
                .build();
        HttpUriRequest request = new HttpGet(url);
        return client.execute(request);
    }
}
