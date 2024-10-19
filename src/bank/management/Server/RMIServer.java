package bank.management.Server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIServer {
    public static void main(String[] args) {
        try {
            // Tạo đối tượng BankServiceImpl cho Master Server (chỉ để ghi dữ liệu)
            BankServiceImpl bankService = new BankServiceImpl();

            // Xuất đối tượng bankService để nó có thể được gọi từ xa
            BankService stub = (BankService) UnicastRemoteObject.exportObject(bankService, 0);

            // Tạo Registry RMI trên cổng 1236
            Registry registry = LocateRegistry.createRegistry(1236); 
            
            // Đăng ký đối tượng BankService vào registry
            registry.rebind("BankService", stub);  

            System.out.println("RMI Master Server is running on port 1236...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
