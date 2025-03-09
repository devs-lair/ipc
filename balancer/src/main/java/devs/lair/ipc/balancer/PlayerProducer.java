package devs.lair.ipc.balancer;

import devs.lair.ipc.balancer.service.ProcessStarter;
import devs.lair.ipc.balancer.service.interfaces.ConfigurableProcess;

import java.util.HashSet;
import java.util.Set;

import static devs.lair.ipc.balancer.service.enums.ProcessType.PLAYER;
import static java.lang.Thread.currentThread;

public class PlayerProducer extends ConfigurableProcess {
    private final Set<Process> players = new HashSet<>();

    public void startProduce() {
        super.start();
        int currentCount = 1;

        try {
            while (!currentThread().isInterrupted()) {
                if (currentCount <= configProvider.getMaxPlayerCount()) {
                    players.add(ProcessStarter.startProcess(PLAYER).getProcess());
                    currentCount++;
                }

                players.removeIf(p -> !p.isAlive());
                Thread.sleep(configProvider.getSpawnPeriod());
            }
        } catch (InterruptedException e) {
            System.out.println("Процесс был прерван");
        }
    }

    public void stop() {
        super.stop();
        players.forEach(Process::destroy);
    }

    public static void main(String[] args) {
        new PlayerProducer().startProduce();
    }
}
