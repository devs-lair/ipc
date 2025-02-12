package devs.lair.ipc.rmi;

import devs.lair.ipc.rmi.utils.CommonUtils;
import devs.lair.ipc.rmi.utils.DirWatcher;
import devs.lair.ipc.rmi.utils.IPlayerProvider;

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
import java.util.stream.Stream;

import static devs.lair.ipc.rmi.utils.CommonUtils.*;

public class GameController implements IPlayerProvider {

    private final BlockingQueue<String> players = new ArrayBlockingQueue<>(1024);
    private DirWatcher dirWatcher;

    @Override
    public String getPlayerName(String arbiterName) throws RemoteException {
        String playerName = players.poll();
        return playerName != null && Files.exists(getPathFromName(playerName))
                ? playerName : null;
    }

    private void startWatch() throws IOException {
        CommonUtils.createDirectoryIfNotExist(Paths.get(FILE_DIR));

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

    private void start() throws InterruptedException {
        while (true) {
            Thread.sleep(10000);
        }
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

    public static void main(String[] args) {
        try {
            GameController gameController = new GameController();
            gameController.startWatch();
            gameController.register();
            gameController.start();
        } catch (Exception e) {
            System.out.println("Произошла ошибка: " + e.getMessage());
        }
    }
}
