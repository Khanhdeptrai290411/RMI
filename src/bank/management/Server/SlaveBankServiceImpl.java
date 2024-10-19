package bank.management.Server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class SlaveBankServiceImpl implements BankService {
    private Connection slaveConnection;

    // Địa chỉ và cổng của Master Server
    private final String masterHost = "localhost";
    private final int masterPort = 1236; // Thay đổi port này nếu cần

    public SlaveBankServiceImpl() throws RemoteException {
        try {
            // Kết nối đến DatabaseSlave
            slaveConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/bank", "root", "");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Kiểm tra số dư từ DatabaseSlave
    @Override
    public double checkBalance(String pin) throws RemoteException {
        try (Statement stmt = slaveConnection.createStatement()) {
            String query = "SELECT amount FROM bank WHERE pin = '" + pin + "'";
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getDouble("amount");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during balance check: " + e.getMessage(), e);
        }
        return 0;
    }

    // Gửi tiền: Gọi đến Master Server để xử lý
    @Override
    public synchronized void deposit(String pin, double amount) throws RemoteException {
        try {
            // Kết nối tới Master Server qua RMI
            Registry registry = LocateRegistry.getRegistry(masterHost, masterPort);
            BankService masterService = (BankService) registry.lookup("BankService");

            // Gọi phương thức gửi tiền trên Master Server
            masterService.deposit(pin, amount);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Error during deposit: " + e.getMessage(), e);
        }
    }

    // Rút tiền: Gọi đến Master Server để xử lý
    @Override
    public synchronized void withdraw(String pin, double amount) throws RemoteException {
        try {
            // Kết nối tới Master Server qua RMI
            Registry registry = LocateRegistry.getRegistry(masterHost, masterPort);
            BankService masterService = (BankService) registry.lookup("BankService");

            // Gọi phương thức rút tiền trên Master Server
            masterService.withdraw(pin, amount);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Error during withdrawal: " + e.getMessage(), e);
        }
    }

    // Đăng ký tài khoản mới: Gọi đến Master Server để xử lý
    @Override
    public void simpleSignUp(String formNo, String cardNumber, String pin) throws RemoteException {
        try {
            // Kết nối tới Master Server qua RMI
            Registry registry = LocateRegistry.getRegistry(masterHost, masterPort);
            BankService masterService = (BankService) registry.lookup("BankService");

            // Gọi phương thức đăng ký trên Master Server
            masterService.simpleSignUp(formNo, cardNumber, pin);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Error during sign up: " + e.getMessage(), e);
        }
    }

    // Xác thực đăng nhập từ DatabaseSlave
    @Override
    public boolean validateLogin(String cardNumber, String pin) throws RemoteException {
        try (Statement stmt = slaveConnection.createStatement()) {
            String query = "SELECT * FROM login WHERE card_number = '" + cardNumber + "' AND pin = '" + pin + "'";
            ResultSet rs = stmt.executeQuery(query);
            return rs.next();  // Nếu tìm thấy, đăng nhập hợp lệ
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during login validation: " + e.getMessage(), e);
        }
    }

    // Không thực hiện đồng bộ giao dịch trên Slave Server
    public void syncTransaction(String transactionType, String pin, double amount) throws RemoteException {
        throw new RemoteException("This operation should be handled by the Master Server.");
    }
}
