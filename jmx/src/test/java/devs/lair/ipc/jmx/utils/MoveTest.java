package devs.lair.ipc.jmx.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoveTest {

    @Test
    @DisplayName("Compare With")
    void compareWithTest() {
        Move rock = Move.ROCK;
        Move paper = Move.PAPER;
        Move scissors = Move.SCISSORS;

        assertThat(rock.compareWith(rock)).isEqualTo(0);
        assertThat(rock.compareWith(paper)).isEqualTo(-1);
        assertThat(rock.compareWith(scissors)).isEqualTo(1);

        assertThat(paper.compareWith(paper)).isEqualTo(0);
        assertThat(paper.compareWith(rock)).isEqualTo(1);
        assertThat(paper.compareWith(scissors)).isEqualTo(-1);

        assertThat(scissors.compareWith(scissors)).isEqualTo(0);
        assertThat(scissors.compareWith(rock)).isEqualTo(-1);
        assertThat(scissors.compareWith(paper)).isEqualTo(1);
    }

    @Test
    @DisplayName("Get random move")
    void testRandomMove() {
        Move randomMove = Move.getRandomMove();
        Move anoherMove;
        int tryCount = 1;

        while (randomMove == (anoherMove = Move.getRandomMove()) && tryCount <= 100) {
            tryCount++;
        }

        assertThat(tryCount).isLessThan(100);
        assertThat(anoherMove).isNotEqualTo(randomMove);
    }

    @Test
    @DisplayName("Get default case")
    void testDefaultCase() {
        int tryCount = 1;

        while (Move.getRandomMove()!=Move.PAPER && tryCount <= 100) {
            tryCount++;
        }
        assertThat(tryCount).isLessThan(100);
    }


    @Test
    @DisplayName("Throw exception on wrong bytes")
    void testThrowOnValueOfException() {
        assertThrows(IllegalArgumentException.class, ()->Move.valueOf("SOME".getBytes()));
    }

    @Test
    @DisplayName("Positive valueOf")
    void positiveValueOf() {
        Move rock = Move.valueOf("ROCK".getBytes());
        assertThat(rock).isEqualTo(Move.ROCK);

        Move paper = Move.valueOf("PAPER".getBytes());
        assertThat(paper).isEqualTo(Move.PAPER);

        Move scissors = Move.valueOf("SCISSORS".getBytes());
        assertThat(scissors).isEqualTo(Move.SCISSORS);
    }

    @Test
    @DisplayName("Test gee bytes")
    void testGetRandomBytes() {
        assertDoesNotThrow(Move::getRandomMoveBytes);
        assertDoesNotThrow(Move::getRandomMoveBytes);
        assertDoesNotThrow(Move::getRandomMoveBytes);
    }
}