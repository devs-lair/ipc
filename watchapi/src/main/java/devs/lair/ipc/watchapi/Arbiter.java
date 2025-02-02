package devs.lair.ipc.watchapi;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

public class Arbiter {

    private final int tick;
    private final String FILE_SUFFIX = ".move";

    private final Set<String> legalMoves =
            Set.of("ROCK", "SCISSORS", "PAPER");
    private final BlockingQueue<String> players =
            new ArrayBlockingQueue<>(1024);

    private String playerOneName;
    private String playerTwoName;
    private DirWatcher dirWatcher;

    public Arbiter(int tick) {
        this.tick = checkTick(tick);
    }

    public void start() throws InterruptedException {
        try (Stream<Path> playersFiles = Files.walk(Paths.get("").toAbsolutePath(), 1)) {
            players.addAll(playersFiles
                    .filter(p -> Files.isRegularFile(p) && p.toString().contains(FILE_SUFFIX))
                    .map(p -> getNameFromPath(p.getFileName()))
                    .toList());

        } catch (IOException e) {
            throw new IllegalStateException("Ошибка, при получении списка файлов", e);
        }

        dirWatcher = new DirWatcher("");
        dirWatcher.addListener(new DirWatcher.DirWatcherListener() {
            @Override
            public void onCreate(WatchEvent<Path> event) {
                Path eventPath = event.context();
                if (Files.isRegularFile(eventPath)
                        && eventPath.getFileName().toString().contains(FILE_SUFFIX)) {
                    String playerName = getNameFromPath(eventPath);

                    // Случай, когда игрок быстро вернулся к жизни
                    if (!playerName.equals(playerOneName)
                            && !playerName.equals(playerTwoName)) {
                        players.add(getNameFromPath(eventPath));
                    }
                }
            }

            @Override
            public void onDelete(WatchEvent<Path> event, boolean isDirectory) {
                String removedPlayerName = getNameFromPath(event.context());
                players.remove(removedPlayerName);
            }
        });
        dirWatcher.startWatch();

        int totalGamesCount = 1;
        int roundNumber = 1;
        boolean isNewGame = false;

        while (true) {

            if (playerOneName == null || !Files.exists(getPathFromName(playerOneName))) {
                playerOneName = getPlayerName();
                isNewGame = true;
            }

            if (playerTwoName == null || !Files.exists(getPathFromName(playerTwoName))) {
                playerTwoName = getPlayerName();
                isNewGame = true;
            }

            if (playerOneName == null || playerTwoName == null) {
                int missingCount = (playerOneName == null && playerTwoName == null) ? 2 : 1;
                System.out.printf("Не хватает %d игроков(а) \n", missingCount);
                Thread.sleep(2L * tick);
                continue;
            }

            if (isNewGame) {
                System.out.println("Началась новая игра");
                System.out.printf("Игроки %s и %s \n", playerOneName, playerTwoName);
                roundNumber = 1;
                isNewGame = false;
            }

            System.out.printf("Игра номер %d \n", roundNumber);

            String playerOneMove = readPlayerMove(playerOneName);
            printPlayerMove(playerOneMove, playerOneName);

            String playerTwoMove = readPlayerMove(playerTwoName);
            printPlayerMove(playerTwoMove, playerTwoName);

            if (playerOneMove != null && playerTwoMove != null) {
                printResult(playerOneMove, playerTwoMove);
                clearFiles();
                roundNumber++;
                totalGamesCount++;
            }

            Thread.sleep(tick);
        }
    }

    public void stop() {
        clearFiles();
        closeWatcher();
    }

    private String getPlayerName() {
        String playerName = players.poll();
        return playerName != null && Files.exists(getPathFromName(playerName))
                ? playerName : null;
    }

    private String readPlayerMove(String playerName) throws InterruptedException {
        int attempt = 0;
        int maxAttempt = 10;

        Path playerFile = Paths.get(playerName + FILE_SUFFIX);
        while (attempt < maxAttempt) {
            try {
                if (Files.size(playerFile) == 0) {
                    System.out.println("Ожидаем хода игрока " + playerName);
                } else {
                    String playerMove = new String(Files.readAllBytes(playerFile));
                    return checkPlayerMove(playerMove);
                }
            } catch (IOException e) {
                if (e instanceof NoSuchFileException) {
                    System.out.println("Нет файла для игрока " + playerName);
                }
            }
            attempt++;
            Thread.sleep(tick);
        }

        //Убиваем зомби игроков, которые есть, но не ходят
        try {
            if (Files.exists(playerFile)) {
                Files.delete(playerFile);
            }
        } catch (IOException e) {
            System.out.println("Не удалось удалить файл зомби игрока");
        }

        return null;
    }

    private void printResult(String playerOneMove, String playerTwoMove) {
        System.out.printf(playerOneMove.equals(playerTwoMove) ? "Ничья \n" :
                "Выиграл %s \n", computeWinner(playerOneMove, playerTwoMove));
    }

    private String computeWinner(String playerOneMove, String playerTwoMove) {
        if ((playerOneMove.equals("ROCK") && playerTwoMove.equals("SCISSORS"))
                || (playerOneMove.equals("PAPER") && playerTwoMove.equals("ROCK")
                || (playerOneMove.equals("SCISSORS") && playerTwoMove.equals("PAPER")))) {
            return playerOneName;
        }
        return playerTwoName;
    }

    private void printPlayerMove(String playerOneMove, String playerName) {
        System.out.println(playerOneMove == null
                ? "Нет хода игрока " + playerName
                : "Ход игрока " + playerName + " = " + playerOneMove);
    }

    private int checkTick(int tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("Тик должен быть строго больше нуля");
        }
        return tick;
    }
    private String checkPlayerMove(String playerMove) {
        return legalMoves.contains(playerMove) ? playerMove : null;
    }

    private String getNameFromPath(Path path) {
        return path.toString().replace(FILE_SUFFIX, "");
    }

    private Path getPathFromName(String playerName) {
        return Paths.get(playerName + FILE_SUFFIX);
    }

    private void closeWatcher() {
        if (dirWatcher == null) return;
        dirWatcher.close();
    }

    private void clearFiles() {
        clearFile(playerOneName);
        clearFile(playerTwoName);
    }

    private void clearFile(String playerName) {
        if (playerName == null) return;
        try {
            Files.write(Paths.get(playerName + FILE_SUFFIX), "".getBytes());
        } catch (IOException ex) {
            if (ex instanceof NoSuchFileException) {
                System.out.println("В момент очистки, нет файл игрока " + playerName);
            }
        }
    }
}
