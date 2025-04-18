package devs.lair.ipc.jmx.service.interfaces;

public interface ConfigProviderMBean {
    String getProperty(String propertyName) throws IllegalStateException;

    int getPlayerTick();

    int getArbiterTick();

    int getMaxPlayerCount();

    int getProducerTick();

    int getMaxRound();

    int getArbiterZombieTimeout();

    void setPlayerTick(int playerTick);

    void setArbiterTick(int arbiterTick);

    void setMaxPlayerCount(int maxPlayerCount);

    void setProducerTick(int producerTick);

    void setMaxRound(int maxRound);

    void setArbiterZombieTimeout(int arbiterZombieTimeout);
}
