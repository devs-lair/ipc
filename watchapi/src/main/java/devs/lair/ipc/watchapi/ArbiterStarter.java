package devs.lair.ipc.watchapi;

public class ArbiterStarter {

    public static void main(String[] args) throws InterruptedException {
        int tick = 1000;

        Arbiter arbiter = new Arbiter(tick);
        Runtime.getRuntime().addShutdownHook(new Thread(arbiter::stop));
        arbiter.start();
    }
}
