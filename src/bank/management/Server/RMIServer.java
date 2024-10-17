package bank.management.Server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIServer {
    public static void main(String[] args) {
        try {
            BankServiceImpl bankService = new BankServiceImpl();
            BankService stub = (BankService) UnicastRemoteObject.exportObject(bankService, 0);

            Registry registry = LocateRegistry.createRegistry(4445);
            registry.rebind("BankService", stub);


            System.out.println("RMI Server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


