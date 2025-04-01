package devs.lair.ipc.jmx;

import devs.lair.ipc.jmx.service.ConfigProvider;
import devs.lair.ipc.jmx.service.interfaces.IPlayerProvider;
import devs.lair.ipc.jmx.utils.Move;
import devs.lair.ipc.jmx.utils.Utils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ArbiterTest {

    @Test
    @DisplayName("Create instance")
    void createInstancePositive() {
        assertDoesNotThrow((() -> {
            Arbiter arbiter = new Arbiter(null);
            arbiter.stop();
        }));
    }

    @Test
    @DisplayName("Test without providers")
    void startWithoutProvider() {
        Arbiter arbiter = spy(Arbiter.class);

        Thread starter = new Thread(arbiter::start);
        starter.start();

        //careful with default
        verify(arbiter, timeout(600).times(1)).playersReady();
        arbiter.stop();
        starter.interrupt();
    }


    @Test
    @DisplayName("Test without providers, and throw")
    void startWithoutProviderTestNotBound() {
        Arbiter arbiter = spy(Arbiter.class);

        Thread starter = new Thread(() -> {
            try (MockedStatic<LocateRegistry> locate = mockStatic(LocateRegistry.class)) {
                Registry registry = mock(Registry.class);
                when(registry.lookup(any())).thenThrow(new NotBoundException());
                locate.when(LocateRegistry::getRegistry).thenReturn(registry);
                arbiter.start();
            } catch (Exception ignored) {}
        });

        starter.start();

        verify(arbiter, timeout(600).times(1)).playersReady();
        arbiter.stop();
        starter.interrupt();
    }

    @Test
    @DisplayName("Test without providers, return wrong object")
    void startWithoutProviderTestWrongObject() throws InterruptedException {
        Arbiter arbiter = spy(Arbiter.class);

        Thread starter = new Thread(() -> {
            try (MockedStatic<LocateRegistry> locate = mockStatic(LocateRegistry.class)) {
                Registry registry = mock(Registry.class);

                when(registry.lookup(any())).thenReturn(new IPlayerProvider() {
                    @Override
                    public String getPlayerName(String arbiterName) throws RemoteException {
                        return "pl";
                    }

                    @Override
                    public void returnPlayer(String player) throws RemoteException {

                    }
                });

                locate.when(LocateRegistry::getRegistry).thenReturn(registry);
                assertThrows(ClassCastException.class, arbiter::start);
            } catch (Exception ignored) {
               }
        });

        starter.start();
        Thread.sleep(500);
    }


    @Test
    @DisplayName("Start with mocks, without players")
    void startWithMocks() throws RemoteException, IllegalAccessException {
        Arbiter arbiter = spy(Arbiter.class);

        ConfigProvider configProvider = getSpyForConfigProvider();
        IPlayerProvider playerProvider = getSpyForPlayerProvider();
        writeMocks(arbiter, configProvider, playerProvider);

        Thread starter = new Thread(arbiter::start);
        starter.start();

        int arbiterTick = configProvider.getArbiterTick();
        verify(playerProvider, timeout(3L * arbiterTick).times(4)).getPlayerName(anyString());
        verify(configProvider, timeout(3L * arbiterTick).times(3)).getArbiterTick();

        arbiter.stop();
        starter.interrupt();
    }

    @Test
    @DisplayName("Start with mocks, players file exists")
    void startWithMocksPlayerFilesExists() throws IOException, IllegalAccessException {
        Arbiter arbiter = new Arbiter(null);

        ConfigProvider configProvider = getSpyForConfigProvider();
        IPlayerProvider playerProvider = getSpyForPlayerProvider();
        writeMocks(arbiter, configProvider, playerProvider);
        int arbiterTick = configProvider.getArbiterTick();
        int maxRound = configProvider.getMaxRound();

        Thread starter = new Thread(() -> {
            try (MockedStatic<Files> files = mockStatic(Files.class)) {
                files.when(() -> Files.size(any(Path.class))).thenReturn(0L);
                files.when(() -> Files.exists(any(Path.class))).thenReturn(true);
                files.when(() -> Files.newByteChannel(any(), any(OpenOption[].class))).thenCallRealMethod();
                arbiter.start();
            }
        });
        starter.start();

        verify(configProvider, timeout((long) (maxRound) * 3 * arbiterTick).times(maxRound)).getArbiterTick();
        arbiter.stop();
        starter.interrupt();
    }

    @Test
    @DisplayName("Start with player answer")
    void playerAnswer() throws Exception {
        Arbiter arbiter = new Arbiter(null);

        ConfigProvider configProvider = getSpyForConfigProvider();
        IPlayerProvider playerProvider = getSpyForPlayerProvider();
        writeMocks(arbiter, configProvider, playerProvider);

        Thread starter = new Thread(() -> {
            try (MockedStatic<Files> files = mockStatic(Files.class)) {
                files.when(() -> Files.newByteChannel(any(), any(OpenOption[].class))).thenCallRealMethod();
                files.when(() -> Files.exists(any(Path.class))).thenReturn(true);
                files.when(() -> Files.size(any(Path.class))).thenReturn(1L);
                files.when(() -> Files.readAllBytes(any())).thenAnswer(invocation -> Move.getRandomMoveBytes());
                arbiter.start();
            }
        });
        starter.start();

        verify(configProvider, timeout(1000).times(25)).getArbiterTick();
        arbiter.stop();
        starter.interrupt();
    }

    @Test
    @DisplayName("Start with player wrong answer")
    void playerAnswerWrong() throws Exception {
        Arbiter arbiter = new Arbiter(null);

        ConfigProvider configProvider = getSpyForConfigProvider();
        IPlayerProvider playerProvider = getSpyForPlayerProvider();
        writeMocks(arbiter, configProvider, playerProvider);

        Thread starter = new Thread(() -> {
            try (MockedStatic<Files> files = mockStatic(Files.class)) {
                files.when(() -> Files.newByteChannel(any(), any(OpenOption[].class))).thenCallRealMethod();
                files.when(() -> Files.exists(any(Path.class))).thenReturn(true);
                files.when(() -> Files.size(any(Path.class))).thenReturn(1L);
                files.when(() -> Files.readAllBytes(any())).thenAnswer(invocation -> "INVALID".getBytes());
                arbiter.start();
            }
        });
        starter.start();

        verify(configProvider, timeout(1000).times(5)).getArbiterTick();
        arbiter.stop();
        starter.interrupt();
    }


    @Test
    @DisplayName("Only one player ready")
    void playerOnlyOneReady() throws Exception {
        Arbiter arbiter = new Arbiter(null);

        ConfigProvider configProvider = getSpyForConfigProvider();
        IPlayerProvider playerProvider = getSpyForPlayerProvider();
        when(playerProvider.getPlayerName(anyString())).thenAnswer(
                invocation -> new Random().nextInt(3) == 0
                        ? Utils.generateUniqueName(null, "player")
                        : null);

        writeMocks(arbiter, configProvider, playerProvider);

        Thread starter = new Thread(() -> {
            try (MockedStatic<Files> files = mockStatic(Files.class)) {
                files.when(() -> Files.newByteChannel(any(), any(OpenOption[].class))).thenCallRealMethod();
                files.when(() -> Files.exists(any(Path.class))).thenReturn(true);
                files.when(() -> Files.size(any(Path.class))).thenReturn(1L);
                files.when(() -> Files.readAllBytes(any())).thenAnswer(invocation -> Move.getRandomMoveBytes());
                arbiter.start();
            }
        });
        starter.start();

        verify(configProvider, timeout(1000).times(25)).getArbiterTick();
        arbiter.stop();
        starter.interrupt();
    }

    @Test
    @DisplayName("Player random fails")
    void playerAnswerWrongRandom() throws Exception {
        Arbiter arbiter = new Arbiter(null);

        ConfigProvider configProvider = getSpyForConfigProvider();
        IPlayerProvider playerProvider = getSpyForPlayerProvider();
        writeMocks(arbiter, configProvider, playerProvider);

        Thread starter = new Thread(() -> {
            try (MockedStatic<Files> files = mockStatic(Files.class)) {
                files.when(() -> Files.newByteChannel(any(), any(OpenOption[].class))).thenCallRealMethod();
                files.when(() -> Files.exists(any(Path.class))).thenReturn(true);
                files.when(() -> Files.size(any(Path.class))).thenReturn(1L);
                files.when(() -> Files.readAllBytes(any())).thenAnswer(invocation ->
                        new Random().nextInt(3) == 0 ? "INVALID".getBytes() : Move.getRandomMoveBytes());
                arbiter.start();
            }
        });
        starter.start();

        verify(configProvider, timeout(1000).times(35)).getArbiterTick();
        arbiter.stop();
        starter.interrupt();
    }


    @Test
    @DisplayName("deletePlayersFiles coverege test")
    void deletePlayersFilesCoverageTest() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Arbiter arbiter = new Arbiter(null);

        MethodUtils.invokeMethod(arbiter, true, "deletePlayersFiles");

        FieldUtils.writeField(arbiter, "players", new String[]{"one", "two"}, true);
        MethodUtils.invokeMethod(arbiter, true, "deletePlayersFiles");
    }

    @Test
    @DisplayName("Exception on clear file, coverage test")
    void clearPlayersFilesTest() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        Arbiter arbiter = new Arbiter(null);

        FieldUtils.writeField(arbiter, "players", new String[]{"one", "two"}, true);

        MockedStatic<Files> files = mockStatic(Files.class);
        files.when(()->Files.write(any(), any(byte[].class))).thenThrow(new NoSuchFileException(null));
        MethodUtils.invokeMethod(arbiter, true, "clearPlayersFiles");
        files.close();
    }

    @Test
    @DisplayName("Start main, coverage test")
    void startMain() throws InterruptedException {
        Thread starter = new Thread(() -> Arbiter.main(new String[0]));
        starter.start();

        starter.interrupt();
        Thread.sleep(100);

        assertThat(starter.isAlive()).isFalse();
    }

    private void writeMocks(Arbiter arbiter, ConfigProvider configProvider, IPlayerProvider playerProvider)
            throws IllegalAccessException {
        FieldUtils.writeField(arbiter, "configProvider", configProvider, true);
        FieldUtils.writeField(arbiter, "playerProvider", playerProvider, true);
    }

    private ConfigProvider getSpyForConfigProvider() {
        ConfigProvider configProvider = spy(ConfigProvider.class);
        when(configProvider.getArbiterTick()).thenReturn(10);
        when(configProvider.getMaxRound()).thenReturn(5);
        return configProvider;
    }

    private IPlayerProvider getSpyForPlayerProvider() throws RemoteException {
        IPlayerProvider playerProvider = spy(IPlayerProvider.class);
        when(playerProvider.getPlayerName(anyString())).thenAnswer(invocation ->
                Utils.generateUniqueName(null, "player"));
        return playerProvider;
    }
}