package devs.lair.ipc.jmx.service;

import devs.lair.ipc.jmx.service.enums.ProcessStatus;
import devs.lair.ipc.jmx.service.model.ActorProcess;
import devs.lair.ipc.jmx.service.model.ArbiterProcess;
import devs.lair.ipc.jmx.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static devs.lair.ipc.jmx.service.enums.ProcessType.ARBITER;
import static devs.lair.ipc.jmx.utils.Constants.ARBITER_DIR;

public class ArbiterProvider implements AutoCloseable {
    private final Map<String, ActorProcess> arbiters = new ConcurrentHashMap<>();
    private final AtomicInteger zombie = new AtomicInteger(0);

    public void init() {
        //Поиск уже запущенных арбитров
        Path playerDir = Paths.get(ARBITER_DIR);
        try (Stream<Path> arbiterFiles = Files.walk(playerDir.toAbsolutePath(), 1)) {
            for (Path path : arbiterFiles.filter(Files::isRegularFile).toList()) {
                ActorProcess processByFile = Utils.findProcessByFile(path);
                if (processByFile != null) {
                    arbiters.put(path.getFileName().toString(), processByFile);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Ошибка, при получении списка файлов", e);
        }
    }

    public void startArbiter(int count) {
        for (int i = 0; i < count; i++) {
            ActorProcess arbiter = ProcessStarter.startProcess(ARBITER);
            arbiters.put(arbiter.getName(), arbiter);
        }
    }

    public void finishArbiter(int count) {
        removeDeadProcess();

        List<ActorProcess> terminating = arbiters.values().stream()
                .filter(ActorProcess::isTerminating).toList();

        int finishCount = count - terminating.size();
        if (finishCount <= 0) {
            return;
        }

        int totalCount = arbiters.size();
        if (finishCount >= totalCount) {
            finishCount = totalCount - 1;
        }

        arbiters.values().stream()
                .filter(p -> !terminating.contains(p))
                .limit(finishCount).forEach(ActorProcess::terminate);
    }

    public void removeDeadProcess() {
        for (Map.Entry<String, ActorProcess> entry : arbiters.entrySet()) {
            ActorProcess actor = entry.getValue();
            if (entry.getValue().isDead()) {
                arbiters.remove(entry.getKey());
                System.out.printf("Арбитр был остановлен и удален : имя %s, тип %s, pid = %d \n",
                        actor.getName(), actor.getType(), actor.getProcess().pid());
            }
        }
    }

    public void onArbiterRequest(String arbiterName) {
        ArbiterProcess arbiter = (ArbiterProcess) arbiters.get(arbiterName);
        if (arbiter == null) {
            System.out.println("Не найден процесс арбитра с именем " + arbiterName);
            return;
        }

        arbiter.setLastTimeUsage(System.currentTimeMillis());
    }

    public void addPlayerToArbiter(String arbiterName, int position, String playerName) {
        ArbiterProcess arbiter = (ArbiterProcess) arbiters.get(arbiterName);
        if (arbiter == null) {
            System.out.println("Не найден процесс арбитра с именем " + arbiterName);
            return;
        }

        arbiter.setPlayer(playerName, position);
    }

    public void removePlayerFromArbiter(String arbiterName, String playerName) {
        ArbiterProcess arbiter = (ArbiterProcess) arbiters.get(arbiterName);
        if (arbiter == null) {
            System.out.println("Не найден процесс арбитра с именем " + arbiterName);
            return;
        }

        arbiter.removePlayer(playerName);
    }

    public void findAndKillZombie(PlayerProvider playerProvider, int arbiterZombieTimeout) {
        for (Map.Entry<String, ActorProcess> entry : arbiters.entrySet()) {
            ArbiterProcess zombieCandidate = (ArbiterProcess) entry.getValue();
            long current = System.currentTimeMillis();
            if (current - zombieCandidate.getLastTimeUsage() > arbiterZombieTimeout) {
                zombieCandidate.getProcess().destroyForcibly();
                zombieCandidate.setStatus(ProcessStatus.ZOMBIE);

                playerProvider.returnZombiePlayers(zombieCandidate.getPlayers());
                arbiters.remove(zombieCandidate.getName());
                zombie.incrementAndGet();
                System.out.println("Обнаружен и убит зомби арбитр " + entry.getKey());
            }
        }
    }

    public int getArbitersCount() {
        return arbiters.size();
    }

    public int getZombieCount() {
        return zombie.get();
    }

    @Override
    public void close() {
        arbiters.values().forEach(ActorProcess::terminate);
    }
}
