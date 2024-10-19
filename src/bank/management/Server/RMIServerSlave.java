package bank.management.Server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIServerSlave {
    public static void main(String[] args) {
        try {
            // Tạo đối tượng BankServiceImpl cho Slave Server
            BankServiceImpl bankService = new BankServiceImpl();

            // Xuất đối tượng bankService để nó có thể được gọi từ xa
            BankService stub = (BankService) UnicastRemoteObject.exportObject(bankService, 0);

            // Tạo Registry RMI trên cổng 1243 (ví dụ cổng của Slave)
            Registry registry = LocateRegistry.createRegistry(1243); 
            
            // Đăng ký đối tượng BankService vào registry với tên "BankService"
            registry.rebind("BankService", stub);  

            System.out.println("RMI Slave Server is running on port 1243...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
