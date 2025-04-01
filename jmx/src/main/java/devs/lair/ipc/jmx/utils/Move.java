package devs.lair.ipc.jmx.utils;

import java.util.Random;

public enum Move {
    ROCK,
    SCISSORS,
    PAPER;

    public int compareWith(Move other) {
        if (this == other)  return 0;

        return this == ROCK && other == SCISSORS
                || this == PAPER && other == ROCK
                || this == SCISSORS && other == PAPER
                ? 1
                : -1;
    }

    private static final Random random = new Random();

    public static Move getRandomMove() {
        return switch (random.nextInt(3)) {
            case 0 -> ROCK;
            case 1 -> SCISSORS;
            default -> PAPER;
        };
    }

    public static Move valueOf(byte[] bytes) {
        return valueOf(new String(bytes));
    }

    public static byte[] getRandomMoveBytes() {
        return getRandomMove().toString().getBytes();
    }
}
