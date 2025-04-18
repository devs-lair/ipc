package devs.lair.ipc.jmx.service.model;

import devs.lair.ipc.jmx.service.enums.ProcessType;

public class ArbiterProcess extends ActorProcess {
    private long lastTimeUsage;
    private final String[] players = new String[2];

    public ArbiterProcess(ProcessHandle processHandle, ProcessType type, String name) {
        super(processHandle, type, name);

        lastTimeUsage = System.currentTimeMillis();
    }

    public void setPlayer(String playerName, int position) {
        players[position] = playerName;
        lastTimeUsage = System.currentTimeMillis();
    }

    public void removePlayer(String playerName) {
        if (playerName.equals(players[0])) {
            players[0] = null;
        }

        if (playerName.equals(players[1])) {
            players[1] = null;
        }

        lastTimeUsage = System.currentTimeMillis();
    }

    public long getLastTimeUsage() {
        return lastTimeUsage;
    }

    public void setLastTimeUsage(long lastTimeUsage) {
        this.lastTimeUsage = lastTimeUsage;
    }

    public String[] getPlayers() {
        return players;
    }
}
