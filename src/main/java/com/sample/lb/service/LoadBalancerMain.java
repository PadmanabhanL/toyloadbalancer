package com.sample.lb.service;

import com.sample.lb.bo.BalancingStrategy;
import com.sample.lb.bo.LoadBalancer;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoadBalancerMain {


    public static final int SERVER_PORT = 8000;

    private static final ExecutorService executor = Executors.newFixedThreadPool(100);

    public static void main(String[] args) throws IOException {

        LoadBalancer loadBalancer = new LoadBalancer(List.of("http://localhost:9000"), BalancingStrategy.RANDOM);

        try {
            HttpServer lbServer = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
            lbServer.createContext("/", exchange -> {
                executor.submit(() -> {
                    String respText = "Hello!";
                    try {
                        exchange.sendResponseHeaders(200, respText.getBytes().length);
                        OutputStream output = exchange.getResponseBody();
                        output.write(respText.getBytes());
                        output.flush();
                        exchange.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            });
            lbServer.setExecutor(executor);
            lbServer.start();
            System.out.println("Server started in " +SERVER_PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
