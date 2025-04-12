package devs.lair.ipc.jmx.service.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IPlayerProvider extends Remote {
    String getPlayerName(String arbiterName, int position) throws RemoteException;
    void returnPlayer(String arbiterName, String player) throws RemoteException;
    void killZombie(String arbiterName, String player) throws RemoteException;
    void finishPlayer(String arbiterName, String[] players) throws RemoteException;
}
