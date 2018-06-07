package com.github.antego.spaceserver.server;


public class SessionContext {
    private SessionPhase phase;
    private PlayerSide playerSide;
    private volatile SocketThread masterThread;
    private volatile SocketThread slaveThread;
    public final Object slaveMonitor = new Object();

    public SessionPhase getPhase() {
        return phase;
    }

    public void setPhase(SessionPhase phase) {
        this.phase = phase;
    }

    public PlayerSide getPlayerSide() {
        return playerSide;
    }

    public void setPlayerSide(PlayerSide playerSide) {
        this.playerSide = playerSide;
    }

    public SocketThread getMasterThread() {
        return masterThread;
    }

    public void setMasterThread(SocketThread masterThread) {
        this.masterThread = masterThread;
    }

    public SocketThread getSlaveThread() {
        return slaveThread;
    }

    public void setSlaveThread(SocketThread slaveThread) {
        synchronized (slaveMonitor) {
            this.slaveThread = slaveThread;
            slaveMonitor.notify();
        }
    }

    public enum SessionPhase {WAITING_FOR_GAME, SETUP_WORLD, GAME}
    public enum PlayerSide{LEFT, RIGHT}
}
