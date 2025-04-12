package devs.lair.ipc.jmx;

import devs.lair.ipc.jmx.service.interfaces.ConfigurableProcess;
import devs.lair.ipc.jmx.utils.Move;
import devs.lair.ipc.jmx.utils.Utils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static devs.lair.ipc.jmx.service.enums.ProcessType.PLAYER;
import static devs.lair.ipc.jmx.utils.Utils.*;
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

        Path playerFile = getPathFromName(name);
        try (configProvider) {
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
            tryDelete(playerFile);
        }
    }

    public static void main(String[] args) {
        new Player(Utils.isNullOrEmpty(args) ? null : args[0]).start();
    }
}
