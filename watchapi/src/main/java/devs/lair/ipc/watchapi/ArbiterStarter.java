package devs.lair.ipc.watchapi;

public class ArbiterStarter {

    public static void main(String[] args) throws InterruptedException {
        int tick = 1000;

        if (args.length < 2) {
            System.out.println("Передайте имена игроков");
            System.exit(-1);
        }

        Arbiter arbiter = new Arbiter(args[0], args[1], tick);
        Runtime.getRuntime().addShutdownHook(new Thread(arbiter::clearFiles));
        arbiter.start();
    }
}
