package ru.ifmo.rain.moshnikov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;


public class HelloUDPClient implements HelloClient {

    private static final int TIMEOUT_SECONDS_PER_REQUEST = 10;
    private static final int SOCKET_SO_TIMEOUT = 200;

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.out.println("Usage: HelloUDPClient (name|ip) port prefix threads requests");
        } else {
            if (Arrays.stream(args).anyMatch(Objects::isNull)) {
                System.out.println("Non-null arguments expected");
                return;
            }
            try {
                int port = Integer.parseInt(args[1]);
                int threads = Integer.parseInt(args[3]);
                int requests = Integer.parseInt(args[4]);
                new HelloUDPClient().run(args[0], port, args[2], threads, requests);
            } catch (NumberFormatException e) {
                System.out.println("Arguments 'port', 'threads' and 'requests' are expected to be integers: " +
                        e.getMessage());
            }
        }
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try {
            final SocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);
            parallelProcessAll(address, prefix, threads, requests);
        } catch (UnknownHostException e) {
            System.err.println("Unable to reach specified host: " + e.getMessage());
        } catch (InterruptedException e) {
            System.err.println("Execution was interrupted: " + e.getMessage());
        }
    }

    private void parallelProcessAll(final SocketAddress address, String prefix, int threads, int requests)
            throws InterruptedException {
        ExecutorService workers = Executors.newFixedThreadPool(threads);
        IntStream.range(0, threads).forEach(threadId -> workers.submit(
                () -> processTask(address, prefix, threadId, requests)));
        workers.shutdown();
        workers.awaitTermination(TIMEOUT_SECONDS_PER_REQUEST * requests * threads, TimeUnit.SECONDS);
    }

    private void processTask(final SocketAddress address, String prefix, int threadId, int requests) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(SOCKET_SO_TIMEOUT);
            int receiveBufferSize = socket.getReceiveBufferSize();
            for (int requestId = 0; requestId < requests; requestId++) {
                String requestMessage = encodeMessage(prefix, threadId, requestId);

                boolean received = false;
                while (!received && !socket.isClosed() && !Thread.interrupted()) {
                    try {
                        final DatagramPacket request = new DatagramPacket(new byte[0], 0, address);
                        request.setData(requestMessage.getBytes(StandardCharsets.UTF_8));
                        socket.send(request);
                        request.setData(new byte[receiveBufferSize]);
                        socket.receive(request);
                        String responseMessage = decodeMessage(request);
                        if (received = responseMessage.contains(requestMessage)) {
                            System.out.println(String.format("Received '%s'", responseMessage));
                        }
                    } catch (IOException e) {
                        System.err.println("Error occured while trying to send a request or process a response: "
                                + e.getMessage());
                    }
                }
            }
        } catch (SocketException e) {
            System.err.println("Unable to establish connection via socket: " + e.getMessage());
        }
    }

    private static String encodeMessage(String prefix, int threadId, int requestId) {
        return prefix + threadId + "_" + requestId;
    }

    private static String decodeMessage(final DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

}