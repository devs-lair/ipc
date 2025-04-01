package devs.lair.ipc.jmx.service.interfaces;

import devs.lair.ipc.jmx.service.ConfigProvider;
import sun.misc.Signal;

public abstract class ConfigurableProcess {
    protected final ConfigProvider configProvider = new ConfigProvider();
    protected Thread mainTread;

    protected String name;
    protected boolean interrupted = false;

    public ConfigurableProcess() {
        init();
    }

    protected void init() {
        Signal.handle(new Signal("TERM"), sig -> stop());
        Signal.handle(new Signal("INT"), sig -> stop());

        configProvider.init();
        mainTread = Thread.currentThread();
    }

    public void stop() {
        configProvider.close();

        if (mainTread != null) {
            mainTread.interrupt();
            mainTread = null;
        }

        interrupted = true;
    }
}
