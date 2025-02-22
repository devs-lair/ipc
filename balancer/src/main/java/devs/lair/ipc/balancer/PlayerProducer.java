package devs.lair.ipc.balancer;

import devs.lair.ipc.balancer.service.ConfigProvider;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static devs.lair.ipc.balancer.utils.Constants.INITIAL_PLAYER_COUNT_KEY;

public class PlayerProducer {
    private static final String CLASS_PATH = "/home/devslair/prjs/ipc/balancer/target/classes";
    private static final String MAIN_CLASS = "devs.lair.ipc.balancer.Player";
    private static final String[] COMMANDS = {"java", "-cp", CLASS_PATH, MAIN_CLASS};

    private final Set<Process> players = new HashSet<>();
    private final ConfigProvider configProvider;

    private boolean isStop = false;

    public PlayerProducer() {
        configProvider = new ConfigProvider();
    }

    public void startProduce() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

        configProvider.init();
        int initialPlayerCount = configProvider.getInt(INITIAL_PLAYER_COUNT_KEY,
                2, value -> value >=0);

        int currentCount = 1;
        try {
            while (!isStop) {
                if (currentCount < initialPlayerCount
                        || currentCount <= configProvider.getMaxPlayerCount()) {
                    players.add(new ProcessBuilder(COMMANDS).start());
                    System.out.println("Стартовал процесс игрока с номером " + currentCount);
                    currentCount++;
                }
                Thread.sleep(currentCount > initialPlayerCount
                        ? configProvider.getSpawnPeriod() : 10);
            }
        } catch (IOException e) {
            System.out.println("При создании процесса произошла ошибка"); // от start()
        } catch (InterruptedException e) {
            System.out.println("Процесс был прерван");
        }
    }

    public void stop() {
        isStop = true;
        configProvider.close();
        players.forEach(Process::destroy);
    }

    public static void main(String[] args) {
        new PlayerProducer().startProduce();
    }
}
