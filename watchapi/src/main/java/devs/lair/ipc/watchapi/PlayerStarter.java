package devs.lair.ipc.watchapi;

public class PlayerStarter {

    public static void main(String[] args) {
        int tick = 500;

        String name = "player" + System.currentTimeMillis();
        if (args.length > 0 && !args[0].isEmpty()) {
            name = args[0];
        }

        try {
            Player player = new Player(name, tick);
            Runtime.getRuntime().addShutdownHook(
                    new Thread(player::stop));
            player.start();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}

