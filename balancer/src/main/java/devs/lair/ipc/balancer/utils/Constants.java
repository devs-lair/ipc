package devs.lair.ipc.balancer.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants {
    public static final String PLAYER_FILE_SUFFIX = ".move";
    public static final String PLAYER_DIR = "players";
    public static final String CONFIG_DIR = "config";
    public static final String CONFIG_FILE = "config.properties";
    public static final String MEMORY_CONFIG_FILE = "memory.config";
    public static final String INITIAL_PLAYER_COUNT_KEY = "producer.initialPlayerCount";
    public static final String MAX_ATTEMPT_KEY = "arbiter.maxAttempt ";
    public static final int MEMORY_SIZE = 1000;
    public static final int DEFAULT_POLL_TIMEOUT = 1000;

    public static final Path CONFIG_PATH = Paths.get(CONFIG_DIR + "/" + CONFIG_FILE);
    public static final Path MEMORY_CONFIG_PATH = Paths.get(CONFIG_DIR + "/" + MEMORY_CONFIG_FILE);

    private Constants() {
    }
}
