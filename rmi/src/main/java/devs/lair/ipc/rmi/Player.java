package devs.lair.ipc.rmi;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Random;

public class Player {
    private final String FILE_SUFFIX = ".move";
    private final String FILE_DIR = "players";
    private final Random random = new Random();
    private final String name;
    private final Path playerFile;
    private final int tick;

    private final Map<Integer, String> moves = Map.of(
            0, "ROCK",
            1, "SCISSORS",
            2, "PAPER");

    private boolean isStop = false;

    public Player(String name, int tick) {
        this.name = name;
        this.tick = tick;
        this.playerFile = Paths.get("./" + FILE_DIR + "/" + name + FILE_SUFFIX);

        checkArgs(name, tick);
        createPlayerFile();
    }

    public void start() {
        while (!isStop) {
            try {
                if (Files.size(playerFile) == 0) {
                    Files.write(playerFile, makeMove());
                }
                Thread.sleep(tick);
            } catch (InterruptedException | IOException e) {
               break;
            }
        }
    }

    public void stop() {
        isStop = true;
        try {
            Files.delete(playerFile);
        } catch (IOException e) {
            if (e instanceof NoSuchFileException) {
                System.out.println("Файл уже удален");
            }
        }
    }

    private void createPlayerFile() {
        try {
            if (!Files.exists(playerFile.getParent())) {
                Files.createDirectory(playerFile.getParent());
            }

            Files.createFile(playerFile);
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        } catch (IOException e) {
            throw new IllegalArgumentException(e instanceof FileAlreadyExistsException
                    ? "Игрок с именем " + name + " уже играет (есть файл)"
                    : "При создании файла игрока произошла ошибка");
        }
    }

    private byte[] makeMove() {
        int rnd = random.nextInt(3);
        return moves.get(rnd).getBytes();
    }

    private void checkArgs(String name, int tick) {
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
