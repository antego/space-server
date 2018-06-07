package com.github.antego.spaceserver.client;

import com.github.antego.spaceserver.server.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class Client {
    public static final String HOST = "localhost";
    public static final Integer PORT = 9998;
    private static final Integer MESSAGE_COUNT = 100;
    private static final Integer MESSAGE_LENGTH = 4;
    private static final Integer WAVE_COUNT = 3000;
    private static final Integer CLIENTS_COUNT = 1000;
    ExecutorService receiverExecutor = Executors.newSingleThreadExecutor();
    Socket senderSocket;
    Socket receiverSocket;
    ArrayList<Socket> firstPart;
    ArrayList<Socket> secondPart;
    ArrayList<long[]> wavePeriods = new ArrayList<>();
    long sentMessagesCount;
    private long startTime;
    private long endTime;


    public Client() throws IOException {
        senderSocket = new Socket(HOST, PORT);
        receiverSocket = new Socket(HOST, PORT);
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.doPingPong();
    }

    private void startTest() throws InterruptedException, IOException, ExecutionException {
        new Thread(() -> {
            for(int i = 0; i < MESSAGE_COUNT; i++) {
                try {
                    new DataOutputStream(senderSocket.getOutputStream()).writeDouble(12321.123);
                    Thread.sleep(1000/30);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Future<Integer> times = receiverExecutor.submit(() -> {
            int i = 0;
            DataInputStream dataInputStream = new DataInputStream(receiverSocket.getInputStream());
            while (i < MESSAGE_COUNT) {
                if(i != 0 && i % 100 == 0) {
                    System.out.println(Server.SEP + i + "" + Server.SEP);
                }
                try {
                    System.out.println(dataInputStream.readDouble());
                } catch (IOException e) {
                    System.out.println("connection halted, received " + i + " packets");
                    break;
                }
                i++;
            }
            return i;
        });

        try {
            System.out.println("received " + times.get(65, TimeUnit.SECONDS));
        } catch (TimeoutException e) {
        }
        receiverExecutor.shutdown();
        receiverSocket.close();
    }

    private void connectClients(int count) throws IOException {
        firstPart = new ArrayList<>(count/2);
        secondPart = new ArrayList<>(count/2);
        for (int i = 0; i < count; i++) {
            Socket socket = new Socket(HOST, PORT);

            if(i % 2 == 1) {
                firstPart.add(socket);
            } else {
                secondPart.add(socket);
            }
        }
    }

    private void disconnectClients() {
        closeAllSockets(firstPart);
        closeAllSockets(secondPart);
    }

    private void doPingPong() throws IOException {
        connectClients(CLIENTS_COUNT);
        Echoer firstEchoer = new Echoer(firstPart);
        firstEchoer.setName("firstEchoer");
        Echoer secondEchoer = new Echoer(secondPart);
        secondEchoer.setName("seondEchoer");
        startTime = System.currentTimeMillis();
        firstEchoer.start();
        secondEchoer.start();
        doInitialSend();
        try {
            firstEchoer.join();
            secondEchoer.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        endTime = System.currentTimeMillis();
        disconnectClients();
        printResults();
    }

    private void printResults() {
        System.out.println((double)(1000 * sentMessagesCount) / (endTime - startTime) / CLIENTS_COUNT + " msg/sec");
    }

    private void doInitialSend() {
        long[] initialTimes = new long[firstPart.size()];
        try {
            for (int i = 0; i < firstPart.size(); i++) {
                Socket socket = firstPart.get(i);
                byte[] message = {1,1,0,1};
                socket.getOutputStream().write(message);
                sentMessagesCount++;
                initialTimes[i] = System.currentTimeMillis();
            }
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
        wavePeriods.add(initialTimes);
    }

    private void closeAllSockets(Collection<Socket> sockets) {
        if (sockets != null) {
            sockets.stream().forEach(socket -> {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private class Echoer extends Thread {
        ArrayList<Socket> sockets;

        public Echoer(ArrayList<Socket> sockets) {
            this.sockets = sockets;
        }

        @Override
        public void run() {
            long[] intervals;
            int waveNum = 0;
            try {
                byte[] message = new byte[MESSAGE_LENGTH];
                while (!Thread.currentThread().isInterrupted() && waveNum < WAVE_COUNT) {
                    intervals = new long[sockets.size()];
                    for (int i = 0; i < sockets.size(); i++) {
                        Socket socket = sockets.get(i);
                        int len = socket.getInputStream().read(message);
                        while (len < MESSAGE_LENGTH) {
                            len += socket.getInputStream().read(message, len, MESSAGE_LENGTH - len);
                        }
                        socket.getOutputStream().write(message);
                        sentMessagesCount++;
                        intervals[i] = System.currentTimeMillis();
                    }
                    wavePeriods.add(intervals);
                    waveNum++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}