package devs.lair.ipc.jmx;

import devs.lair.ipc.jmx.service.interfaces.ConfigurableProcess;
import devs.lair.ipc.jmx.service.interfaces.IPlayerProvider;
import devs.lair.ipc.jmx.utils.Move;
import devs.lair.ipc.jmx.utils.Utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static devs.lair.ipc.jmx.service.enums.ProcessType.ARBITER;
import static devs.lair.ipc.jmx.utils.Constants.ARBITER_DIR;
import static devs.lair.ipc.jmx.utils.Constants.MAX_ATTEMPT_KEY;
import static devs.lair.ipc.jmx.utils.Utils.*;
import static java.lang.Thread.currentThread;

public class Arbiter extends ConfigurableProcess {
    private final String[] players = new String[2];
    private final Move[] moves = new Move[2];
    private IPlayerProvider playerProvider;

    private int roundNumber = 1;

    public Arbiter(String name) {
        this.name = name == null
                ? generateUniqueName(ARBITER.name().toLowerCase())
                : name;
    }

    public void start() {
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));

        Path arbiterFile = Paths.get(ARBITER_DIR + "/" + name);
        try {
            //Move to system check
            createDirectoryIfNotExist(arbiterFile.getParent());
            Files.createFile(arbiterFile);

            while (!currentThread().isInterrupted()) {
                fetchPlayers();

                if (playersReady()) {
                    printGreeting();
                    fetchPlayersMove();
                    fetchResult();
                }

                Thread.sleep(configProvider.getArbiterTick());
            }
        } catch (FileAlreadyExistsException e) {
            System.out.println("Арбитр с именем " + name + " уже запущен " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("Основной поток был прерван");
        } catch (IOException e) {
            System.out.println("Ошибка при создании файла арбитра");
        } finally {
            tryDelete(arbiterFile);
        }

        returnPlayers();
    }

    private void fetchPlayers() {
        for (int i = 0; i < 2; i++) {
            String playerName = players[i];
            if (playerName == null || !Files.exists(getPathFromName(playerName))) {
                if (interrupted) throw new IllegalStateException();

                players[i] = fetchPlayerName();
                roundNumber = 1;
            }
        }
    }

    private String fetchPlayerName() {
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

    private void returnPlayers() {
        try {
            for (int i = 0; i < 2; i++) {
                String player = players[i];
                if (player != null) {
                    playerProvider.returnPlayer(player);
                    players[i] = null;
                }
            }
        } catch (RemoteException e) {
            System.out.println("Не удалось вернуть игрока");
        } catch (NullPointerException e) {
            System.out.println("Нет playerProvider");
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

            if (roundNumber++ >= configProvider.getMaxRound()) {
                System.out.printf("Игроки %s и %s завершили игру \n\n", players[0], players[1]);
                deletePlayersFiles();
                if (interrupted) {
                    throw new IllegalStateException();
                }
            }
        }
    }

    //public, fot test only
    public boolean playersReady() {
        if (players[0] == null || players[1] == null) {
            System.out.printf("Не хватает %d игроков(а) \n",
                    players[0] == null && players[1] == null ? 2 : 1);
            return false;
        }
        return true;
    }

    private Move readPlayerMove(String playerName) {
        int attempt = 0;
        Path playerFile = getPathFromName(playerName);

        try {
            do {
                if (Files.size(playerFile) == 0) {
                    System.out.println("Ожидаем хода игрока " + playerName);
                    Thread.sleep(configProvider.getArbiterTick());
                    continue;
                }
                return Move.valueOf(Files.readAllBytes(playerFile));
            } while (++attempt <= configProvider.readPositiveInt(MAX_ATTEMPT_KEY, 5));
        } catch (Exception e) {
            switch (e) {
                case NoSuchFileException nsfe -> System.out.printf("Нет файла для игрока %s\n", playerName);
                case IllegalArgumentException iae ->
                        System.out.printf("Некорректный ход игрока %s. %s\n", playerName, iae.getMessage());
                default -> System.out.println("Ошибка при получении хода игрока: " + e.getMessage());
            }
        }

        //Убиваем зомби игрока, который есть, но не ходят
//        if (!tryDelete(getPathFromName(playerName))) {
//            System.out.println("Не удалось удалить файл зомби игрока" + playerName);
//        }
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
        new Arbiter(Utils.isNullOrEmpty(args) ? null : args[0]).start();
    }
}
