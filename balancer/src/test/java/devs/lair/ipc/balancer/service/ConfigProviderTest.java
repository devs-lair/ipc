package devs.lair.ipc.balancer.service;

import devs.lair.ipc.balancer.ConfigLoader;
import devs.lair.ipc.balancer.utils.Constants;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.MappedByteBuffer;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static devs.lair.ipc.balancer.utils.Constants.DEFAULT_POLL_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigProviderTest {

    @Test
    @Order(1)
    @DisplayName("Create instance without loader")
    void createInstanceWithoutLoader() {
        assertDoesNotThrow(() -> new ConfigProvider());
    }

    @Test
    @DisplayName("Creaet and stop")
    void createAndStopTest() throws IllegalAccessException {
        ConfigProvider configProvider = new ConfigProvider();
        configProvider.close();

        Thread watchThread = readWatchThread(configProvider);
        boolean isStop = readIsStop(configProvider);

        int pollTimeout = (int) FieldUtils.readField(configProvider, "pollTimeout", true);

        assertThat(pollTimeout).isEqualTo(Constants.DEFAULT_POLL_TIMEOUT);
        assertThat(watchThread).isNull();
        assertThat(isStop).isTrue();
    }

    @Test
    @DisplayName("Positive poll timeout")
    void positivePollTimeout() {
        assertDoesNotThrow(() -> {
            try (ConfigProvider configProvider = new ConfigProvider(100)) {
                int pollTimeout = (int) FieldUtils.readField(configProvider, "pollTimeout", true);
                assertThat(pollTimeout).isEqualTo(100);
            }
        });
    }


    @Test
    @SuppressWarnings("resource")
    @DisplayName("Negative and zero poll timeout")
    void negativeAndZeroPollTimeout() {
        assertThrows(IllegalArgumentException.class, () -> new ConfigProvider(0));
        assertThrows(IllegalArgumentException.class, () -> new ConfigProvider(-10));
    }

    @Test
    @DisplayName("Load config without loader")
    void loadConfigWithoutLoader() {
        try (ConfigProvider configProvider = new ConfigProvider()) {
            assertThat(configProvider.getPlayerTick()).isPositive();
            assertThat(configProvider.getArbiterTick()).isPositive();
            assertThat(configProvider.getMaxRound()).isPositive();
            assertThat(configProvider.getMaxPlayerCount()).isPositive();
            assertThat(configProvider.getProducerTick()).isPositive();

            assertDoesNotThrow(configProvider::loadConfig);

            assertThat(configProvider.getPlayerTick()).isPositive();
            assertThat(configProvider.getArbiterTick()).isPositive();
            assertThat(configProvider.getProperty(Constants.INITIAL_PLAYER_COUNT_KEY)).isNull();
            assertThat(configProvider.getProperty(Constants.MAX_ATTEMPT_KEY)).isNull();
        }
    }

    @Test
    @DisplayName("Load config from file")
    void loadConfigWithConfigFile() {
        try (ConfigLoader configLoader = new ConfigLoader();
             ConfigProvider configProvider = new ConfigProvider()) {
            configLoader.loadFileToMemory();
            configProvider.loadConfig();

            assertThat(configProvider.getProperty(Constants.MAX_ATTEMPT_KEY)).isNotNull();
        }
    }

    @Test
    @DisplayName("Catch errors on loadConfig")
    void catchErrorsOnLoadConfig() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        try (ConfigLoader configLoader = new ConfigLoader();
             ConfigProvider configProvider = new ConfigProvider()) {
            configLoader.loadFileToMemory();

            MethodUtils.invokeMethod(configProvider, true, "initMemoryBuffer");
            MappedByteBuffer memory = (MappedByteBuffer) FieldUtils
                    .readField(configProvider, "memory", true);
            memory.limit(0);

            assertDoesNotThrow(configProvider::loadConfig);
            assertThat(configProvider.getPlayerTick()).isPositive();
        }
    }

    @Test
    @DisplayName("Catch errors on loadConfig")
    void catchErrorsReloadProps() throws IllegalAccessException, IOException {
        Properties mockProperties = mock(Properties.class);
        doThrow(new IOException()).when(mockProperties).load(any(InputStream.class));

        try (ConfigLoader configLoader = new ConfigLoader();
             ConfigProvider configProvider = new ConfigProvider()) {
            configLoader.loadFileToMemory();

            FieldUtils.writeField(configProvider, "props",
                    mockProperties, true);

            assertDoesNotThrow(configProvider::loadConfig);
            assertThat(configProvider.getPlayerTick()).isPositive();
        }
    }

    @Test
    @DisplayName("Throw when updating")
    void throwWhenUpdating() throws IllegalAccessException {
        ConfigProvider configProvider = new ConfigProvider();
        FieldUtils.writeField(configProvider, "updating", true, true);
        assertThrows(IllegalStateException.class, () -> configProvider.getProperty("SOME"));
    }

    @Test
    @DisplayName("Try parse incorrect props value")
    void tryParseIncorrect() throws IllegalAccessException {
        Properties mockProperties = mock(Properties.class);
        when(mockProperties.getProperty(any())).thenReturn("INVALID");

        ConfigProvider configProvider = new ConfigProvider();
        FieldUtils.writeField(configProvider, "props", mockProperties, true);

        int intValue = configProvider.getInt("propName", 10, null);
        assertThat(intValue).isEqualTo(10);
    }

    @Test
    @DisplayName("Negative in props file")
    void negativeInProps() throws IllegalAccessException {
        try (ConfigLoader configLoader = new ConfigLoader();
             ConfigProvider configProvider = new ConfigProvider()) {
            configLoader.loadFileToMemory();

            Properties mockProperties = mock(Properties.class);
            when(mockProperties.getProperty(any())).thenReturn("-10");
            FieldUtils.writeField(configProvider, "props", mockProperties, true);

            assertDoesNotThrow(configProvider::loadConfig);
        }
    }

    @Test
    @DisplayName("Start watch without loader")
    void startWatchWithoutLoader() throws IllegalAccessException {
        ConfigProvider configProvider = Mockito.spy(ConfigProvider.class);

        new Thread(configProvider::init).start();
        verify(configProvider, timeout(2L * DEFAULT_POLL_TIMEOUT).times(2)).loadConfig();

        Thread watchThread = readWatchThread(configProvider);
        assertThat(watchThread).isNotNull();
        assertThat(watchThread.isAlive()).isTrue();

        verify(configProvider, timeout(2L * DEFAULT_POLL_TIMEOUT).times(3)).loadConfig();

        configProvider.close();

        Awaitility.await()
                .timeout(25, TimeUnit.MILLISECONDS)
                .pollDelay(5, TimeUnit.MILLISECONDS)
                .until(() -> !watchThread.isAlive());

        assertThat(watchThread.isAlive()).isFalse();
        assertThat(readIsStop(configProvider)).isTrue();
    }

    @Test
    @DisplayName("Start watch with loader")
    void startWatchWithLoader() {
        try (ConfigLoader configLoader = new ConfigLoader();
             ConfigProvider configProvider = Mockito.spy(ConfigProvider.class)) {
            configLoader.loadFileToMemory();

            new Thread(configProvider::init).start();
            verify(configProvider, timeout(2L * DEFAULT_POLL_TIMEOUT)
                    .times(2)).loadConfig();
        }
    }

    @Test
    @DisplayName("Second start")
    void trySecondStart() throws IllegalAccessException {
        ConfigProvider configProvider = new ConfigProvider();
        configProvider.startWatch();
        assertDoesNotThrow(configProvider::startWatch);
        configProvider.close();

        assertDoesNotThrow(configProvider::startWatch);

        Thread watchThread = readWatchThread(configProvider);
        Awaitility.await()
                .timeout(25, TimeUnit.MILLISECONDS)
                .pollDelay(5, TimeUnit.MILLISECONDS)
                .until(() -> !watchThread.isAlive());
        assertThat(watchThread.isAlive()).isFalse();
    }

    private boolean readIsStop(ConfigProvider configProvider) throws IllegalAccessException {
        return (boolean) FieldUtils.readField(configProvider, "isStop", true);
    }

    private Thread readWatchThread(ConfigProvider configProvider) throws IllegalAccessException {
        return (Thread) FieldUtils.readField(configProvider, "watchThread", true);
    }
}