package devs.lair.ipc.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;

public class Player {
    static Random random = new Random();
    static Map<Integer, String> moves = Map.of(
            0, "ROCK",
            1, "SCISSORS",
            2, "PAPER");

    public static void main(String[] args) throws IOException, InterruptedException {
        int tick = 1000;

        if (args.length == 0) {
            System.out.println("Передайте имя игрока в параметрах!");
            System.exit(-1);
        }

        String name = args[0];
        Path path = Paths.get(name);

        if (!Files.exists(path)) {
            Files.createFile(path);
        }

        while (true) {
            if (Files.size(path) == 0) {
                Files.write(path, makeMove());
            }
            Thread.sleep(tick);
        }
    }

    private static byte[] makeMove() {
        int rnd = random.nextInt(3);
        return moves.get(rnd).getBytes();
    }
}

