package com.solv.websockets;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


public class ApplicationServer {

    private static DeploymentManager deploymentManager;

    private static Undertow server;

    private static final Logger LOGGER = Logger.getLogger(ApplicationServer.class.getName());

    public static void main(String[] args) {
        startServer(8025);
    }

    public static void startServer(int port) {
        PathHandler path = Handlers.path();

        // Add WebSocket endpoint
        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(ApplicationServer.class.getClassLoader())
                .setContextPath("/")
                .setResourceManager(new ClassPathResourceManager(ApplicationServer.class.getClassLoader()))
                .addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                        new WebSocketDeploymentInfo()
                                .setBuffers(new DefaultByteBufferPool(true, 100))
                                .addEndpoint(PushEndpoint.class))
                .setDeploymentName("application.war");

        LOGGER.info("Starting application deployment");

        deploymentManager = Servlets.defaultContainer().addDeployment(servletBuilder);
        deploymentManager.deploy();

        try {
            path.addPrefixPath("/", deploymentManager.start());
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }

        server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(path)
                .build();

        server.start();

        System.out.println("WebSocket Server started on port " + port);
        startCommandLineReader();
    }


    private static void startCommandLineReader() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                while (true) {
                    System.out.print("Enter message to send to clients or type 'stop' to stop the server: ");
                    String message = reader.readLine();
                    if (message != null && !message.isEmpty()) {
                        if (message.equalsIgnoreCase("stop")) {
                            stopServer();
                            break;
                        } else {
                            PushEndpoint.broadcast(message);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
    }

    public static void stopServer() {
        try {
            server.stop();
            System.out.println("WebSocket Server stopped.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
