package devs.lair.ipc.balancer;

import devs.lair.ipc.balancer.service.ConfigProvider;
import devs.lair.ipc.balancer.utils.Utils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class PlayerTest {

    @Test
    @DisplayName("Create instance, mock config provider")
    void createInstanceMockConfigProvider() {
        assertDoesNotThrow(() -> {
            Player player = new Player(null);
            writeMock(player, mockConfigProvider());
            player.stop();
        });
    }

    @Test
    @DisplayName("Create instance, mock config provider")
    void createInstanceMockConfigProviderWithName() {
        assertDoesNotThrow(() -> {
            Player player = new Player(null);
            writeMock(player, mockConfigProvider());
            player.stop();
        });
    }

    @Test
    @DisplayName("Already in game")
    void alreadyInGame() throws IOException, IllegalAccessException {
        Player player = new Player(null);
        String name = (String) FieldUtils.readField(player, "name", true);

        Path pathFromName = Utils.getPathFromName(name);
        Files.createFile(pathFromName);

        assertDoesNotThrow(player::start);
        Utils.tryDelete(pathFromName);
    }

    @Test
    @DisplayName("Exception on createFile")
    void exceptionOnFileCreate() {
        Player player = new Player(null);
        MockedStatic<Files> files = mockStatic(Files.class);
        files.when(()->Files.createFile(any())).thenThrow(new IOException());
        assertDoesNotThrow(player::start);

        files.close();
    }

    @Test
    @DisplayName("Delete file")
    void deletePlayerFile() throws IllegalAccessException, InterruptedException {
        Player player = spy(Player.class);
        String name = (String) FieldUtils.readField(player, "name", true);
        Thread starter = new Thread(player::start);
        starter.start();
        Thread.sleep(100);

        Path pathFromName = Utils.getPathFromName(name);
        Utils.tryDelete(pathFromName);
        verify(player, timeout(500).times(1)).stop();
    }

    @Test
    @DisplayName("Start")
    void testOnStart() throws IllegalAccessException {
        Player player = spy(Player.class);
        ConfigProvider configProvider = spy(ConfigProvider.class);
        when(configProvider.getPlayerTick()).thenReturn(10);

        writeMock(player, configProvider);

        MockedStatic<Files> files = mockStatic(Files.class);
        files.when(()->Files.size(any())).thenReturn(1L);

        Thread starter = new Thread(player::start);
        starter.start();

        verify(configProvider, timeout(40).times(3)).getPlayerTick();
        configProvider.close();
        starter.interrupt();
        files.close();
    }

    @Test
    @DisplayName("Start main")
    void startMain() throws  InterruptedException {
        Thread starter = new Thread(()-> Player.main(new String[0]));
        starter.start();

        Thread.sleep(100);

        starter.interrupt();

        Thread.sleep(350);
        assertThat(starter.isAlive()).isFalse();
    }

    private ConfigProvider mockConfigProvider() {
        ConfigProvider mock = mock(ConfigProvider.class);
        when(mock.getPlayerTick()).thenReturn(10);
        return mock;
    }

    private void writeMock(Player player, ConfigProvider configProvider) throws IllegalAccessException {
        FieldUtils.writeField(player, "configProvider", configProvider, true);
    }
}