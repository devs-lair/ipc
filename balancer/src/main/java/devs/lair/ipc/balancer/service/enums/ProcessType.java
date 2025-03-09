package devs.lair.ipc.balancer.service.enums;

import devs.lair.ipc.balancer.*;

public enum ProcessType {
    PLAYER_PRODUCER(PlayerProducer.class),
    CONFIG_LOADER(ConfigLoader.class),
    ARBITER(Arbiter.class),
    PLAYER(Player.class),
    CONTROLLER(GameController.class);

    private final Class<?> mainClass;

    ProcessType(Class<?> mainClass) {
        this.mainClass = mainClass;
    }

    public Class<?> getMainClass() {
        return mainClass;
    }
}