package devs.lair.ipc.rmi;

import devs.lair.ipc.rmi.utils.Move;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static devs.lair.ipc.rmi.utils.CommonUtils.*;

public class Player {
    private final String name;
    private final Path playerFile;
    private final int tick;

    private boolean isStop = false;

    public Player(String name, int tick) {
        this.name = name;
        this.tick = tick;
        this.playerFile = getPathFromName(name);

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
        tryDelete(playerFile);
    }

    private void createPlayerFile() {
        try {
            createDirectoryIfNotExist(playerFile.getParent());
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
        new Player(generateUniqueName(args, "player"), tick)
                .start();
    }
}
