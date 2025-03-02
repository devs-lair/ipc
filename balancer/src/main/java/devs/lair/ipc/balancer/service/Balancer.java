package devs.lair.ipc.balancer.service;

import devs.lair.ipc.balancer.service.model.ActorProcess;

import java.util.List;

import static devs.lair.ipc.balancer.service.enums.ProcessType.ARBITER;

public class Balancer implements AutoCloseable {
    private Thread worker;
    private boolean isStop = false;

    public void init(List<ActorProcess> actors, ProcessStarter starter, PlayerProvider playerProvider) {
        worker = new Thread(() -> {
            try {
                while (!isStop) {
                    long arbitersCount = actors.stream()
                            .filter(ActorProcess::isArbiter).count();

                    if (arbitersCount == 0 || playerProvider.getQuerySize() > 10) {
                        actors.add(starter.startProcess(ARBITER));
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
        worker.interrupt();
    }
}
