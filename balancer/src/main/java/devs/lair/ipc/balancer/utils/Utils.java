package devs.lair.ipc.balancer.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static devs.lair.ipc.balancer.utils.Constants.PLAYER_DIR;
import static devs.lair.ipc.balancer.utils.Constants.PLAYER_FILE_SUFFIX;

public class Utils {

    private Utils() {
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
        Objects.requireNonNull(playerName);
        return Paths.get(PLAYER_DIR + "/" + playerName + PLAYER_FILE_SUFFIX);
    }

    public static String generateUniqueName(String[] args, String prefix) {
        Objects.requireNonNull(prefix);

        if (prefix.isEmpty())
            throw new IllegalArgumentException("Prefix is empty!");

        return (args.length > 0 && !args[0].isEmpty())
                ? args[0]
                : prefix + (System.currentTimeMillis() - 1738605400000L);
    }
}
