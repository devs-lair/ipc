package devs.lair.ipc.balancer.service.model;

import devs.lair.ipc.balancer.service.enums.ProcessStatus;
import devs.lair.ipc.balancer.service.enums.ProcessType;
import devs.lair.ipc.balancer.utils.Utils;

import static devs.lair.ipc.balancer.service.enums.ProcessStatus.DEAD;
import static devs.lair.ipc.balancer.service.enums.ProcessStatus.STARTED;
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
        status = DEAD;
        System.out.printf("Процесс был завершен: имя %s, тип %s, pid = %d \n",
                name, type, process.pid());
    }

    public boolean isArbiter() {
        return type == ARBITER;
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
