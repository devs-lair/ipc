package devs.lair.ipc.rmi.reverse;

import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ReverseService implements IReverseService{

    @Override
    public String reverse(String input)  {
        System.out.println(Thread.currentThread().getName());
        return new StringBuilder(input).reverse().toString();
    }

    public static void main(String[] args) throws RemoteException, AlreadyBoundException, InterruptedException {
        ReverseService service = new ReverseService();
        Remote stub = UnicastRemoteObject.exportObject(service, 0);

        Registry registry = LocateRegistry.createRegistry(1099);
        registry.bind(IReverseService.class.getName(), stub);

        int i = 0;
        while (true) {
            System.out.print(i++ + " ");
            Thread.sleep(100);
        }
    }
}
