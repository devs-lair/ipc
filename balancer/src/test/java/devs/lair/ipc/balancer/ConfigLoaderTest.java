package devs.lair.ipc.balancer;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.MappedByteBuffer;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import static devs.lair.ipc.balancer.utils.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigLoaderTest {

    @Test
    @DisplayName("Positive create instance")
    void positiveCreateInstance() {
        assertDoesNotThrow(() -> {
            ConfigLoader configLoader = new ConfigLoader();
            configLoader.close();
        });
    }

    @Test
    @DisplayName("Positive create instance")
    void positiveNegativeInstance() {
        MockedStatic<Files> files = mockStatic(Files.class);
        try (files) {
            files.when(()->Files.exists(any())).thenReturn(false);
            assertThrows(IllegalArgumentException.class, ConfigLoader::new);
        }
    }

    @Test
    @DisplayName("Exception on createMemoryBuffer")
    void exceptionOnCreateMemoryBuffer() {
        MockedStatic<Files> files = mockStatic(Files.class);
        files.when(() -> Files.newByteChannel(any(), any(OpenOption[].class))).thenThrow(new IOException());
        files.when(() -> Files.exists(any())).thenReturn(true);


        ConfigLoader configLoader = new ConfigLoader();
        assertThrows(IllegalStateException.class, configLoader::loadFileToMemory);
        configLoader.close();
        files.close();
    }

    @Test
    @DisplayName("Throw when can't read")
    void throwWhenWrongBuffer() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        ConfigLoader configLoader = new ConfigLoader();
        MethodUtils.invokeMethod(configLoader, true, "createMemoryBuffer");

        MappedByteBuffer memory = (MappedByteBuffer)
                FieldUtils.readField(configLoader, "memory", true);
        memory.limit(0);

        assertThrows(IllegalStateException.class, configLoader::loadFileToMemory);
        configLoader.close();
    }

    @Test
    @DisplayName("Throw when config so big")
    void throwWhenConfigBig() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        ConfigLoader configLoader = new ConfigLoader();
        MethodUtils.invokeMethod(configLoader, true, "createMemoryBuffer");

        MockedStatic<Files> files = mockStatic(Files.class);
        files.when(() -> Files.readAllBytes(any())).thenReturn(new byte[MEMORY_SIZE * 2]);
        assertThrows(IllegalStateException.class, configLoader::loadFileToMemory);

        configLoader.close();
        files.close();
    }

    @Test
    @DisplayName("Throw when config is empty")
    void throwWhenConfigIsEmpty() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        ConfigLoader configLoader = new ConfigLoader();
        MethodUtils.invokeMethod(configLoader, true, "createMemoryBuffer");

        MockedStatic<Files> files = mockStatic(Files.class);
        files.when(() -> Files.readAllBytes(any())).thenReturn(new byte[0]);
        assertThrows(IllegalStateException.class, configLoader::loadFileToMemory);

        configLoader.close();
        files.close();
    }

    @Test
    @DisplayName("Full start")
    void startMain() throws InterruptedException, IllegalAccessException {
        Thread starter = new Thread(() -> ConfigLoader.main(new String[0]));
        starter.setUncaughtExceptionHandler((t, e) ->
                assertThat(e).isInstanceOf(IllegalStateException.class));
        starter.start();

        Thread.sleep(50);

        ConfigLoader cl = (ConfigLoader) FieldUtils
                .readDeclaredStaticField(ConfigLoader.class, "cl", true);
        cl.close();

        Awaitility.await()
                .timeout(50, TimeUnit.MILLISECONDS)
                .pollDelay(5, TimeUnit.MILLISECONDS)
                .until(() -> !starter.isAlive());
        assertThat(starter.isAlive()).isFalse();
    }

    @Test
    @DisplayName("Interrupt in sleep ")
    void interruptInSleepMain() throws InterruptedException {
        WAIT_OTHERS_TIMEOUT = 5000;
        Thread starter = new Thread(() -> ConfigLoader.main(new String[0]));
        starter.start();
        starter.setUncaughtExceptionHandler((t, e) ->
                assertThat(e).isInstanceOf(IllegalStateException.class));
        Thread.sleep(100);

        starter.interrupt();
        Awaitility.await()
                .timeout(150, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until(() -> !starter.isAlive());

        assertThat(starter.isAlive()).isFalse();
        WAIT_OTHERS_TIMEOUT = 25;
    }

    @Test
    @DisplayName("Start by instance")
    void startByInstance() throws IOException, InterruptedException {
        ConfigLoader configLoader = Mockito.spy(ConfigLoader.class);
        Thread starter = new Thread(configLoader::init);
        starter.start();

        Thread.sleep(100);

        byte[] saved = Files.readAllBytes(CONFIG_PATH);
        try {
            Files.write(CONFIG_PATH, "1".getBytes(), StandardOpenOption.APPEND);
            verify(configLoader, timeout(500).times(2)).loadFileToMemory();
        } finally {
            Files.write(CONFIG_PATH, saved);
        }
    }

    @Test
    @DisplayName("Modify not config file")
    void modifyAnotherFile() throws InterruptedException, IOException {
        ConfigLoader configLoader = Mockito.spy(ConfigLoader.class);
        Thread starter = new Thread(configLoader::init);
        starter.start();
        starter.setUncaughtExceptionHandler((t, e) ->
                assertThat(e).isInstanceOf(IllegalStateException.class));

        Thread.sleep(100);

        Files.write(MEMORY_CONFIG_PATH, "1".getBytes(), StandardOpenOption.APPEND);
        verify(configLoader, timeout(500).times(1)).loadFileToMemory();
        starter.interrupt();
        configLoader.close();
    }
}