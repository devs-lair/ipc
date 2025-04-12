package devs.lair.ipc.jmx;

import devs.lair.ipc.jmx.service.Balancer;
import devs.lair.ipc.jmx.service.PlayerProvider;
import devs.lair.ipc.jmx.service.ProcessStarter;
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
    private final PlayerProvider playerProvider = new PlayerProvider();
    private final Balancer balancer = new Balancer();
    private final List<ActorProcess> actors = new ArrayList<>();

    public void init() {
        try {
            //! Order !
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
            commonChecks();

            playerProvider.init();
            actors.add(ProcessStarter.startProcess(CONFIG_LOADER));
            waitConfigLoaderStarted(); //not necessary

            actors.add(ProcessStarter.startProcess(PLAYER_PRODUCER));
            balancer.init(playerProvider);

            while (true) {
                Thread.sleep(1000);
                printStatus();
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
        balancer.close();
        actors.forEach(ActorProcess::terminate);
    }

    private void printStatus() {
        long arbiterCount = balancer.getArbitersCount();
        int querySize = playerProvider.getQuerySize();
        int finished = playerProvider.getFinishedPlayersCount();
        int added = playerProvider.getTotalPlayersCount();
        int provided = playerProvider.getProvidedPlayersCount();
        int zombieCount = playerProvider.getZombieCount();
        int returned = playerProvider.getReturnedCount();

        System.out.printf("Всего Арбитров %d, очередь %d, игроков обнаружено %d, выдано %d (r = %d), отыграли %d (z = %d) \n",
                    arbiterCount, querySize, added, provided, returned, finished, zombieCount);

    }

    public static void main(String[] args) {
        new GameController().init();
    }
}
