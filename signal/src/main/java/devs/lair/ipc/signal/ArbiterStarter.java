package devs.lair.ipc.signal;

import java.io.IOException;

public class ArbiterStarter {

    public static void main(String[] args) throws IOException, InterruptedException {
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
