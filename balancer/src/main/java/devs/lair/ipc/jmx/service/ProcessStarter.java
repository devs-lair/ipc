package devs.lair.ipc.jmx.service;


import devs.lair.ipc.jmx.service.enums.ProcessType;
import devs.lair.ipc.jmx.service.model.ActorProcess;
import devs.lair.ipc.jmx.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessStarter {
    private static final ProcessBuilder processBuilder = new ProcessBuilder();
    private static final String CLASS_PATH = ProcessStarter.class.getProtectionDomain()
            .getCodeSource().getLocation().getPath();

    private static final List<String> commands = List.of("java", "-cp", CLASS_PATH);

    public static ActorProcess startProcess(ProcessType type, String[] args, boolean passName) {
        if (type == null)
            throw new IllegalArgumentException("Необходимо передать тип");

        try {
            List<String> executeLine = new ArrayList<>(commands);
            executeLine.add(type.getMainClass().getCanonicalName());

            String name = Utils.generateUniqueName(
                    type.toString().toLowerCase());

            if (passName) {
                executeLine.add(name);
            }

            if (args != null) {
                executeLine.addAll(Arrays.stream(args).toList());
            }

            Process process = processBuilder.command(executeLine).start();
            ActorProcess actorProcess = new ActorProcess(process, type, name);

            System.out.printf("Процесс был запущен: имя %s, тип %s, pid = %d \n",
                    actorProcess.getName(),
                    actorProcess.getType(),
                    actorProcess.getProcess().pid());

            return actorProcess;
        } catch (IOException e) {
            System.out.println("Не удалось запустить процесс " + e.getMessage());
        }
        return null;
    }

    public static ActorProcess startProcess(ProcessType type) {
        return startProcess(type, null, true);
    }
}
