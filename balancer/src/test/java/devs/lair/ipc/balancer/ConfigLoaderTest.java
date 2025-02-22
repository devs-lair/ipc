package devs.lair.ipc.balancer;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.BeforeAll;
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
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static devs.lair.ipc.balancer.utils.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigLoaderTest {

    @BeforeAll
    void beforeAll() {
        CONFIG_PATH = Paths.get("../" + CONFIG_DIR + "/" + CONFIG_FILE);
        MEMORY_CONFIG_PATH = Paths.get("../" + CONFIG_DIR + "/" + MEMORY_CONFIG_FILE);
    }

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
        CONFIG_PATH = Paths.get("NotExists");
        assertThrows(IllegalArgumentException.class, ConfigLoader::new);
        CONFIG_PATH = Paths.get("../" + CONFIG_DIR + "/" + CONFIG_FILE);
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
        starter.start();
        starter.setUncaughtExceptionHandler((t, e) ->
                assertThat(e).isInstanceOf(IllegalStateException.class));
        Thread.sleep(100);
        ConfigLoader cl = (ConfigLoader) FieldUtils.readDeclaredStaticField(ConfigLoader.class, "cl", true);
        cl.close();
        Thread.sleep(100);

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
        Thread.sleep(100);

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
}