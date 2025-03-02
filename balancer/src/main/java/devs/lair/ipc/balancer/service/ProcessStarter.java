package devs.lair.ipc.balancer.service;


import devs.lair.ipc.balancer.service.enums.ProcessType;
import devs.lair.ipc.balancer.service.model.ActorProcess;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static devs.lair.ipc.balancer.service.enums.ProcessType.*;

public class ProcessStarter {
    private static final String CLASS_PATH = "/home/devslair/prjs/ipc/balancer/target/classes";
    private static final String[] COMMANDS = {"java", "-cp", CLASS_PATH};

    private final ProcessBuilder processBuilder = new ProcessBuilder();
    private final Map<ProcessType, String> actorsClass = Map.of(
            ARBITER, "devs.lair.ipc.balancer.Arbiter",
            CONFIG_LOADER, "devs.lair.ipc.balancer.ConfigLoader",
            PLAYER_PRODUCER, "devs.lair.ipc.balancer.PlayerProducer"
    );

    public ActorProcess startProcess(ProcessType type, String[] args) {
        if (type == null) {
            throw new IllegalArgumentException("Необходимо передать тип");
        }

        try {
            int commandsLength = 4;
            if (args != null && args.length == 0) {
                commandsLength += args.length;
            }

            String[] executeLine = Arrays.copyOf(COMMANDS, commandsLength);
            executeLine[3] = actorsClass.get(type);

            if (args != null) {
                System.arraycopy(args, 0, executeLine, 4, args.length);
            }

            Process process = processBuilder.command(executeLine).start();
            ActorProcess actorProcess = new ActorProcess(process, type);
            System.out.printf("Процесс был запущен: имя %s, тип %s, pid = %d \n",
                    actorProcess.getName(),
                    actorProcess.getType(),
                    actorProcess.getProcess().pid() );

            return actorProcess;
        } catch (IOException e) {
            System.out.println("Не удалось запустить процесс " + e.getMessage());
        }
        return null;
    }

    public ActorProcess startProcess(ProcessType type) {
        return startProcess(type, null);
    }
}
