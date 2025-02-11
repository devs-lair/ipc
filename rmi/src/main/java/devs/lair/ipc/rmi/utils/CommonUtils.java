package devs.lair.ipc.rmi.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommonUtils {
    public static final String FILE_SUFFIX = ".move";
    public static final String FILE_DIR = "players";

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
        return path.getFileName().toString().replace(FILE_SUFFIX, "");
    }

    public static Path getPathFromName(String playerName) {
        return Paths.get(FILE_DIR + "/" + playerName + FILE_SUFFIX);
    }

    public static String generateUniqueName(String[] args, String prefix) {
        return (args.length > 0 && !args[0].isEmpty())
                ? args[0]
                : prefix + (System.currentTimeMillis() - 1738605400000L);
    }
}
