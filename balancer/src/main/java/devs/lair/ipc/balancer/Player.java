package devs.lair.ipc.balancer;

import devs.lair.ipc.balancer.service.interfaces.ConfigurableProcess;
import devs.lair.ipc.balancer.utils.Move;
import devs.lair.ipc.balancer.utils.Utils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static devs.lair.ipc.balancer.service.enums.ProcessType.PLAYER;
import static devs.lair.ipc.balancer.utils.Utils.*;
import static java.lang.Thread.currentThread;
import static java.nio.file.StandardOpenOption.WRITE;

public class Player extends ConfigurableProcess {

    public Player(String name) {
        this.name = name  == null
                ? generateUniqueName(PLAYER.name().toLowerCase())
                : name;
    }

    public void start() {
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));

        try (configProvider) {
            Path playerFile = getPathFromName(name);
            //Move to system check
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
        } finally {
            tryDelete(getPathFromName(name));
        }
    }

    public static void main(String[] args) {
        new Player(Utils.isNullOrEmpty(args) ? null : args[0]).start();
    }
}
