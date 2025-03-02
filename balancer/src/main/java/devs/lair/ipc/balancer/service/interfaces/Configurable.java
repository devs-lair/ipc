package devs.lair.ipc.balancer.service.interfaces;

import devs.lair.ipc.balancer.service.ConfigProvider;

public interface Configurable extends AutoCloseable {
    ConfigProvider configProvider = new ConfigProvider();

    default void closeProvider() {
        configProvider.close();
    }
}
