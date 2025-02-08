package devs.lair.ipc.rmi.reverse;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IReverseService extends Remote {
    String reverse(String input) throws RemoteException;
}
