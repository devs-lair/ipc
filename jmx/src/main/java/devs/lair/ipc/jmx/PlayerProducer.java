package devs.lair.ipc.jmx;

import devs.lair.ipc.jmx.service.ProcessStarter;
import devs.lair.ipc.jmx.service.interfaces.ConfigurableProcess;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static devs.lair.ipc.jmx.service.enums.ProcessType.PLAYER;
import static java.lang.Thread.currentThread;

public class PlayerProducer extends ConfigurableProcess {
    private final List<ProcessHandle> players = new ArrayList<>();
    private int currentCount = 0;

    public void startProduce() {
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));

        try (configProvider) {
            while (!currentThread().isInterrupted()) {
                if (currentCount < configProvider.getMaxPlayerCount()) {
                    players.add(ProcessStarter.startProcess(PLAYER).getProcess());
                    currentCount++;
                }

                players.removeIf(p -> !p.isAlive());
                Thread.sleep(configProvider.getProducerTick());
            }
        } catch (InterruptedException e) {
            System.out.println("Процесс был прерван");
        }

        try {
            //Need time to normally start ProcessStarter::startProcess
            //Calculate pause (producerTick * players.size() maybe)
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            System.out.println("Процесс был прерван повторно");
        }
        players.forEach(ProcessHandle::destroy);
    }

    public static void main(String[] args) {
        new PlayerProducer().startProduce();
    }
}
