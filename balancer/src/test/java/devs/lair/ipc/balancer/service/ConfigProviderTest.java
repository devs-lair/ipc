package devs.lair.ipc.balancer.service;

import devs.lair.ipc.balancer.ConfigLoader;
import devs.lair.ipc.balancer.utils.Constants;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.MappedByteBuffer;
import java.nio.file.Paths;
import java.util.Properties;

import static devs.lair.ipc.balancer.utils.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigProviderTest {

    @BeforeAll
    void beforeAll() {
        CONFIG_PATH = Paths.get("../" + CONFIG_DIR + "/" + CONFIG_FILE);
        MEMORY_CONFIG_PATH = Paths.get("../" + CONFIG_DIR + "/" + MEMORY_CONFIG_FILE);
        DEFAULT_POLL_TIMEOUT = 50;
    }


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
            ConfigProvider configProvider = new ConfigProvider(100);
            int pollTimeout = (int) FieldUtils.readField(configProvider, "pollTimeout", true);
            assertThat(pollTimeout).isEqualTo(100);
            configProvider.close();
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
            assertThat(configProvider.getSpawnPeriod()).isPositive();

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
        ConfigLoader configLoader = new ConfigLoader();
        configLoader.loadFileToMemory();

        ConfigProvider configProvider = new ConfigProvider();
        configProvider.loadConfig();

        assertThat(configProvider.getProperty(INITIAL_PLAYER_COUNT_KEY)).isNotNull();
        assertThat(configProvider.getProperty(Constants.MAX_ATTEMPT_KEY)).isNotNull();

        configProvider.close();
        configLoader.close();
    }

    @Test
    @DisplayName("Catch errors on loadConfig")
    void catchErrorsOnLoadConfig() throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        ConfigLoader configLoader = new ConfigLoader();
        configLoader.loadFileToMemory();

        ConfigProvider configProvider = new ConfigProvider();
        MethodUtils.invokeMethod(configProvider, true, "initMemoryBuffer");

        MappedByteBuffer memory = (MappedByteBuffer) FieldUtils.readField(configProvider, "memory", true);
        memory.limit(0);

        assertDoesNotThrow(configProvider::loadConfig);
        assertThat(configProvider.getPlayerTick()).isPositive();

        configProvider.close();
        configLoader.close();
    }

    @Test
    @DisplayName("Catch errors on loadConfig")
    void catchErrorsReloadProps() throws IllegalAccessException, IOException {
        Properties mockProperties = mock(Properties.class);
        doThrow(new IOException()).when(mockProperties).load(any(InputStream.class));

        ConfigLoader configLoader = new ConfigLoader();
        configLoader.loadFileToMemory();

        ConfigProvider configProvider = new ConfigProvider();
        FieldUtils.writeField(configProvider, "props", mockProperties, true);

        assertDoesNotThrow(configProvider::loadConfig);
        assertThat(configProvider.getPlayerTick()).isPositive();

        configProvider.close();
        configLoader.close();
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
        ConfigLoader configLoader = new ConfigLoader();
        configLoader.loadFileToMemory();

        Properties mockProperties = mock(Properties.class);
        when(mockProperties.getProperty(any())).thenReturn("-10");

        ConfigProvider configProvider = new ConfigProvider();
        FieldUtils.writeField(configProvider, "props", mockProperties, true);

        assertDoesNotThrow(configProvider::loadConfig);
        configLoader.close();
        configProvider.close();
    }

    @Test
    @DisplayName("Start watch without loader")
    void startWatchWithoutLoader() throws IllegalAccessException, InterruptedException {
        ConfigProvider configProvider = Mockito.spy(ConfigProvider.class);

        new Thread(configProvider::init).start();
        verify(configProvider, timeout(2L * DEFAULT_POLL_TIMEOUT).times(2)).loadConfig();

        Thread watchThread = readWatchThread(configProvider);
        assertThat(watchThread).isNotNull();
        assertThat(watchThread.isAlive()).isTrue();

        verify(configProvider, timeout(3L * DEFAULT_POLL_TIMEOUT).times(3)).loadConfig();

        configProvider.close();
        Thread.sleep(2L * DEFAULT_POLL_TIMEOUT);
        assertThat(readIsStop(configProvider)).isTrue();
        assertThat(watchThread.isAlive()).isFalse();
    }

    @Test
    @DisplayName("Start watch with loader")
    void startWatchWithLoader() {
        ConfigProvider configProvider = Mockito.spy(ConfigProvider.class);
        ConfigLoader configLoader = new ConfigLoader();
        configLoader.loadFileToMemory();

        new Thread(configProvider::init).start();
        verify(configProvider, timeout(2L * DEFAULT_POLL_TIMEOUT).times(2)).loadConfig();

        configProvider.close();
    }

    @Test
    @DisplayName("Second start")
    void trySecondStart() throws InterruptedException, IllegalAccessException {
        ConfigProvider configProvider = new ConfigProvider();
        configProvider.startWatch();
        assertDoesNotThrow(configProvider::startWatch);
        configProvider.close();

        assertDoesNotThrow(configProvider::startWatch);
        Thread.sleep(DEFAULT_POLL_TIMEOUT);

        Thread watchThread = readWatchThread(configProvider);
        assertThat(watchThread.isAlive()).isFalse();
    }

    private boolean readIsStop(ConfigProvider configProvider) throws IllegalAccessException {
        return (boolean) FieldUtils.readField(configProvider, "isStop", true);
    }

    private Thread readWatchThread(ConfigProvider configProvider) throws IllegalAccessException {
        return (Thread) FieldUtils.readField(configProvider, "watchThread", true);
    }
}