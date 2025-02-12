package devs.lair.ipc.rmi;

import devs.lair.ipc.rmi.utils.IPlayerProvider;
import devs.lair.ipc.rmi.utils.Move;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static devs.lair.ipc.rmi.utils.CommonUtils.getPathFromName;
import static devs.lair.ipc.rmi.utils.CommonUtils.tryDelete;

public class Arbiter {
    private final int tick;
    private String playerOneName;
    private String playerTwoName;
    private IPlayerProvider playerProvider;

    public Arbiter(int tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("Тик должен быть строго больше нуля");
        }
        this.tick = tick;
    }

    public void start() throws InterruptedException {
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

    private String getPlayerName() {
        try {
            if (playerProvider == null) {
                Registry registry = LocateRegistry.getRegistry();
                playerProvider = (IPlayerProvider) registry.lookup(IPlayerProvider.class.getName());
            }
            return playerProvider.getPlayerName("arbiter");
        } catch (RemoteException e) {
            playerProvider = null;
            System.out.println("Ошибка при получения хода игрока из PlayerProvider");
        } catch (NullPointerException | NotBoundException e) {
            System.out.println("Ошибка при получения сервиса PlayerProvider");
        }
        return null;
    }

    private Move readPlayerMove(String playerName) throws InterruptedException {
        int attempt = 0;
        int maxAttempt = 10;

        Path playerFile = getPathFromName(playerName);
        while (attempt++ <= maxAttempt) {
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
                return null;
            } catch (IllegalArgumentException e) {
                System.out.println("Не корректный ход игрока "
                        + playerName + ". Ход = " + playerMove);
                return null;
            }

            attempt++;
            Thread.sleep(tick);
        }

        //Убиваем зомби игроков, которые есть, но не ходят
        deletePlayersFiles(playerName);
        return null;
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

    private void deletePlayersFiles(String... playerNames) {
        for (String playerName : playerNames) {
            if (playerName == null) continue;
            if (!tryDelete(getPathFromName(playerName))) {
                System.out.println("Не удалось удалить файл игрока" + playerName);
            }
        }
        playerOneName = playerTwoName = null;
    }

    private void clearPlayerFiles(String... playerNames) {
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

    public static void main(String[] args) throws InterruptedException {
        int tick = 1000;
        new Arbiter(tick).start();
    }
}
