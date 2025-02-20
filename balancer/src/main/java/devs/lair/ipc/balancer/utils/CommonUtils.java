package devs.lair.ipc.balancer.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommonUtils {
    public static final String PLAYER_FILE_SUFFIX = ".move";
    public static final String PLAYER_DIR = "players";
    public static final String CONFIG_DIR = "config";
    public static final String CONFIG_FILE = "config.properties";
    public static final String MEMORY_CONFIG_FILE = "memory.config";
    public static final int MEMORY_SIZE = 1000;
    public static final int DEFAULT_POLL_TIMEOUT = 1000;

    public static final Path CONFIG_PATH = Paths.get(CONFIG_DIR + "/" + CONFIG_FILE);
    public static final Path MEMORY_CONFIG_PATH = Paths.get(CONFIG_DIR + "/" + MEMORY_CONFIG_FILE);

    private CommonUtils() {
    }

    public static boolean tryDelete(Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static void createDirectoryIfNotExist(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }
    }

    public static String getNameFromPath(Path path) {
        return path.getFileName().toString().replace(PLAYER_FILE_SUFFIX, "");
    }

    public static Path getPathFromName(String playerName) {
        return Paths.get(PLAYER_DIR + "/" + playerName + PLAYER_FILE_SUFFIX);
    }

    public static String generateUniqueName(String[] args, String prefix) {
        return (args.length > 0 && !args[0].isEmpty())
                ? args[0]
                : prefix + (System.currentTimeMillis() - 1738605400000L);
    }
}
