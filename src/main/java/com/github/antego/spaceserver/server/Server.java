package com.github.antego.spaceserver.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Server {
    public static final Logger logger = Logger.getLogger(Server.class.getName());

    public static final String SEP=System.lineSeparator();

    ServerSocket serverSocket;
    AcceptThread acceptThread;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            logger.log(Level.SEVERE, "invalid arguments, please specify a port");
            throw new IllegalArgumentException();
        }
        Server server = new Server();
        server.inputLoop();
    }

    private void inputLoop() throws IOException {
        boolean exit = false;
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        while (!exit) {
            showHint();
            String command;
            while ((command = consoleReader.readLine()) != null) {
                switch (command) {
                    case "start":
                        if(acceptThread == null) {
                            start();
                            System.out.println("server started");
                        } else {
                            System.out.println("already started");
                        }
                        break;
                    case "stop":
                        if (acceptThread != null) {
                            stop();
                            serverSocket.close();
                            System.out.println("server stopped");
                            return;
                        } else {
                            System.out.println("nothing to stop");
                        }
                        break;
                    default:
                        showHint();
                }
            }
        }
    }

    private void stop() {
        acceptThread.interrupt();
        acceptThread.closeAllSockets();
        acceptThread = null;
    }

    private void start() throws IOException {
        serverSocket = new ServerSocket(9998);
        acceptThread = new AcceptThread(serverSocket);
        acceptThread.start();
    }

    private void showHint() {
        System.out.println("Print <start> to start server, or <stop> to stop:");
    }
}
