package devs.lair.ipc.rmi;

import devs.lair.ipc.rmi.utils.DirWatcher;
import devs.lair.ipc.rmi.utils.Move;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Stream;

import static devs.lair.ipc.rmi.utils.CommonUtils.*;

public class Arbiter {
    private final int tick;
    private final BlockingQueue<String> players = new ArrayBlockingQueue<>(1024);

    private String playerOneName;
    private String playerTwoName;
    private DirWatcher dirWatcher;

    public Arbiter(int tick) {
        this.tick = checkTick(tick);
    }

    public void start() throws InterruptedException {
        try (Stream<Path> playersFiles =
                     Files.walk(Paths.get(FILE_DIR).toAbsolutePath(), 1)) {
            players.addAll(playersFiles
                    .filter(p -> Files.isRegularFile(p) && p.toString().contains(FILE_SUFFIX))
                    .map(p -> getNameFromPath(p.getFileName()))
                    .toList());

        } catch (IOException e) {
            throw new IllegalStateException("Ошибка, при получении списка файлов", e);
        }

        dirWatcher = new DirWatcher(FILE_DIR);
        dirWatcher.addListener(new DirWatcher.DirWatcherListener() {
            @Override
            public void onCreate(WatchEvent<Path> event) {
                Path eventPath = event.context();
                if (eventPath.getFileName().toString().contains(FILE_SUFFIX)) {
                    String playerName = getNameFromPath(eventPath);

                    // Случай, когда игрок быстро вернулся к жизни
                    if (!playerName.equals(playerOneName)
                            && !playerName.equals(playerTwoName)) {
                        players.add(playerName);
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
        int maxRound = 5;
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

            System.out.printf("\nИгра номер %d \n", roundNumber);

            Move playerOneMove = readPlayerMove(playerOneName);
            printPlayerMove(playerOneMove, playerOneName);

            Move playerTwoMove = readPlayerMove(playerTwoName);
            printPlayerMove(playerTwoMove, playerTwoName);

            if (playerOneMove != null && playerTwoMove != null) {
                printResult(playerOneMove, playerTwoMove);
                clearPlayerFiles(playerOneName, playerTwoName);
                totalGamesCount++;

                if (roundNumber == maxRound) {
                    System.out.printf("Игроки %s и %s завершили игру \n\n",
                            playerOneName, playerTwoName);
                    deletePlayersFiles();
                }
                roundNumber++;
            }
            Thread.sleep(tick);
        }
    }

    public void stop() {
        clearPlayerFiles();
        closeWatcher();
    }

    private String getPlayerName() {
        String playerName = players.poll();
        return playerName != null && Files.exists(getPathFromName(playerName))
                ? playerName : null;
    }

    private Move readPlayerMove(String playerName) throws InterruptedException {
        int attempt = 0;
        int maxAttempt = 10;

        Path playerFile = getPathFromName(playerName);
        while (attempt < maxAttempt) {
            String playerMove = "";
            try {
                if (Files.size(playerFile) == 0) {
                    System.out.println("Ожидаем хода игрока " + playerName);
                } else {
                    playerMove = new String(Files.readAllBytes(playerFile));
                    return Move.valueOf(playerMove);
                }
            } catch (IOException e) {
                if (e instanceof NoSuchFileException) {
                    System.out.println("Нет файла для игрока " + playerName);
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Не корректный ход игрока "
                        + playerName + ". Ход = " + playerMove);
                return null;
            }

            attempt++;
            Thread.sleep(tick);
        }

        //Убиваем зомби игроков, которые есть, но не ходят
        if (!tryDelete(playerFile)) {
            System.out.println("Не удалось удалить файл зомби игрока");
        }

        return null;
    }

    private void deletePlayersFiles() {
        deletePlayersFile(playerOneName);
        deletePlayersFile(playerTwoName);

        playerOneName = null;
        playerTwoName = null;
    }

    private void deletePlayersFile(String playerName) {
        if (!tryDelete(getPathFromName(playerName))) {
            System.out.println("Не удалось удалить файл игрока");
        }
    }

    private void printResult(Move playerOneMove, Move playerTwoMove) {
        int compare = playerOneMove.compareWith(playerTwoMove);
        System.out.printf(compare == 0
                ? "Ничья \n" :
                "Выиграл %s \n", compare == 1 ? playerOneName : playerTwoName);
    }

    private void printPlayerMove(Move playerOneMove, String playerName) {
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

    private void closeWatcher() {
        if (dirWatcher == null) return;
        dirWatcher.close();
    }

    private void clearPlayerFiles(String ... playerNames) {
        for (String playerName : playerNames) {
            if (playerName == null) continue;
            try {
                Files.write(getPathFromName(playerName), "".getBytes());
            } catch (IOException ex) {
                if (ex instanceof NoSuchFileException) {
                    System.out.println("В момент очистки, нет файл игрока " + playerName);
                }
            }
        }
    }
}
