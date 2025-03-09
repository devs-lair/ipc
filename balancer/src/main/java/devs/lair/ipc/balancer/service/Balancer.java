package devs.lair.ipc.balancer.service;

import devs.lair.ipc.balancer.service.enums.ProcessStatus;
import devs.lair.ipc.balancer.service.model.ActorProcess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static devs.lair.ipc.balancer.service.enums.ProcessType.ARBITER;

public class Balancer implements AutoCloseable {
    private Thread worker;
    private boolean isStop = false;
    private final List<ActorProcess> arbiters = new ArrayList<>();

    public void init(PlayerProvider playerProvider) {
        worker = new Thread(() -> {
            try {
                while (!isStop) {
                    long arbitersCount = arbiters.size();

                    if (arbitersCount == 0 || playerProvider.getQuerySize() > 10) {
                        arbiters.add(ProcessStarter.startProcess(ARBITER));
                    } else if (arbitersCount > 1 && playerProvider.getQuerySize() <= 5) {
                        ActorProcess termProcess = arbiters.stream()
                                .filter(ActorProcess::isTerminating).findFirst().orElse(null);
                        if (termProcess == null) {
                            arbiters.getFirst().terminate();
                        } else {
                            if (!termProcess.getProcess().isAlive()) {
                                termProcess.setStatus(ProcessStatus.DEAD);
                            }
                        }
                    }

                    Iterator<ActorProcess> iterator = arbiters.iterator();
                    while (iterator.hasNext()) {
                        ActorProcess actor = iterator.next();
                        if (actor.isDead() || !actor.getProcess().isAlive()) {
                            iterator.remove();
                            System.out.printf("Арбитр был остановлен и удален : имя %s, тип %s, pid = %d \n",
                                    actor.getName(), actor.getType(), actor.getProcess().pid());
                        }
                    }

                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                System.out.println("Основной поток был прерван");
            }
        });

        worker.start();
    }

    @Override
    public void close() {
        isStop = true;
        arbiters.forEach(ActorProcess::terminate);
        if (worker != null) {
            worker.interrupt();
        }
    }

    public long getArbitersCount() {
        return arbiters.size();
    }
}
