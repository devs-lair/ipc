package devs.lair.ipc.balancer.service;


import devs.lair.ipc.balancer.service.enums.ProcessType;
import devs.lair.ipc.balancer.service.model.ActorProcess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessStarter {
    private static final ProcessBuilder processBuilder = new ProcessBuilder();
    private static final String CLASS_PATH = ProcessStarter.class.getProtectionDomain()
            .getCodeSource().getLocation().getPath();

    private static final List<String> commands = List.of("java", "-cp", CLASS_PATH);

    public static ActorProcess startProcess(ProcessType type, String[] args) {
        if (type == null)
            throw new IllegalArgumentException("Необходимо передать тип");

        try {
            List<String> executeLine = new ArrayList<>(commands);
            executeLine.add(type.getMainClass().getCanonicalName());
            if (args != null) {
                executeLine.addAll(Arrays.stream(args).toList());
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

    public static ActorProcess startProcess(ProcessType type) {
        return startProcess(type, null);
    }
}
