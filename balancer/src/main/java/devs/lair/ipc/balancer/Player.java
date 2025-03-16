package devs.lair.ipc.balancer;

import devs.lair.ipc.balancer.service.interfaces.ConfigurableProcess;
import devs.lair.ipc.balancer.utils.Move;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static devs.lair.ipc.balancer.utils.Utils.*;
import static java.lang.Thread.currentThread;
import static java.nio.file.StandardOpenOption.WRITE;

public class Player extends ConfigurableProcess {
    private final String name;

    public Player() {
        this.name = generateUniqueName("player");
    }

    @Override
    public void start() {
        super.start();

        try {
            Path playerFile = getPathFromName(name);
            createDirectoryIfNotExist(playerFile.getParent());
            Files.createFile(playerFile);

            while (!currentThread().isInterrupted()) {
                if (Files.size(playerFile) == 0) {
                    Files.write(playerFile, Move.getRandomMoveBytes(), WRITE);
                }
                Thread.sleep(configProvider.getPlayerTick());
            }

        } catch (FileAlreadyExistsException e) {
            System.out.println("Игрок с именем " + name + " уже играет " + e.getMessage());
        } catch (NoSuchFileException e) {
            System.out.println("Нет файла игрока " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Основной поток был прерван");
        } catch (Exception e) {
            System.out.println("Произошла непредвиденная ошибка: " + e.getMessage());
        }

        stop();
    }

    @Override
    public void stop() {
        super.stop();
        tryDelete(getPathFromName(name));
    }

    public static void main(String[] args) {
        new Player().start();
    }
}
