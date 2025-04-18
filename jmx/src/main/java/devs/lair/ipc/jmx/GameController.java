package devs.lair.ipc.jmx;

import devs.lair.ipc.jmx.service.*;
import devs.lair.ipc.jmx.service.model.ActorProcess;
import devs.lair.ipc.jmx.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static devs.lair.ipc.jmx.service.enums.ProcessType.CONFIG_LOADER;
import static devs.lair.ipc.jmx.service.enums.ProcessType.PLAYER_PRODUCER;
import static devs.lair.ipc.jmx.utils.Constants.*;

public class GameController {
    private final ConfigProvider configProvider;
    private final PlayerProvider playerProvider;
    private final ArbiterProvider arbiterProvider;
    private final Balancer balancer;

    private final List<ActorProcess> actors = new ArrayList<>();

    public GameController() {
        configProvider = new ConfigProvider();
        arbiterProvider = new ArbiterProvider();
        playerProvider = new PlayerProvider(arbiterProvider);
        balancer = new Balancer(playerProvider, arbiterProvider, configProvider);
    }

    public void init() {
        try {
            //! Order !
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
            commonChecks();

            configProvider.init();
            playerProvider.init();
            arbiterProvider.init();
            balancer.init();

            actors.add(ProcessStarter.startProcess(CONFIG_LOADER));
            actors.add(ProcessStarter.startProcess(PLAYER_PRODUCER));

            while (true) {
                balancer.balance();
                printStatus();
                Thread.sleep(1000);
            }
        } catch (Exception ex) {
            System.out.println("При инициализации произошла ошибка: " + ex.getMessage());
        }
    }

    private void commonChecks() throws IOException {
        Utils.createDirectoryIfNotExist(Paths.get(PLAYER_DIR));
        Utils.createDirectoryIfNotExist(Paths.get(ARBITER_DIR));
        Utils.tryDelete(Paths.get(MEMORY_CONFIG_FILE));
    }

    private void waitConfigLoaderStarted() throws InterruptedException {
        while (!Files.exists(MEMORY_CONFIG_PATH)) {
            Thread.sleep(10);
        }
    }

    public void stop() {
        playerProvider.close();
        arbiterProvider.close();
        actors.forEach(ActorProcess::terminate);
    }

    private void printStatus() {
        int arbiterCount = arbiterProvider.getArbitersCount();
        int querySize = playerProvider.getQuerySize();
        int finished = playerProvider.getFinishedPlayersCount();
        int added = playerProvider.getTotalPlayersCount();
        int provided = playerProvider.getProvidedPlayersCount();
        int playerZombieCount = playerProvider.getZombieCount();
        int returned = playerProvider.getReturnedCount();
        int arbiterZombieCount = arbiterProvider.getZombieCount();

        System.out.printf("Всего Арбитров %d (z = %d), очередь %d, игроков обнаружено %d, выдано %d (r = %d), отыграли %d (z = %d) \n",
                    arbiterCount, arbiterZombieCount, querySize, added, provided, returned, finished, playerZombieCount);
    }

    public static void main(String[] args) {
        new GameController().init();
    }
}
