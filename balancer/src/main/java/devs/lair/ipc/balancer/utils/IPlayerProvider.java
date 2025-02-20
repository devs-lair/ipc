package devs.lair.ipc.balancer.utils;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IPlayerProvider extends Remote {

    String getPlayerName(String arbiterName) throws RemoteException;
}
