package devs.lair.ipc.watchapi;

public class PlayerStarter {

    public static void main(String[] args) {
        int tick = 1000;

        if (args.length == 0) {
            System.out.println("Передайте имя игрока в параметрах!");
            System.exit(-1);
        }

        String name = args[0];

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

