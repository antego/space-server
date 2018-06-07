package com.github.antego.spaceserver.server;

import java.io.IOException;
import java.net.Socket;
import java.util.Set;
import java.util.logging.Level;


public class SocketThread extends Thread {
    private final Socket sourceSocket;
    private final Object pairedThreadLock = new Object();
    private volatile SocketThread pairedThread;
    private Long closeTime;

    private SessionContext context;

    private Set<SocketThread> socketThreadSet;

    public SocketThread(Socket sourceSocket, Set<SocketThread> socketThreadSet) {
        this.sourceSocket = sourceSocket;
        this.socketThreadSet = socketThreadSet;
    }
    @Override
    public void run() {
        try (Socket socket = this.sourceSocket) {
            if(!ProtocolUtils.doHandshake(socket)) {
                return;
            }
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                ProtocolUtils.processMessage(context);
            }
        } catch (IOException e) {
            Server.logger.log(Level.INFO, "exception in socket thread", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            closePairedThread();
            deleteFromThreadSet();
        }
    }

    public void setContext(SessionContext context) {
        this.context = context;
    }

    private void deleteFromThreadSet() {
        synchronized (socketThreadSet) {
            socketThreadSet.remove(this);
        }
    }

    private void closePairedThread() {
        if (pairedThread != null) {
            try {
                pairedThread.getSocket().getOutputStream().write(new byte[]{5});
            } catch (IOException e) {
                e.printStackTrace();
            }
            pairedThread.setCloseTime(System.currentTimeMillis());
            pairedThread = null;
        }
    }

    public void registerPairedThread(SocketThread pairedThread) {
        if (pairedThread == null) {
            return;
        }
        synchronized (pairedThreadLock) {
            this.pairedThread = pairedThread;
            context.setSlaveThread(pairedThread);
            pairedThreadLock.notify();
        }
    }

    public Socket getSocket() {
        return sourceSocket;
    }

    public Long getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(Long closeTime) {
        this.closeTime = closeTime;
    }

    public void closeSocket() {
        try {
            sourceSocket.close();
        } catch (IOException e) {
            Server.logger.log(Level.INFO, "exception on socket closing");
        }
    }

    public boolean isClientAvailable() {
        try {
            Thread.sleep(20);
            sourceSocket.getOutputStream().write(new byte[]{127});
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }
}
