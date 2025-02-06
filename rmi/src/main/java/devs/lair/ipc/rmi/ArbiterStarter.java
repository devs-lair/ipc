package devs.lair.ipc.rmi;

public class ArbiterStarter {

    public static void main(String[] args) throws InterruptedException {
        int tick = 1000;

        Arbiter arbiter = new Arbiter(tick);
        Runtime.getRuntime().addShutdownHook(new Thread(arbiter::stop));
        arbiter.start();
    }
}
