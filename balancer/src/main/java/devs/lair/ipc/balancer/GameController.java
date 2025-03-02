package devs.lair.ipc.balancer;

import devs.lair.ipc.balancer.service.Balancer;
import devs.lair.ipc.balancer.service.PlayerProvider;
import devs.lair.ipc.balancer.service.ProcessStarter;
import devs.lair.ipc.balancer.service.model.ActorProcess;

import java.util.ArrayList;
import java.util.List;

import static devs.lair.ipc.balancer.service.enums.ProcessType.*;

public class GameController {
    private final PlayerProvider playerProvider = new PlayerProvider();
    private final ProcessStarter starter = new ProcessStarter();
    private final Balancer balancer = new Balancer();
    private final List<ActorProcess> actors = new ArrayList<>();

    public void init() {
        try {
            //Order
            playerProvider.init();

            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

            actors.add(starter.startProcess(CONFIG_LOADER));
            actors.add(starter.startProcess(PLAYER_PRODUCER));
            balancer.init(actors, starter, playerProvider);

            while (true) {
                Thread.sleep(1000);
                printStatus();
            }

        } catch (Exception ex) {
            System.out.println("При инициализации произошла ошибка: " + ex.getMessage());
        }
    }

    public void stop() {
        actors.forEach(ActorProcess::terminate);
        balancer.close();
    }

    private void printStatus() {
        long arbiterCount = actors.stream().filter(p -> p.getType() == ARBITER).count();
        int querySize = playerProvider.getQuerySize();
        int provided = playerProvider.getProviderPlayersCount();

        System.out.printf("Всего Арбитров %d, размер очереди %d, игроков обработано %d \n",
                arbiterCount, querySize, provided);
    }

    public static void main(String[] args) {
        try {
            new GameController().init();
        } catch (Exception e) {
            System.out.println("Произошла ошибка: " + e.getMessage());
        }
    }
}
