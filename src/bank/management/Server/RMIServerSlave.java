package bank.management.Server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIServerSlave {
    public static void main(String[] args) {
        try {
            SlaveBankServiceImpl bankService = new SlaveBankServiceImpl();
            BankService stub = (BankService) UnicastRemoteObject.exportObject(bankService, 0);

            // Tạo RMI Registry trên cổng 1274
            Registry registry = LocateRegistry.createRegistry(1243);
            registry.rebind("BankService", stub);

            System.out.println("RMI Slave Server is running on port 1247...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
