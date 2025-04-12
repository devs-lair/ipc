package devs.lair.ipc.jmx.service;

import devs.lair.ipc.jmx.service.interfaces.IPlayerProvider;
import devs.lair.ipc.jmx.service.model.ArbiterProgress;
import devs.lair.ipc.jmx.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static devs.lair.ipc.jmx.utils.Constants.PLAYER_DIR;
import static devs.lair.ipc.jmx.utils.Constants.PLAYER_FILE_SUFFIX;
import static devs.lair.ipc.jmx.utils.Utils.*;

public class PlayerProvider implements IPlayerProvider {

    private DirWatcher dirWatcher;
    private final Queue<String> players = new ConcurrentLinkedQueue<>();
    private final Map<String, ArbiterProgress> playersByArbiter = new ConcurrentHashMap<>();

    private final AtomicInteger provided = new AtomicInteger(0);
    private final AtomicInteger returned = new AtomicInteger(0);
    private final AtomicInteger finished = new AtomicInteger(0);
    private final AtomicInteger added = new AtomicInteger(0);
    private final AtomicInteger zombie = new AtomicInteger(0);

    @Override
    public String getPlayerName(String arbiterName, int position) throws RemoteException {
        String playerName = players.poll();

        if (playerName != null && Files.exists(getPathFromName(playerName))) {
            addPlayerToArbiter(arbiterName, position, playerName);
            provided.incrementAndGet();
            return playerName;
        }
        return null;
    }

    @Override
    public void finishPlayer(String arbiterName, String[] players) throws RemoteException {
        for (String playerName : players) {
            if (playerName == null) continue;
            removePlayerFromArbiter(arbiterName, playerName);
            tryDelete(getPathFromName(playerName));
            finished.incrementAndGet();
        }
    }

    @Override
    public void returnPlayer(String arbiterName, String playerName) throws RemoteException {
        if (playerName != null && Files.exists(getPathFromName(playerName))) {
            players.add(playerName);
            returned.incrementAndGet();
            removePlayerFromArbiter(arbiterName, playerName);
        }
    }

    @Override
    public void killZombie(String arbiterName, String playerName) throws RemoteException {
        ProcessHandle zombieProcess = Utils.findZombieProcess(playerName);

        if (zombieProcess == null) {
            System.out.println("Не найден процесс зомби игрока");
            return;
        }

        zombieProcess.destroyForcibly();
        tryDelete(getPathFromName(playerName));
        removePlayerFromArbiter(arbiterName, playerName);

        zombie.incrementAndGet();
        System.out.println("Зомби игрок был убит: " + playerName);
    }

    private void addPlayerToArbiter(String arbiterName, int position, String playerName) {
        ArbiterProgress progress = playersByArbiter
                .computeIfAbsent(arbiterName, p -> new ArbiterProgress(arbiterName));
        progress.setPlayer(playerName, position);
    }

    private void removePlayerFromArbiter(String arbiterName, String playerName) {
        ArbiterProgress progress = playersByArbiter.get(arbiterName);
        if (progress == null) {
            System.out.println("Не найден прогресс арбитра с именем " + arbiterName);
            return;
        }

        progress.removePlayer(playerName);
    }

    public void init() {
        startWatch();
        register();
    }

    private void startWatch() {
        Path playerDir = Paths.get(PLAYER_DIR);
        try (Stream<Path> playersFiles =
                     Files.walk(playerDir.toAbsolutePath(), 1)) {
            players.addAll(playersFiles
                    .filter(p -> Files.isRegularFile(p) && p.toString().contains(PLAYER_FILE_SUFFIX))
                    .map(p -> getNameFromPath(p.getFileName()))
                    .toList());

        } catch (IOException e) {
            throw new IllegalStateException("Ошибка, при получении списка файлов", e);
        }

        dirWatcher = new DirWatcher(PLAYER_DIR);
        dirWatcher.addListener(new DirWatcher.DirWatcherListener() {
            @Override
            public void onCreate(WatchEvent<Path> event) {
                Path eventPath = event.context();
                String playerName = getNameFromPath(eventPath);
                players.add(playerName);
                added.incrementAndGet();
            }
        });
        dirWatcher.startWatch();

        Runtime.getRuntime()
                .addShutdownHook(new Thread(this::close));
    }

    public void close() {
        if (dirWatcher == null) return;
        dirWatcher.close();
    }

    private void register() {
        try {
            Remote stub = UnicastRemoteObject.exportObject(this, 0);
            Registry registry = LocateRegistry.createRegistry(1099);

            registry.bind(IPlayerProvider.class.getName(), stub);
        } catch (RemoteException | AlreadyBoundException e) {
            throw new IllegalArgumentException("Не удалось зарегистрироваться в Registry");
        }
    }

    public int getQuerySize() {
        return players.size();
    }

    public int getTotalPlayersCount() {
        return added.get();
    }

    public int getProvidedPlayersCount() {
        return provided.get();
    }

    public int getFinishedPlayersCount() {
        return finished.get();
    }

    public int getReturnedCount() {
        return returned.get();
    }

    public int getZombieCount() {
        return zombie.get();
    }
}