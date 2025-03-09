package devs.lair.ipc.balancer.service.interfaces;

import devs.lair.ipc.balancer.service.ConfigProvider;

import static java.lang.Thread.currentThread;

public abstract class ConfigurableProcess {
    protected final ConfigProvider configProvider = new ConfigProvider();

    protected void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        configProvider.init();
    }

    protected void stop() {
        configProvider.close();
        currentThread().interrupt();
    }
}
