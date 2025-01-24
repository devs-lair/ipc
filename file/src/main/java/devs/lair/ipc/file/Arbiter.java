package devs.lair.ipc.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Arbiter {

    public static void main(String[] args) throws IOException, InterruptedException {
        int tick = 1000;

        if (args.length < 2) {
            System.out.println("Передайте имена игроков");
            System.exit(-1);
        }

        String playerOneName = args[0];
        String playerTwoName = args[1];

        Path playerOneFile = Paths.get(playerOneName);
        Path playerTwoFile = Paths.get(playerTwoName);

        if (!Files.exists(playerOneFile)) {
            System.out.println("Первый игрок не начал игру (Файл не найден)");
            System.exit(-1);
        }

        if (!Files.exists(playerTwoFile)) {
            System.out.println("Второй игрок не начал игру (Файл не найден)");
            System.exit(-1);
        }

        int gameNumber = 1;
        while (true) {
            if (Files.size(playerOneFile) != 0 && Files.size(playerTwoFile) != 0) {

                String playerOneMove = new String(Files.readAllBytes(playerOneFile));
                String playerTwoMove = new String(Files.readAllBytes(playerTwoFile));

                System.out.printf("Игра номер %d \n", gameNumber);
                System.out.printf("Ход игрока %s = %s \n", playerOneName, playerOneMove);
                System.out.printf("Ход игрока %s = %s \n", playerTwoName, playerTwoMove);

                if (playerOneMove.equals(playerTwoMove)) {
                    System.out.println("Ничья");
                } else {
                    System.out.printf("Выиграл %s \n", computeWinner(playerOneName, playerTwoName,
                            playerOneMove, playerTwoMove));
                }
                Files.write(playerOneFile, "".getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
                Files.write(playerTwoFile, "".getBytes(), StandardOpenOption.TRUNCATE_EXISTING);

                gameNumber++;
            }
            Thread.sleep(tick);
        }
    }

    private static String computeWinner(String playerOneName, String playerTwoName,
                                        String playerOneMove, String playerTwoMove) {
        if ((playerOneMove.equals("ROCK") && playerTwoMove.equals("SCISSORS"))
                || (playerOneName.equals("PAPER") && playerTwoMove.equals("ROCK")
                || (playerOneName.equals("SCISSORS") && playerTwoMove.equals("PAPER")))) {
            return playerOneName;
        }
        return playerTwoName;
    }
}
