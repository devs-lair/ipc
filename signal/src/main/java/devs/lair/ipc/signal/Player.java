package devs.lair.ipc.signal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;

public class Player {

    private final Random random = new Random();
    private final String name;
    private final int tick;
    private final Map<Integer, String> moves = Map.of(
            0, "ROCK",
            1, "SCISSORS",
            2, "PAPER");
    private boolean isStop = false;

    public Player(String name, int tick) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Не верное имя!");
        }

        if (tick < 0) {
            throw new IllegalArgumentException("Тик должен быть строго больше нуля");
        }

        this.name = name;
        this.tick = tick;
    }

    public void start() throws IOException, InterruptedException {
        Path path = Paths.get(name);

        if (!Files.exists(path)) {
            Files.createFile(path);
        }

        while (!isStop) {
            if (Files.size(path) == 0) {
                Files.write(path, makeMove());
            }
            Thread.sleep(tick);
        }

        Files.delete(path);
    }

    public void stop() {
        isStop = true;
        while (Files.exists(Paths.get(name))) ;
    }

    private byte[] makeMove() {
        int rnd = random.nextInt(3);
        return moves.get(rnd).getBytes();
    }
}

