package devs.lair.ipc.balancer.service;

import devs.lair.ipc.balancer.service.interfaces.IPlayerProvider;
import devs.lair.ipc.balancer.utils.Utils;

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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static devs.lair.ipc.balancer.utils.Constants.PLAYER_DIR;
import static devs.lair.ipc.balancer.utils.Constants.PLAYER_FILE_SUFFIX;
import static devs.lair.ipc.balancer.utils.Utils.getNameFromPath;
import static devs.lair.ipc.balancer.utils.Utils.getPathFromName;

public class PlayerProvider implements IPlayerProvider {

    private final BlockingQueue<String> players = new ArrayBlockingQueue<>(1024);
    private final AtomicInteger provided = new AtomicInteger(0);
    private DirWatcher dirWatcher;

    @Override
    public String getPlayerName(String arbiterName) throws RemoteException {
        String playerName = players.poll();

        if (playerName != null && Files.exists(getPathFromName(playerName))) {
            provided.incrementAndGet();
            return playerName;
        }

        return null;
    }

    public void init() {
        startWatch();
        register();
    }

    private void startWatch() {
        Path playerDir = Paths.get(PLAYER_DIR);

        try {
            Utils.createDirectoryIfNotExist(playerDir);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Ошибка при создании директории " + playerDir, ex);
        }

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
                if (eventPath.getFileName().toString().contains(PLAYER_FILE_SUFFIX)) {
                    String playerName = getNameFromPath(eventPath);
                    players.add(playerName);
                }
            }

            @Override
            public void onDelete(WatchEvent<Path> event, boolean isDirectory) {
                String removedPlayerName = getNameFromPath(event.context());
                players.remove(removedPlayerName);
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

    public int getProviderPlayersCount() {
        return provided.get();
    }
}