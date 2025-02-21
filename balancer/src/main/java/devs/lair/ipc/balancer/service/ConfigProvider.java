package devs.lair.ipc.balancer.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Properties;
import java.util.function.Predicate;

import static devs.lair.ipc.balancer.utils.Constants.*;
import static java.nio.file.StandardOpenOption.READ;

public class ConfigProvider {
    private final Properties props = new Properties();
    private final int pollTimeout;

    private MappedByteBuffer memory;
    private Thread watchThread;

    //Params
    private int playerTick = 500;
    private int arbiterTick = 500;
    private int maxPlayerCount = 4;
    private int spawnPeriod = 1000;

    //Controls
    private byte currentVersion = 0;
    private boolean isStop = false;
    private boolean updating = false;

    public ConfigProvider() {
        this(DEFAULT_POLL_TIMEOUT);
    }

    public ConfigProvider(int pollTimeout) {
        if (pollTimeout < 0) {
            throw new IllegalArgumentException("Таймаут должен быть строго больше нуля");
        }

        this.pollTimeout = pollTimeout;
    }

    public void init() {
        loadConfig();
        startWatch();
    }

    public void loadConfig() {
        try {
            if (Files.exists(MEMORY_CONFIG_PATH)) {
                if (memory == null) initMemoryBuffer();

                if (memory != null) {
                    byte configVersion = memory.get(0);
                    if (configVersion != -1 && configVersion > currentVersion) {
                        byte[] configBytes = new byte[999];
                        currentVersion = configVersion;
                        memory.get(1, configBytes);
                        readConfig(configBytes);
                    }
                }
            }
        } catch (Exception e) {
            memory = null;
            System.out.println("Ошибка при чтении конфига из памяти: " + e.getMessage());
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

    public void stop() {
        isStop = true;
        if (watchThread != null) {
            watchThread.interrupt();
        }
    }

    private void initMemoryBuffer() {
        try (FileChannel fc = (FileChannel) Files.newByteChannel(MEMORY_CONFIG_PATH, READ)) {
            memory = fc.map(FileChannel.MapMode.READ_ONLY, 0, MEMORY_SIZE);
        } catch (IOException e) {
            System.out.println("Не удалось создать буфер памяти " + e.getMessage());
        }
    }

    private void readConfig(byte[] configBytes) {
        try {
            updating = true;
            props.clear();
            props.load(new ByteArrayInputStream(configBytes));

            //Player params
            playerTick = readPositiveInt("player.tick", playerTick);

            //Arbiter params
            arbiterTick = readPositiveInt("arbiter.tick", arbiterTick);

            //Producer params
            maxPlayerCount = readPositiveInt("producer.maxPlayers", maxPlayerCount);
            spawnPeriod = readPositiveInt("producer.spawnPeriod", spawnPeriod);
        } catch (IOException e) {
            System.out.println("Не удалось прочитать конфиг байтов памяти ");
        } catch (NumberFormatException e) {
            System.out.println("Ошибка парсинга параметра конфига");
        } finally {
            updating = false;
        }
    }

    private int readPositiveInt(String propertyName, int defaultValue) {
        return getInt(propertyName, defaultValue, value -> value > 0);
    }

    public String getProperty(String propertyName) throws IllegalStateException {
        if (!updating) {
            return props.getProperty(propertyName);
        }
        throw new IllegalStateException("Идет обновление. Приходите позже");
    }

    public int getInt(String propertyName, int defaultValue) {
        return getInt(propertyName, defaultValue, null);
    }

    public int getInt(String propertyName, int defaultValue, Predicate<Integer> checker) {
        try {
            String propertyValue = getProperty(propertyName);
            if (propertyValue != null && !propertyValue.isEmpty()) {
                int invValue = Integer.parseInt(propertyValue);
                if (checker != null && checker.test(invValue)) {
                    return invValue;
                }
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
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
}
