package devs.lair.ipc.jmx.utils;

import devs.lair.ipc.jmx.service.enums.ProcessType;
import devs.lair.ipc.jmx.service.model.ActorProcess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static devs.lair.ipc.jmx.utils.Constants.PLAYER_DIR;
import static devs.lair.ipc.jmx.utils.Constants.PLAYER_FILE_SUFFIX;

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

        return (args != null && args.length > 0 && !args[0].isEmpty())
                ? args[0]
                : prefix + UUID.randomUUID();
    }

    public static String generateUniqueName(String prefix) {
        return prefix + UUID.randomUUID();
    }

    public static int checkInt(int intValue, Predicate<Integer> predicate) {
        if (!predicate.test(intValue)) {
            throw new IllegalArgumentException("Аргумент не прошел проверку: " + intValue);
        }
        return intValue;
    }

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isNullOrEmpty(String[] s) {
        return s == null || s.length == 0;
    }

    public static ProcessHandle findZombieProcess(String argumentValue) {
        return findProcessByArgument(argumentValue);
    }

    public static ProcessHandle findProcessByArgument(String argumentValue) {
        for (ProcessHandle process : ProcessHandle.allProcesses().toList()) {
            Optional<String[]> optionalArguments = process.info().arguments();
            if (optionalArguments.isPresent()) {
                String[] arguments = optionalArguments.get();
                for (String argument : arguments) {
                    if (argument.equals(argumentValue)) {
                        return process;
                    }
                }
            }
        }

        return null;
    }

    public static ActorProcess findProcessByFile(Path p) {
        ProcessHandle processByArgument = Utils.findProcessByArgument(p.getFileName().toString());
        if (processByArgument != null) {
            return new ActorProcess(processByArgument,
                    ProcessType.ARBITER,
                    p.getFileName().toString());
        } else {
            return null;
        }
    }
}
