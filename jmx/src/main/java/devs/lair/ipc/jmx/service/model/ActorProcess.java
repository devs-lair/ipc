package devs.lair.ipc.jmx.service.model;

import devs.lair.ipc.jmx.service.enums.ProcessStatus;
import devs.lair.ipc.jmx.service.enums.ProcessType;
import devs.lair.ipc.jmx.utils.Utils;

import static devs.lair.ipc.jmx.service.enums.ProcessStatus.*;
import static devs.lair.ipc.jmx.service.enums.ProcessType.ARBITER;

public class ActorProcess {
    private final String name;
    private final ProcessHandle process;
    private final ProcessType type;

    private ProcessStatus status = STARTED;

    public ActorProcess(Process process, ProcessType type, String name) {
        this(process.toHandle(), type, name);
    }

    public ActorProcess(ProcessHandle processHandle, ProcessType type, String name) {
        checkArgs(processHandle, name, type);

        this.process = processHandle;
        this.name = name;
        this.type = type;
    }

    public void terminate() {
        status = TERMINATING;
        process.destroy();
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
        if (status == DEAD) {
            return true;
        }
        
        if (!process.isAlive()) {
            status = DEAD;
            return true;
        }
        
        return false;
    }

    public ProcessHandle getProcess() {
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

    private void checkArgs(ProcessHandle processHandle, String name, ProcessType type) {
        if (processHandle == null) {
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
