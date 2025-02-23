package devs.lair.ipc.balancer;

import devs.lair.ipc.balancer.service.ConfigProvider;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static devs.lair.ipc.balancer.utils.Constants.INITIAL_PLAYER_COUNT_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlayerProducerTest {

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
    void startProduce() throws IOException, IllegalAccessException {
        ConfigProvider mockConfigProvider = getMockForProvider(2, 4, 10);
        ProcessBuilder mockProcessBuilder = getMockForProcessBuilder();
        PlayerProducer playerProducer = new PlayerProducer();
        changeToMocks(playerProducer, mockConfigProvider, mockProcessBuilder);

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
    @DisplayName("Start produce, zero initial")
    void startProduceWithZeroInitial() throws IOException, IllegalAccessException {
        ConfigProvider mockConfigProvider = getMockForProvider(2, 4, 10);
        ProcessBuilder mockProcessBuilder = getMockForProcessBuilder();
        when(mockConfigProvider.getInt(eq(INITIAL_PLAYER_COUNT_KEY), eq(2), any()))
                .thenReturn(0);

        PlayerProducer playerProducer = new PlayerProducer();
        changeToMocks(playerProducer, mockConfigProvider, mockProcessBuilder);

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
    void startProduceNotMockedConfigProvider() throws IOException, IllegalAccessException {
        ProcessBuilder mockProcessBuilder = getMockForProcessBuilder();

        PlayerProducer playerProducer = new PlayerProducer();
        changeToMocks(playerProducer, null, mockProcessBuilder);

        new Thread(playerProducer::startProduce).start();
        Set<Process> players = readSetOfProcess(playerProducer);

        Awaitility.await()
                .timeout(500, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until(() -> players.size() == 4); //check defs

        assertThat(players).hasSize(4);
        playerProducer.stop();
    }

    @Test
    @DisplayName("Start produce, all process dead")
    void startProduceWithDeadProcess() throws IOException, IllegalAccessException, InterruptedException {
        ConfigProvider mockConfigProvider = getMockForProvider(2, 4, 10);
        ProcessBuilder mockProcessBuilder = getMockForProcessBuilder();
        when(mockProcessBuilder.start()).thenAnswer(invocation -> new AlwaysDead());

        PlayerProducer playerProducer = new PlayerProducer();
        changeToMocks(playerProducer, mockConfigProvider, mockProcessBuilder);

        new Thread(playerProducer::startProduce).start();

        int spawnPeriod = mockConfigProvider.getSpawnPeriod();
        Thread.sleep(spawnPeriod * 5L);

        Set<Process> players = readSetOfProcess(playerProducer);
        assertThat(players).hasSize(0);
        playerProducer.stop();
    }

    @Test
    @DisplayName("Exception throw")
    void exceptionThrow() throws Exception {
        ConfigProvider mockConfigProvider = getMockForProvider(2, 4, 10);
        ProcessBuilder mockProcessBuilder = getMockForProcessBuilder();
        when(mockProcessBuilder.start()).thenThrow(IOException.class);

        PlayerProducer playerProducer = new PlayerProducer();
        changeToMocks(playerProducer, mockConfigProvider, mockProcessBuilder);

        assertThrows(IllegalStateException.class, playerProducer::startProduce);
        playerProducer.stop();
    }

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

        PlayerProducer pp = (PlayerProducer) FieldUtils
                .readDeclaredStaticField(PlayerProducer.class, "pp", true);
        pp.stop();

        Awaitility.await()
                .timeout(150, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until(() -> !starter.isAlive());

    }

    private ProcessBuilder getMockForProcessBuilder() throws IOException {
        ProcessBuilder processBuilder = mock(ProcessBuilder.class);
        when(processBuilder.command(any(String[].class))).thenCallRealMethod();
        when(processBuilder.start()).thenAnswer(invocation -> new MockProcess());
        return processBuilder;
    }

    private ConfigProvider getMockForProvider(int initialCount, int maxCount, int spanPeriod) {
        ConfigProvider configProvider = mock(ConfigProvider.class);
        when(configProvider.getInt(eq(INITIAL_PLAYER_COUNT_KEY), eq(2), any()))
                .thenReturn(initialCount);
        when(configProvider.getMaxPlayerCount()).thenReturn(maxCount);
        when(configProvider.getSpawnPeriod()).thenReturn(spanPeriod);
        return configProvider;
    }

    @SuppressWarnings("unchecked")
    private Set<Process> readSetOfProcess(PlayerProducer playerProducer) throws IllegalAccessException {
        return (Set<Process>) FieldUtils.readField(playerProducer, "players", true);
    }

    private void changeToMocks(PlayerProducer playerProducer,
                               ConfigProvider configProvider,
                               ProcessBuilder processBuilder) throws IllegalAccessException {
        if (configProvider != null) {
            FieldUtils.writeField(playerProducer, "configProvider", configProvider, true);
        }

        if (processBuilder != null) {
            FieldUtils.writeField(playerProducer, "processBuilder", processBuilder, true);
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

    static class AlwaysDead extends MockProcess {
        @Override
        public boolean isAlive() {
            return false;
        }
    }
}