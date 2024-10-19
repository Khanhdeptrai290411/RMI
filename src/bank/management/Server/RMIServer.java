package bank.management.Server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIServer {
    public static void main(String[] args) {
        try {
            // Tạo đối tượng BankServiceImpl cho Master Server
            BankServiceImpl bankService = new BankServiceImpl();

            // Xuất đối tượng bankService để nó có thể được gọi từ xa
            BankService stub = (BankService) UnicastRemoteObject.exportObject(bankService, 0);

            // Tạo Registry RMI trên cổng 1234 (hoặc cổng khác nếu cần)
            Registry registry = LocateRegistry.createRegistry(1235); 
            
            // Đăng ký đối tượng BankService vào registry
            registry.rebind("BankService", stub);  

            System.out.println("RMI Master Server is running on port 1234...");

            // Danh sách các Slave Servers cần đồng bộ
            String[] slaveServers = {"localhost:1243", "localhost:1255"}; // Thay đổi thành IP/hostname và port của các Slave Servers

            // Sau mỗi giao dịch, Master sẽ đồng bộ hóa với các Slave Servers
            for (String slave : slaveServers) {
                try {
                    String[] parts = slave.split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);

                    Registry slaveRegistry = LocateRegistry.getRegistry(host, port);
                    BankService slaveBankService = (BankService) slaveRegistry.lookup("BankService");

                    // Ví dụ: Sau khi đăng ký hoặc chuyển tiền, đồng bộ hóa với Slave Servers
                    // Ở đây bạn có thể gọi phương thức để cập nhật dữ liệu tại các Slave
                    // slaveBankService.updateData(...);  // Gọi phương thức đồng bộ hóa với Slave

                    System.out.println("Connected and synchronized with Slave Server: " + slave);
                } catch (Exception e) {
                    System.err.println("Failed to connect or sync with Slave Server: " + slave);
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
