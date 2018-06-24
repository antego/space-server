package com.github.antego.spaceserver.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AcceptThread extends Thread implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(AcceptThread.class.getName());

    private final ServerSocket serverSocket;
    private final Set<SocketThread> socketThreadSet = new HashSet<>();
    private SocketThread unpairedThread;
    private ScheduledExecutorService setCleaner = Executors.newSingleThreadScheduledExecutor();

    public AcceptThread(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
        setCleaner.scheduleAtFixedRate(() -> {
            synchronized (socketThreadSet) {
                socketThreadSet.stream()
                                .filter(st -> st.getCloseTime() != null &&
                                        System.currentTimeMillis() - st.getCloseTime() > 10_000)
                                .forEach(st -> {
                                    st.closeSocket();
                                    socketThreadSet.remove(st);
                                });
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        logger.info("Accept thread started");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                SocketThread socketThread = new SocketThread(socket, socketThreadSet);


                SessionContext context = new SessionContext();
                context.setPhase(SessionContext.SessionPhase.SETUP_WORLD);
                context.setMasterThread(socketThread);
                if (unpairedThread != null && !unpairedThread.isClientAvailable()) {
                    unpairedThread.closeSocket();
                    unpairedThread.interrupt();
                    socketThreadSet.remove(unpairedThread);
                    unpairedThread = null;
                }
                if(unpairedThread == null) {
                    context.setPlayerSide(SessionContext.PlayerSide.LEFT);
                    socketThread.setContext(context);
                    unpairedThread = socketThread;
                } else {
                    context.setPlayerSide(SessionContext.PlayerSide.RIGHT);
                    socketThread.setContext(context);
                    unpairedThread.registerPairedThread(socketThread);
                    socketThread.registerPairedThread(unpairedThread);
                    unpairedThread = null;
                }
                socketThread.start();
                synchronized (socketThreadSet) {
                    socketThreadSet.add(socketThread);
                }
            }

        } catch (IOException e) {
            logger.log(Level.INFO, "Exception in accept thread", e);
        }
        close();
        setCleaner.shutdown();
    }

    @Override
    public void close() {
        logger.info("Close accept thread");
        this.interrupt();
        synchronized (socketThreadSet) {
            socketThreadSet.stream().forEach((thread) -> {
                thread.closeSocket();
            });
        }
    }
}
