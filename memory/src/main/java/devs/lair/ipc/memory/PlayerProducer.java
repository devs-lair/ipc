package devs.lair.ipc.memory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PlayerProducer {
    private static final String CLASS_PATH = "/home/devslair/prjs/ipc/rmi/target/classes";
    private static final String MAIN_CLASS = "devs.lair.ipc.rmi.Player";
    private static final String[] COMMANDS = {"java", "-cp", CLASS_PATH, MAIN_CLASS};

    private static int initialPlayerCount = 4;    //начальное количество игроков
    private static int maxPlayerCount = 10;       //максимальное
    private static int spawnPeriod = 1000;        //интервал производства
    private static int currentCount = 1;
    private static boolean isStop = false;

    private static final Set<Process> players = new HashSet<>();

    public static void main(String[] args) {
        extractArgs(args);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            isStop = true;
            players.forEach(Process::destroy);
        }));

        try {
            while (!isStop) {
                if (currentCount < initialPlayerCount || currentCount <= maxPlayerCount) {
                    players.add(new ProcessBuilder(COMMANDS).start());
                    System.out.println("Стартовал процесс игрока с номером " + currentCount);
                    currentCount++;
                }
                Thread.sleep(currentCount > initialPlayerCount
                        ? spawnPeriod : 10);
            }
        } catch (IOException e) {
            System.out.println("При создании процесса произошла ошибка"); // от start()
        } catch (InterruptedException e) {
            System.out.println("Процесс был прерван");
        }
    }

    private static void extractArgs(String[] args) {
        try {
            initialPlayerCount = ifNegativeThrow(Integer.parseInt(args[0]));
            maxPlayerCount = ifNegativeThrow(Integer.parseInt(args[1]));
            spawnPeriod = ifNegativeThrow(Integer.parseInt(args[2]));
        } catch (IndexOutOfBoundsException ignored) {
        }
    }

    private static int ifNegativeThrow(int number) {
        if (number < 0) {
            throw new IllegalArgumentException("Значение должно быть положительным, передано " + number);
        }
        return number;
    }
}
