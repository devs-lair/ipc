package devs.lair.ipc.balancer.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Properties;

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

    //Controls
    private boolean isStop = false;
    private byte currentVersion = 0;

    public ConfigProvider() {
        this(DEFAULT_POLL_TIMEOUT);
    }

    public ConfigProvider(int pollTimeout) {
        if (pollTimeout < 0) {
            throw new IllegalArgumentException("Таймаут должен быть строго больше нуля");
        }

        this.pollTimeout = pollTimeout;
        start();
    }

    private void start() {
        watchThread = new Thread(() -> {
            byte[] configBytes = new byte[999];
            do {
                try {
                    if (Files.exists(MEMORY_CONFIG_PATH)) {
                        if (memory == null) initMemoryBuffer();

                        if (memory != null) {
                            byte configVersion = memory.get(0);
                            if (configVersion != -1 && configVersion > currentVersion) {
                                currentVersion = configVersion;
                                memory.get(1, configBytes);
                                readConfig(configBytes);
                            }
                        }
                    }
                    Thread.sleep(pollTimeout);
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        break;
                    } else {
                        memory = null;
                        System.out.println("Ошибка при чтении конфига из памяти: " + e.getMessage());
                    }
                }
            } while (!isStop && pollTimeout != 0);
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
            props.clear();
            props.load(new ByteArrayInputStream(configBytes));
            playerTick = readPositiveInt("player.tick", playerTick);
            arbiterTick = readPositiveInt("arbiter.tick", arbiterTick);
        } catch (IOException e) {
            System.out.println("Не удалось прочитать конфиг байтов памяти ");
        } catch (NumberFormatException e) {
            System.out.println("Ошибка парсинга параметра конфига");
        }
    }

    private int readPositiveInt(String propertyName, int defaultValue) {
        String property = props.getProperty(propertyName);
        if (property == null || property.isEmpty()) {
            return defaultValue;
        } else {
            int value = Integer.parseInt(property);
            return value < 0 ? defaultValue : value;
        }
    }

    public int getPlayerTick() {
        return playerTick;
    }

    public int getArbiterTick() {
        return arbiterTick;
    }
}
