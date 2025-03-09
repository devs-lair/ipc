package devs.lair.ipc.balancer.service.model;

import devs.lair.ipc.balancer.service.enums.ProcessStatus;
import devs.lair.ipc.balancer.service.enums.ProcessType;
import devs.lair.ipc.balancer.utils.Utils;

import static devs.lair.ipc.balancer.service.enums.ProcessStatus.*;
import static devs.lair.ipc.balancer.service.enums.ProcessType.ARBITER;

public class ActorProcess {
    private final String name;
    private final Process process;
    private final ProcessType type;

    private ProcessStatus status = STARTED;

    public ActorProcess(Process process, ProcessType type, String name) {
        checkArgs(process, name, type);

        this.process = process;
        this.name = name;
        this.type = type;
    }

    public ActorProcess(Process process, ProcessType type) {
        this(process, type, Utils.generateUniqueName(null,
                type.toString().toLowerCase()));
    }

    public void terminate() {
        process.destroy();
        status = TERMINATING;
        System.out.printf("Процессу отправлен сигнал на завершение: имя %s, тип %s, pid = %d \n",
                name, type, process.pid());
    }

    public boolean isArbiter() {
        return type == ARBITER;
    }

    public boolean isTerminating() {
        return status == TERMINATING;
    }

    public boolean isDead() {
        return status == DEAD;
    }

    public Process getProcess() {
        return process;
    }

    public String getName() {
        return name;
    }

    public ProcessType getType() {
        return type;
    }

    public ProcessStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessStatus status) {
        this.status = status;
    }

    private void checkArgs(Process process, String name, ProcessType type) {
        if (process == null) {
            throw new IllegalArgumentException("Процесс не может быть null");
        }

        if (Utils.isNullOrEmpty(name)) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }

        if (type == null) {
            throw new IllegalArgumentException("Тип не может быть null");
        }
    }
}
