package com.github.antego.spaceserver.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Server {
    public static final Logger logger = Logger.getLogger(Server.class.getName());

    public static final String SEP = System.lineSeparator();

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    private void start() {
        CountDownLatch latch = new CountDownLatch(1);
        try (AcceptThread acceptThread = new AcceptThread(new ServerSocket(9998))) {
            acceptThread.start();
            try {
                latch.await();
            } catch (InterruptedException e) {
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error", e);
        }
    }
}
