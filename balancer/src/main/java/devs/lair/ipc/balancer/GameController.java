package devs.lair.ipc.balancer;

import devs.lair.ipc.balancer.service.Balancer;
import devs.lair.ipc.balancer.service.PlayerProvider;
import devs.lair.ipc.balancer.service.ProcessStarter;
import devs.lair.ipc.balancer.service.model.ActorProcess;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static devs.lair.ipc.balancer.service.enums.ProcessType.CONFIG_LOADER;
import static devs.lair.ipc.balancer.service.enums.ProcessType.PLAYER_PRODUCER;
import static devs.lair.ipc.balancer.utils.Constants.MEMORY_CONFIG_PATH;

public class GameController {
    private final PlayerProvider playerProvider = new PlayerProvider();
    private final Balancer balancer = new Balancer();
    private final List<ActorProcess> actors = new ArrayList<>();

    public void init() {
        try {
            //! Order !

            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
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
        int provided = playerProvider.getProviderPlayersCount();

        System.out.printf("Всего Арбитров %d, размер очереди %d, игроков обработано %d \n",
                arbiterCount, querySize, provided);
    }

    public static void main(String[] args) {
        new GameController().init();
    }
}
