package devs.lair.ipc.rmi;

import devs.lair.ipc.rmi.utils.FilesUtils;
import devs.lair.ipc.rmi.utils.Move;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Player {
    private final String FILE_SUFFIX = ".move";
    private final String FILE_DIR = "players";

    private final String name;
    private final Path playerFile;
    private final int tick;

    private boolean isStop = false;

    public Player(String name, int tick) {
        this.name = name;
        this.tick = tick;
        this.playerFile = Paths.get(FILE_DIR + "/" + name + FILE_SUFFIX);

        checkArguments();
        createPlayerFile();
    }

    public void start() {
        while (!isStop) {
            try {
                if (Files.size(playerFile) == 0) {
                    Files.write(playerFile, Move.getRandomMoveBytes());
                }
                Thread.sleep(tick);
            } catch (InterruptedException | IOException e) {
                break;
            }
        }
    }

    public void stop() {
        isStop = true;
        FilesUtils.tryDelete(playerFile);
    }

    private void createPlayerFile() {
        try {
            FilesUtils.createDirectoryIfNotExist(playerFile.getParent());
            Files.createFile(playerFile);
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        } catch (IOException e) {
            throw new IllegalArgumentException(e instanceof FileAlreadyExistsException
                    ? "Игрок с именем " + name + " уже играет (есть файл)"
                    : "При создании файла игрока произошла ошибка");
        }
    }

    private void checkArguments() {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Не верное имя!");
        }
        if (tick < 0) {
            throw new IllegalArgumentException("Тик должен быть строго больше нуля");
        }
    }

    public static void main(String[] args) {
        int tick = 500;

        String name = (args.length > 0 && !args[0].isEmpty())
                ? args[0]
                : "player" + (System.currentTimeMillis() - 1738605400000L);
        try {
            new Player(name, tick).start();
        } catch (Exception ex) {
            System.out.println("Произошла ошибка: " + ex.getMessage());
        }
    }
}
