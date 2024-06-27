package com.sample.lb.service;

import com.sample.lb.bo.BalancingStrategy;
import com.sample.lb.bo.LoadBalancer;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class LoadBalancerMain {


    public static final int SERVER_PORT = 8000;

    private static final ExecutorService executor = Executors.newFixedThreadPool(100);

    private static final Map<String, String> userStickyTarget = new ConcurrentHashMap<>();

    private static final LoadBalancer loadBalancer = new LoadBalancer(List.of("http://localhost:9000", "http://localhost:9001"), BalancingStrategy.STICKY_ROUND_ROBIN);

    private static int index = 0;

    public static void main(String[] args) {



        try {
            HttpServer lbServer = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
            lbServer.createContext("/", exchange -> {
                executor.submit(() -> {
                    HttpResponse<String> stringHttpResponse = null;
                    try {
                        stringHttpResponse = makeExternalCall(exchange);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    String responseBody = stringHttpResponse.body();
                    OutputStream outputStream = exchange.getResponseBody();
                    try {
                        exchange.sendResponseHeaders(stringHttpResponse.statusCode(), stringHttpResponse.body().length());
                        outputStream.write(responseBody.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    exchange.close();
                });
            });
            lbServer.setExecutor(executor);
            lbServer.start();
            System.out.println("Server started in " +SERVER_PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private static HttpResponse<String> makeExternalCall(HttpExchange exchange) throws IOException, InterruptedException {

        String targetInstance = "";

        if (loadBalancer.getBalancingStrategy() == BalancingStrategy.ROUND_ROBIN) {

            int numberOfInstances = loadBalancer.getTargetHosts().size();
            if (++index >= numberOfInstances) {
                index = 0;
            }

            targetInstance = loadBalancer.getTargetHosts().get(index);

        } else if (loadBalancer.getBalancingStrategy() == BalancingStrategy.STICKY_ROUND_ROBIN) {


            String username = exchange.getRequestHeaders().get("username") != null ? exchange.getRequestHeaders().get("username").get(0) : "";

            if (userStickyTarget.containsKey(username)) {
                targetInstance = userStickyTarget.get(username);
            } else {
                int random = ThreadLocalRandom.current().nextInt(0, loadBalancer.getTargetHosts().size());
                targetInstance = loadBalancer.getTargetHosts().get(random);
                userStickyTarget.put(username, targetInstance);
            }

        }
        Headers requestHeaders = exchange.getRequestHeaders();
        Map<String, String> newHeaders = new HashMap<>();
        requestHeaders.forEach((k, v) -> {
            newHeaders.put(k, v.get(0));
        });
        String requestMethod = exchange.getRequestMethod();
        URI requestURI = null;
        try {
            requestURI = new URI(targetInstance + exchange.getRequestURI().getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofMinutes(1)).build();
        HttpRequest.Builder initial = HttpRequest.newBuilder()
                                                .method(requestMethod, HttpRequest.BodyPublishers.noBody());

        //FIXME Handle Propagation of Headers
       /* for (Map.Entry<String, String> entry: newHeaders.entrySet()) {
            initial.headers(entry.getKey(), entry.getValue());
        }*/

        initial.headers("username", "Paddy");

        HttpRequest request = initial.uri(requestURI).build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());


    }
}
