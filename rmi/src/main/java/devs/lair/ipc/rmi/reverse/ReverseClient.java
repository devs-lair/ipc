package devs.lair.ipc.rmi.reverse;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ReverseClient {

    public static void main(String[] args) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry();
        IReverseService reverseService =
                (IReverseService) registry.lookup(IReverseService.class.getName());

        String reversed = reverseService.reverse("I am Anton");
        System.out.println(reversed);
    }
}
