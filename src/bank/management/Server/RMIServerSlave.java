package bank.management.Server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIServerSlave {
    public static void main(String[] args) {
        try {
            // Tạo đối tượng SlaveBankServiceImpl cho Slave Server
            SlaveBankServiceImpl bankService = new SlaveBankServiceImpl();

            // Xuất đối tượng bankService để nó có thể được gọi từ xa
            BankService stub = (BankService) UnicastRemoteObject.exportObject(bankService, 0);

            // Tạo Registry RMI trên cổng 1243
            Registry registry = LocateRegistry.createRegistry(1243); 
            
            // Đăng ký đối tượng BankService vào registry
            registry.rebind("BankService", stub);  

            System.out.println("RMI Slave Server is running on port 1243...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
