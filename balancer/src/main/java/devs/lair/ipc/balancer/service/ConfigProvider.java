package devs.lair.ipc.balancer.service;

import devs.lair.ipc.balancer.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Properties;
import java.util.function.Predicate;

import static devs.lair.ipc.balancer.utils.Constants.*;
import static java.nio.file.StandardOpenOption.READ;

public class ConfigProvider implements AutoCloseable {
    private final Properties props = new Properties();

    private MappedByteBuffer memory;
    private Thread watchThread;

    //Controls
    private final int pollTimeout;
    private byte currentVersion = 0;
    private boolean isStop = false;
    private boolean updating = false;

    //Params
    private int playerTick = 500;
    private int arbiterTick = 500;
    private int maxPlayerCount = 4;
    private int spawnPeriod = 1000;
    private int maxRound = 5;

    public ConfigProvider() {
        this(DEFAULT_POLL_TIMEOUT);
    }

    public ConfigProvider(int pollTimeout) {
        this.pollTimeout = Utils.checkInt(pollTimeout, p -> p > 0);
    }

    public void init() {
        loadConfig();
        startWatch();
    }

    public void loadConfig() {
        try {
            if (Files.exists(MEMORY_CONFIG_PATH)) {
                if (memory == null) initMemoryBuffer();

                if (memory.get(0) > currentVersion) {
                    currentVersion = memory.get(0);
                    readConfig();
                }
            }
        } catch (Exception e) {
            memory = null;
            System.out.println("Ошибка при чтении конфига из памяти: " + e.getMessage());
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
        });
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
        spawnPeriod = readPositiveInt("producer.spawnPeriod", spawnPeriod);
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

    private int readPositiveInt(String propertyName, int defaultValue) {
        return getInt(propertyName, defaultValue, value -> value > 0);
    }

    @Override
    public void close() {
        isStop = true;
        if (watchThread != null) {
            watchThread.interrupt();
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

    public String getProperty(String propertyName) throws IllegalStateException {
        if (updating) {
            throw new IllegalStateException("Идет обновление. Приходите позже");
        }
        String property = props.getProperty(propertyName);
        return Utils.isNullOrEmpty(property) ? null : property.trim();
    }

    public int getPlayerTick() {
        return playerTick;
    }

    public int getArbiterTick() {
        return arbiterTick;
    }

    public int getMaxPlayerCount() {
        return maxPlayerCount;
    }

    public long getSpawnPeriod() {
        return spawnPeriod;
    }

    public int getMaxRound() {
        return maxRound;
    }
}
