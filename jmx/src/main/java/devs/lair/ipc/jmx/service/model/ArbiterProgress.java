package devs.lair.ipc.jmx.service.model;

public class ArbiterProgress {
    private long lastTimeUsage;
    private final String arbiterName;
    private final String[] players = new String[2];

    public ArbiterProgress(String name) {
        this.arbiterName = name;
    }

    public void setPlayer(String playerName, int position) {
        players[position] = playerName;
        lastTimeUsage = System.currentTimeMillis();
    }

    public long getLastTimeUsage() {
        return lastTimeUsage;
    }

    public String[] getPlayers() {
        return players;
    }

    public String getArbiterName() {
        return arbiterName;
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
}
