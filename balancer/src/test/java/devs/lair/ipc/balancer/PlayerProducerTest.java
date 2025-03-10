package devs.lair.ipc.balancer;

import devs.lair.ipc.balancer.service.ConfigProvider;
import devs.lair.ipc.balancer.service.ProcessStarter;
import devs.lair.ipc.balancer.service.enums.ProcessType;
import devs.lair.ipc.balancer.service.model.ActorProcess;
import devs.lair.ipc.balancer.utils.Utils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockedStatic;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static devs.lair.ipc.balancer.utils.Constants.INITIAL_PLAYER_COUNT_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlayerProducerTest {

    @BeforeAll
    void beforeAll() {
        MockedStatic<ProcessStarter> starter = mockStatic(ProcessStarter.class);
        starter.when(()->ProcessStarter.startProcess(any()))
                .thenReturn(new ActorProcess(new MockProcess(),
                        ProcessType.PLAYER,
                        Utils.generateUniqueName("player")));
    }

    @Test
    @DisplayName("Create instance")
    void createInstanceAndStop() {
        assertDoesNotThrow(() -> {
            PlayerProducer playerProducer = new PlayerProducer();
            playerProducer.stop();
        });
    }

    @Test
    @DisplayName("Start produce, standard way")
    void startProduce() throws IllegalAccessException {
        ConfigProvider mockConfigProvider = getMockForProvider(2, 4, 10);
        PlayerProducer playerProducer = new PlayerProducer();
        changeToMocks(playerProducer, mockConfigProvider);

        new Thread(playerProducer::startProduce).start();

        Set<Process> players = readSetOfProcess(playerProducer);

        Awaitility.await()
                .timeout(150, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until(() -> players.size() == 4);

        assertThat(players).hasSize(4);
        playerProducer.stop();
    }

    @Test
    @DisplayName("Start produce, not mock configProvider")
    void startProduceNotMockedConfigProvider() throws IllegalAccessException {
        PlayerProducer playerProducer = new PlayerProducer();
        changeToMocks(playerProducer, null);

        new Thread(playerProducer::startProduce).start();
        Set<Process> players = readSetOfProcess(playerProducer);

        Awaitility.await()
                .timeout(500, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until(() -> players.size() == 4); //check defs

        assertThat(players).hasSize(4);
        playerProducer.stop();
    }

//    @Test
//    @DisplayName("Start produce, all process dead")
//    void startProduceWithDeadProcess() throws IOException, IllegalAccessException, InterruptedException {
//        ConfigProvider mockConfigProvider = getMockForProvider(2, 4, 10);
//        PlayerProducer playerProducer = new PlayerProducer();
//        changeToMocks(playerProducer, mockConfigProvider);
//
//        new Thread(playerProducer::startProduce).start();
//
//        int spawnPeriod = mockConfigProvider.getSpawnPeriod();
//        Thread.sleep(spawnPeriod * 5L);
//
//        Set<Process> players = readSetOfProcess(playerProducer);
//        assertThat(players).hasSize(0);
//        playerProducer.stop();
//    }

    @Test
    @DisplayName("Start main")
    void startMain() throws Exception {
        Thread starter = new Thread(() -> PlayerProducer.main(new String[0]));
        starter.start();
        starter.setUncaughtExceptionHandler((t, e)
                -> assertThat(e).isInstanceOf(IllegalStateException.class));

        Thread.sleep(500); // check default values
        starter.interrupt();

        Awaitility.await()
                .timeout(150, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until(() -> !starter.isAlive());

    }

    @Test
    @DisplayName("Start main, and normal end")
    void startMainAndNormalEnd() throws Exception {
        Thread starter = new Thread(() -> PlayerProducer.main(new String[0]));
        starter.start();
        starter.setUncaughtExceptionHandler((t, e)
                -> assertThat(e).isInstanceOf(IllegalStateException.class));

        Thread.sleep(500); // check default values
        starter.interrupt();


        Awaitility.await()
                .timeout(150, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until(() -> !starter.isAlive());

    }

    private ConfigProvider getMockForProvider(int initialCount, int maxCount, int spanPeriod) {
        ConfigProvider configProvider = mock(ConfigProvider.class);
        when(configProvider.getInt(eq(INITIAL_PLAYER_COUNT_KEY), eq(2), any()))
                .thenReturn(initialCount);
        when(configProvider.getMaxPlayerCount()).thenReturn(maxCount);
        when(configProvider.getProducerTick()).thenReturn(spanPeriod);
        return configProvider;
    }

    @SuppressWarnings("unchecked")
    private Set<Process> readSetOfProcess(PlayerProducer playerProducer) throws IllegalAccessException {
        return (Set<Process>) FieldUtils.readField(playerProducer, "players", true);
    }

    private void changeToMocks(PlayerProducer playerProducer,
                               ConfigProvider configProvider) throws IllegalAccessException {
        if (configProvider != null) {
            FieldUtils.writeField(playerProducer, "configProvider", configProvider, true);
        }
    }

    static class MockProcess extends Process {
        private static int counter = 0;
        private final int pid;

        public MockProcess() {
            counter++;
            this.pid = counter;
        }

        @Override
        public OutputStream getOutputStream() {
            return null;
        }

        @Override
        public InputStream getInputStream() {
            return null;
        }

        @Override
        public InputStream getErrorStream() {
            return null;
        }

        @Override
        public int waitFor() {
            return 0;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public void destroy() {

        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            MockProcess that = (MockProcess) o;
            return pid == that.pid;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(pid);
        }
    }
}