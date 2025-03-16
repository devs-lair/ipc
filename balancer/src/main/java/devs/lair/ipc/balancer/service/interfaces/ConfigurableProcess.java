package devs.lair.ipc.balancer.service.interfaces;

import devs.lair.ipc.balancer.service.ConfigProvider;

import static java.lang.Thread.currentThread;

public abstract class ConfigurableProcess {
    protected final ConfigProvider configProvider = new ConfigProvider();
    private final Thread shutdown = new Thread(this::stop);

    protected void start() {
        Runtime.getRuntime().addShutdownHook(shutdown);
        configProvider.init();
    }

    protected void stop() {
        configProvider.close();
        currentThread().interrupt();
    }

    protected void removeShutdownHook() {
        Runtime.getRuntime().removeShutdownHook(shutdown);
    }
}
