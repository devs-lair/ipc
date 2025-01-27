package devs.lair.ipc.signal;

import java.io.IOException;
import java.nio.file.*;

public class Arbiter {
    private final String playerOneName;
    private final String playerTwoName;
    private final int tick;

    public Arbiter(String playerOneName, String playerTwoName, int tick) {

        if (playerOneName == null || playerOneName.isEmpty()) {
            throw new IllegalArgumentException("Не верное имя первого игрока!");
        }

        if (playerTwoName == null || playerTwoName.isEmpty()) {
            throw new IllegalArgumentException("Не верное имя второго игрока!");
        }

        if (tick < 0) {
            throw new IllegalArgumentException("Тик должен быть строго больше нуля");
        }

        this.playerOneName = playerOneName;
        this.playerTwoName = playerTwoName;
        this.tick = tick;
    }

    public void start() throws InterruptedException {
        int gameNumber = 1;

        while (true) {
            System.out.printf("Игра номер %d \n", gameNumber);

            String playerOneMove = readPlayerMove(playerOneName);
            printPlayerMove(playerOneMove, playerOneName);

            String playerTwoMove = readPlayerMove(playerTwoName);
            printPlayerMove(playerTwoMove, playerTwoName);

            if (playerOneMove != null && playerTwoMove != null) {
                printResult(playerOneMove, playerTwoMove);
                clearFiles();
            }

            Thread.sleep(tick);
            gameNumber++;
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

    public void clearFiles() {
        clearFile(playerOneName);
        clearFile(playerTwoName);
    }

    private String readPlayerMove(String playerName) throws InterruptedException {
        int attempt = 0;
        int maxAttempt = 10;

        Path path = Paths.get(playerName);

        while (attempt < maxAttempt) {
            try {
                if (Files.size(path) == 0) {
                    System.out.println("Ождидаем хода игрока " + playerName);
                } else {
                    return new String(Files.readAllBytes(path));
                }
            } catch (IOException e) {
                if (e instanceof NoSuchFileException) {
                    System.out.println("Нет файла для игрока " + playerName);
                }
            }
            attempt++;
            Thread.sleep(tick);
        }
        return null;
    }

    private void clearFile(String playerName) {
        try {
            Files.write(Paths.get(playerName), "".getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            if (ex instanceof NoSuchFileException) {
                System.out.println("В момент записи, не файл игрока " + playerName);
            }
        }
    }

    private void printResult(String playerOneMove, String playerTwoMove) {
        if (playerOneMove.equals(playerTwoMove)) {
            System.out.println("Ничья");
        } else {
            System.out.printf("Выиграл %s \n", computeWinner(playerOneName, playerTwoName,
                    playerOneMove, playerTwoMove));
        }
    }

    private void printPlayerMove(String playerOneMove, String playerName) {
        if (playerOneMove != null) {
            System.out.printf("Ход игрока %s = %s \n", playerName, playerOneMove);
        } else {
            System.out.println("Нет хода игрока " + playerName);
        }
    }
}
