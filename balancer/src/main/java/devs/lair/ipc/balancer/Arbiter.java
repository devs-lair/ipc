package devs.lair.ipc.balancer;


import devs.lair.ipc.balancer.utils.IPlayerProvider;
import devs.lair.ipc.balancer.utils.Move;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static devs.lair.ipc.balancer.utils.Utils.getPathFromName;
import static devs.lair.ipc.balancer.utils.Utils.tryDelete;


public class Arbiter {
    private final int tick;

    private final String[] players = new String[2];
    private final Move[] moves = new Move[2];
    private int roundNumber = 1;
    private final int maxRound = 5;

    private IPlayerProvider playerProvider;
    private boolean isStop = false;

    public Arbiter(int tick) {
        if (tick < 0) {
            throw new IllegalArgumentException("Тик должен быть строго больше нуля");
        }
        this.tick = tick;
    }

    public void start() {
        Runtime.getRuntime()
                .addShutdownHook(new Thread(this::stop));
        try {
            while (!isStop) {
                fetchPlayers();

                if (playersReady()) {
                    printGreeting();
                    fetchPlayersMove();
                    fetchResult();
                }
                Thread.sleep(tick);
            }
        } catch (InterruptedException e) {
            System.out.println("Основной поток был прерван");
        }
    }

    public void stop() {
        clearPlayersFiles();
        isStop = true;
    }

    private void fetchPlayers() {
        int counter = 0;
        for (String playerName : players) {
            if (playerName == null || !Files.exists(getPathFromName(playerName))) {
                players[counter++] = getPlayerName();
                roundNumber = 1;
            }
        }
    }

    private void fetchPlayersMove() {
        int counter = 0;
        for (String playerName : players) {
            moves[counter] = readPlayerMove(playerName);
            printPlayerMove(moves[counter], playerName);
            counter++;
        }
    }

    private void fetchResult() {
        if (moves[0] != null && moves[1] != null) {
            printResult();
            clearPlayersFiles();

            if (roundNumber++ == maxRound) {
                System.out.printf("Игроки %s и %s завершили игру \n\n", players[0], players[1]);
                deletePlayersFiles();
            }
        }
    }

    private boolean playersReady() {
        if (players[0] == null || players[1] == null) {
            System.out.printf("Не хватает %d игроков(а) \n",
                    players[0] == null && players[1] == null ? 2 : 1);
            return false;
        }
        return true;
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

    private Move readPlayerMove(String playerName) {
        int attempt = 0;
        int maxAttempt = 10;
        Path playerFile = getPathFromName(playerName);

        try {
            while (attempt++ <= maxAttempt) {
                if (Files.size(playerFile) == 0) {
                    System.out.println("Ожидаем хода игрока " + playerName);
                    Thread.sleep(tick);
                    continue;
                }
                return Move.valueOf(Files.readAllBytes(playerFile));
            }
        } catch (Exception e) {
            switch (e) {
                case NoSuchFileException nsfe -> System.out.printf("Нет файла для игрока %s\n", playerName);
                case IllegalArgumentException iae ->
                        System.out.printf("Некорректный ход игрока %s. %s\n", playerName, iae.getMessage());
                default -> System.out.println("Ошибка при получении хода игрока: " + e.getMessage());
            }
        }

        //Убиваем зомби игрока, который есть, но не ходят
        if (!tryDelete(getPathFromName(playerName))) {
            System.out.println("Не удалось удалить файл зомби игрока" + playerName);
        }
        return null;
    }

    private void printGreeting() {
        if (roundNumber == 1) {
            System.out.printf("Началась новая игра \nИгроки %s и %s \n", players[0], players[1]);
        }
        System.out.printf("\nИгра номер %d \n", roundNumber);
    }

    private void printResult() {
        int compare = moves[0].compareWith(moves[1]);
        System.out.printf(compare == 0
                ? "Ничья \n" :
                "Выиграл %s \n", compare == 1 ? players[0] : players[1]);
    }

    private void printPlayerMove(Move playerMove, String playerName) {
        System.out.println(playerMove == null
                ? "Нет хода игрока " + playerName
                : "Ход игрока " + playerName + " = " + playerMove);
    }

    private void deletePlayersFiles() {
        for (String playerName : players) {
            if (playerName == null) continue;
            if (!tryDelete(getPathFromName(playerName))) {
                System.out.println("Не удалось удалить файл игрока" + playerName);
            }
        }
        players[0] = players[1] = null;
    }

    private void clearPlayersFiles() {
        for (String playerName : players) {
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

    public static void main(String[] args) {
        int tick = 1000;
        new Arbiter(tick).start();
    }
}
