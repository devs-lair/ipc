package devs.lair.ipc.balancer;

import devs.lair.ipc.balancer.service.ConfigProvider;
import devs.lair.ipc.balancer.utils.Move;
import devs.lair.ipc.balancer.utils.Utils;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static devs.lair.ipc.balancer.utils.Utils.*;

public class Player {
    private final String name;
    private final Path playerFile;
    private final ConfigProvider configProvider = new ConfigProvider();
    private boolean isStop = false;

    public Player() {
        this(generateUniqueName(null, "player"));
    }

    public Player(String name) {
        if (Utils.isNullOrEmpty(name))
            throw new IllegalArgumentException("Не верное имя!");

        this.name = name;
        this.playerFile = getPathFromName(name);

        createPlayerFile();
    }

    public void start() {
        configProvider.init();
        while (!isStop) {
            try {
                if (Files.size(playerFile) == 0) {
                    Files.write(playerFile, Move.getRandomMoveBytes());
                }
                Thread.sleep(configProvider.getPlayerTick());
            } catch (InterruptedException | IOException e) {
                break;
            }
        }
    }

    public void stop() {
        isStop = true;
        configProvider.close();
        tryDelete(playerFile);
    }

    private void createPlayerFile() {
        try {
            createDirectoryIfNotExist(playerFile.getParent());
            Files.createFile(playerFile);
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    e instanceof FileAlreadyExistsException
                    ? "Игрок с именем " + name + " уже играет (есть файл)"
                    : "При создании файла игрока произошла ошибка");
        }
    }

    private static Player p; // for tests
    public static void main(String[] args) {
        (p = new Player(generateUniqueName(args, "player"))).start();
    }
}
