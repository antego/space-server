package com.github.antego.spaceserver.server;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Logger;


public class ProtocolUtils {
    public static final Logger logger = Logger.getLogger(ProtocolUtils.class.getName());

    public static void processMessage(SessionContext context) throws IOException, InterruptedException {
        byte[] message = null;
        while (true) {
            doBusiness(message, context);
            if (context.getPhase() != SessionContext.SessionPhase.SETUP_WORLD) {
                message = readMessage(context);
            }
        }
    }

    private static void setupWorld(SessionContext context) throws IOException {
        context.getMasterThread().getSocket().getOutputStream().write(context.getPlayerSide() == SessionContext.PlayerSide.LEFT ? new byte[]{0} : new byte[]{1});
    }

    private static byte[] readMessage(SessionContext context) throws IOException {
        InputStream inputStream = context.getMasterThread().getSocket().getInputStream();
        byte[] messageType = new byte[1];
        int len = inputStream.read(messageType);
        if (len == -1) {
            //todo exit without exception
            logger.info("End of file while reading type from socket " + context.getMasterThread().getSocket().getInetAddress());
            throw new EOFException();
        }
        byte[] message = new byte[getMessageLength(messageType[0])];
        len = inputStream.read(message);
        if (len == -1) {
            //todo exit without exception
            logger.info("End of file while reading message from socket " + context.getMasterThread().getSocket().getInetAddress());
            throw new EOFException();
        }
        while (len < getMessageLength(messageType[0])) {
            len += inputStream.read(message, len, getMessageLength(messageType[0]) - len);
        }
        return concat(messageType, message);
    }

    private static void doBusiness(byte[] message, SessionContext context) throws IOException, InterruptedException {
        switch (context.getPhase()) {
            case SETUP_WORLD: {
                synchronized (context.slaveMonitor) {
                    while (context.getSlaveThread() == null) {
                        context.slaveMonitor.wait();
                    }
                }
                setupWorld(context);
                context.setPhase(SessionContext.SessionPhase.GAME);
                break;
            }
            case GAME: {
                writeMessage(context.getSlaveThread().getSocket().getOutputStream(), message);
            }
        }
    }

    private static void writeMessage(OutputStream outputStream, byte[] message) throws IOException {
        outputStream.write(message);
    }

    //todo fire and shoot event
    private static int getMessageLength(byte messageType) {
        switch (messageType){
            case 0:
                return 20; //status
            case 1:
                return 4; //rotate
            case 2:
                return 16; //fire
            case 3:
                return 0; //shoot
            case 4:
                return 4; //accel
            case 5:
                return 0; //end session
            default:
                return -1;
        }
    }

    //TODO proper handshake
    public static boolean doHandshake(Socket socket) {
        return true;
    }
    
    private static byte[] concat(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] c = new byte[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }
}