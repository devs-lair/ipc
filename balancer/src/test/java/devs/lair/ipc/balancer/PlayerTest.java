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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PlayerTest {

    @Test
    @DisplayName("Create instance, mock config provider")
    void createInstanceMockConfigProvider() {
        assertDoesNotThrow(() -> {
            Player player = new Player();
            writeMock(player, mockConfigProvider());
            player.stop();
        });
    }

    @Test
    @DisplayName("Create instance, mock config provider")
    void createInstanceMockConfigProviderWithName() {
        assertDoesNotThrow(() -> {
            Player player = new Player("SOME NAME");
            writeMock(player, mockConfigProvider());
            player.stop();
        });
    }

    @Test
    @DisplayName("Without name")
    void throwOnInvalidName() {
        assertThrows(IllegalArgumentException.class, ()->new Player(""));
        assertThrows(IllegalArgumentException.class, ()->new Player(null));
    }

    @Test
    @DisplayName("Already in game")
    void alreadyInGame() throws IOException {
        Path pathFromName = Utils.getPathFromName("max");
        Files.createFile(pathFromName);

        assertThrows(IllegalArgumentException.class, () -> new Player("max"));
        Utils.tryDelete(pathFromName);
    }

    @Test
    @DisplayName("Exception on createFile")
    void exceptionOnFileCreate() {
        MockedStatic<Files> files = mockStatic(Files.class);
        files.when(()->Files.createFile(any())).thenThrow(new IOException());
        assertThrows(IllegalArgumentException.class, Player::new);

        files.close();
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
    void startMain() throws IllegalAccessException, InterruptedException {
        Thread starter = new Thread(()-> Player.main(new String[0]));
        starter.start();

        Thread.sleep(100);

        Player p = (Player) FieldUtils.readDeclaredStaticField(Player.class, "p", true);
        p.stop();
        starter.interrupt();

        Thread.sleep(250);
        assertThat(starter.isAlive()).isFalse();
    }

    @Test
    @DisplayName("isStop = true, just coverage test")
    void isStopTrue() throws IllegalAccessException, InterruptedException {
        Player player = new Player();
        FieldUtils.writeField(player, "isStop", true, true);
        Thread starter = new Thread(player::start);
        starter.start();

        Thread.sleep(10);
        assertThat(starter.isAlive()).isFalse();
        player.stop();
        starter.interrupt();
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