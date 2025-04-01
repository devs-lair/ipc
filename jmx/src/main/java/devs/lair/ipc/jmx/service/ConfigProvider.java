package devs.lair.ipc.jmx.service;

import devs.lair.ipc.jmx.service.interfaces.ConfigProviderMBean;
import devs.lair.ipc.jmx.utils.Utils;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Properties;
import java.util.function.Predicate;

import static devs.lair.ipc.jmx.utils.Constants.*;
import static java.nio.file.StandardOpenOption.READ;

public class ConfigProvider implements AutoCloseable, ConfigProviderMBean {
    private final Properties props = new Properties();

    private MappedByteBuffer memory;
    private Thread watchThread;

    //Controls
    private final int pollTimeout;
    private byte currentVersion = 0;
    private boolean isStop = false;
    private boolean updating = false;

    //Default params
    private int playerTick = 500;
    private int arbiterTick = 500;
    private int maxPlayerCount = 4;
    private int producerTick = 100;
    private int maxRound = 5;

    public ConfigProvider() {
        this(DEFAULT_POLL_TIMEOUT);
    }

    public ConfigProvider(int pollTimeout) {
        this.pollTimeout = Utils.checkInt(pollTimeout, p -> p > 0);
    }

    public void init() {
        registerMBean();
        loadConfig();
        startWatch();
    }

    private void registerMBean() {
        try {
            ObjectName objectName = new ObjectName("devs.lair.ipc:actor=configProvider");
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            StandardMBean standardMBean = new StandardMBean(this, ConfigProviderMBean.class);
            server.registerMBean(standardMBean, objectName);
        } catch (Exception e) {
            System.out.println("Не удалось зарегистрировать MBean = ConfigProvider");
        }
    }

    public void loadConfig() {
        try {
            if (!Files.exists(MEMORY_CONFIG_PATH)) {
                throw new NoSuchFileException(MEMORY_CONFIG_FILE);
            }

            if (memory == null) initMemoryBuffer();
            if (memory.get(0) > currentVersion) {
                currentVersion++;
                readConfig();
            }
        } catch (Exception e) {
            //Maybe write log
            memory = null;
            currentVersion = 0;
        }
    }

    private void initMemoryBuffer() throws IOException {
        try (FileChannel fc = (FileChannel) Files.newByteChannel(MEMORY_CONFIG_PATH, READ)) {
            memory = fc.map(FileChannel.MapMode.READ_ONLY, 0, MEMORY_SIZE);
        }
    }

    public void startWatch() {
        watchThread = new Thread(() -> {
            while (!isStop) {
                try {
                    Thread.sleep(pollTimeout);
                    loadConfig();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "Config Provider Thread");
        watchThread.start();
    }

    private void readConfig() {
        reloadProperties();

        //Player params
        playerTick = readPositiveInt("player.tick", playerTick);

        //Arbiter params
        arbiterTick = readPositiveInt("arbiter.tick", arbiterTick);
        maxRound = readPositiveInt("arbiter.maxRound", maxRound);

        //Producer params
        maxPlayerCount = readPositiveInt("producer.maxPlayers", maxPlayerCount);
        producerTick = readPositiveInt("producer.tick", producerTick);
    }

    private void reloadProperties() {
        try {
            final byte[] configBytes = new byte[999];
            memory.get(1, configBytes);

            updating = true;
            props.clear();
            props.load(new ByteArrayInputStream(configBytes));
        } catch (Exception e) {
            System.out.println("Не удалось прочитать конфиг байтов памяти ");
        } finally {
            updating = false;
        }
    }

    public int readPositiveInt(String propertyName, int defaultValue) {
        return getInt(propertyName, defaultValue, value -> value > 0);
    }

    @Override
    public void close() {
        if (isStop) return;

        isStop = true;
        if (watchThread != null) {
            watchThread.interrupt();
            watchThread = null;
        }
    }

    public int getInt(String propertyName, int defaultValue, Predicate<Integer> checker) {
        try {
            String propertyValue = getProperty(propertyName);
            return Utils.isNullOrEmpty(propertyValue)
                    ? defaultValue
                    : Utils.checkInt(Integer.parseInt(propertyValue), checker);
        } catch (Exception e) {
            System.out.printf("Ошибка парсинга %s %s \n", propertyName, e.getMessage());
            return defaultValue;
        }
    }

    @Override
    public String getProperty(String propertyName) throws IllegalStateException {
        if (updating) {
            throw new IllegalStateException("Идет обновление. Приходите позже");
        }
        String property = props.getProperty(propertyName);
        return Utils.isNullOrEmpty(property) ? null : property.trim();
    }

    @Override
    public int getPlayerTick() {
        return playerTick;
    }

    @Override
    public int getArbiterTick() {
        return arbiterTick;
    }

    @Override
    public int getMaxPlayerCount() {
        return maxPlayerCount;
    }

    @Override
    public int getProducerTick() {
        return producerTick;
    }

    @Override
    public int getMaxRound() {
        return maxRound;
    }

    @Override
    public void setPlayerTick(int playerTick) {
        this.playerTick = playerTick;
    }

    @Override
    public void setArbiterTick(int arbiterTick) {
        this.arbiterTick = arbiterTick;
    }

    @Override
    public void setMaxPlayerCount(int maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
    }

    @Override
    public void setProducerTick(int producerTick) {
        this.producerTick = producerTick;
    }

    @Override
    public void setMaxRound(int maxRound) {
        this.maxRound = maxRound;
    }
}
