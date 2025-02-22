package devs.lair.ipc.balancer;

import devs.lair.ipc.balancer.utils.DirWatcher;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

import static devs.lair.ipc.balancer.utils.Constants.*;
import static devs.lair.ipc.balancer.utils.Utils.tryDelete;
import static java.nio.file.StandardOpenOption.*;

public class ConfigLoader implements AutoCloseable {
    private static ConfigLoader cl; //only for tests

    private MappedByteBuffer memory;
    private DirWatcher watcher;
    private byte version = 0;

    public ConfigLoader() {
        if (!Files.exists(CONFIG_PATH)) {
            throw new IllegalArgumentException("Нет файла конфигурации: "
                    + CONFIG_PATH.toAbsolutePath());
        }
    }

    public void init() {
        createMemoryBuffer();
        loadFileToMemory();
        watchByConfigFile();

        watcher.join();
    }

    private void createMemoryBuffer() {
        try (FileChannel fc = (FileChannel) Files.newByteChannel(MEMORY_CONFIG_PATH, CREATE, WRITE, READ)) {
            memory = fc.map(FileChannel.MapMode.READ_WRITE, 0, MEMORY_SIZE);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось создать область в памяти " + e.getMessage());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
    }

    public void loadFileToMemory() {
        if (memory == null) {
            createMemoryBuffer();
        }

        try {
            memory.put((byte) -1); // block read
            Thread.sleep(WAIT_OTHERS_TIMEOUT); // wait others

            memory.clear();
            memory.put(1, readConfigFile());
            memory.put(0, ++version);
        } catch (IOException | BufferOverflowException e) {
            throw new IllegalStateException("Ну удалось записать файл в память " + e.getMessage());
        } catch (InterruptedException ignored) {
            throw new IllegalStateException("Основной поток был прерван");
        }
    }

    private byte[] readConfigFile() throws IOException {
        byte[] bytes = Files.readAllBytes(CONFIG_PATH);
        if (bytes.length == 0 || bytes.length > MEMORY_SIZE) {
            throw new IOException("Файл конфига не помещается в памяти");
        }
        return bytes;
    }

    private void watchByConfigFile() {
        watcher = new DirWatcher(CONFIG_PATH.getParent().toString());
        watcher.addOnModifyListener(this::onModify);
        watcher.startWatch();
    }

    private void onModify(WatchEvent<Path> event) {
        if (event.context().getFileName().toString().equals(CONFIG_FILE)) {
            loadFileToMemory();
        }
    }

    public void close() {
        if (!tryDelete(MEMORY_CONFIG_PATH)) {
            System.out.println("Ну удалось удалить файл памяти");
        }

        if (watcher != null) {
            watcher.close();
        }
    }

    public static void main(String[] args) {
        cl = new ConfigLoader();
        cl.init();
        cl.close();
    }
}
