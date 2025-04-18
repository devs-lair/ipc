package devs.lair.ipc.jmx.service;

public class Balancer {
    private final PlayerProvider playerProvider;
    private final ArbiterProvider arbiterProvider;
    private final ConfigProvider configProvider;

    public Balancer(PlayerProvider playerProvider,
                    ArbiterProvider arbiterProvider,
                    ConfigProvider configProvider) {
        this.playerProvider = playerProvider;
        this.arbiterProvider = arbiterProvider;
        this.configProvider = configProvider;
    }

    public void init() {
        arbiterProvider.startArbiter(1);
    }

    public void balance() {
        arbiterProvider.removeDeadProcess();
        int arbitersCount = arbiterProvider.getArbitersCount();
        int querySize = playerProvider.getQuerySize();

        //Balance
        if (arbitersCount == 0 || querySize > 20) {
            arbiterProvider.startArbiter(1);
        } else if (arbitersCount > 1 && playerProvider.getQuerySize() <= 10) {
            arbiterProvider.finishArbiter(1);
        }

        //Zombie check
        int zombieTimeout = configProvider.getArbiterZombieTimeout();
        arbiterProvider.findAndKillZombie(playerProvider, zombieTimeout);
    }
}
