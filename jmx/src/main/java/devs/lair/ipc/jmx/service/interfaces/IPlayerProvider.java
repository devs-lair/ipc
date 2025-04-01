package devs.lair.ipc.jmx.service.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IPlayerProvider extends Remote {
    String getPlayerName(String arbiterName) throws RemoteException;
    void returnPlayer(String player) throws RemoteException;
}
