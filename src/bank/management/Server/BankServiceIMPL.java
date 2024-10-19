package bank.management.Server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

class BankServiceImpl implements BankService {

    // Kết nối đến master và slave
    private Connection masterConnection;
    private Connection slaveConnection;

    // Cơ chế khóa cho các giao dịch đồng thời
    private static final HashMap<String, Boolean> accountLocks = new HashMap<>();

    // Thiết lập kết nối cho master (ghi) và slave (đọc)
    public BankServiceImpl() throws RemoteException {
        try {
            // Kết nối đến master để ghi dữ liệu
            masterConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/bank", "root", "");
            
            // Kết nối đến slave để đọc dữ liệu
            slaveConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/bank", "root", "");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Kiểm tra số dư từ cơ sở dữ liệu slave
    @Override
    public double checkBalance(String pin) throws RemoteException {
        try (Connection con = slaveConnection; 
             Statement stmt = con.createStatement()) {
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

    // Xác thực đăng nhập từ cơ sở dữ liệu slave
    @Override
    public boolean validateLogin(String cardNumber, String pin) throws RemoteException {
        try (Connection con = slaveConnection; 
             Statement stmt = con.createStatement()) {
            String query = "SELECT * FROM login WHERE card_number = '" + cardNumber + "' AND pin = '" + pin + "'";
            ResultSet rs = stmt.executeQuery(query);
            return rs.next();  // Nếu tìm thấy bản ghi, đăng nhập hợp lệ
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during login validation: " + e.getMessage(), e);
        }
    }

    // Gửi tiền vào tài khoản (ghi vào master)
    @Override
    public synchronized void deposit(String pin, double amount) throws RemoteException {
        try (Connection con = masterConnection; 
             Statement stmt = con.createStatement()) {
            // Thực hiện gửi tiền
            String query = "UPDATE bank SET amount = amount + " + amount + " WHERE pin = '" + pin + "'";
            stmt.executeUpdate(query);
            
            // Ghi nhật ký giao dịch
            String transactionQuery = "INSERT INTO bank (pin, date, type, amount) VALUES ('" + pin + "', NOW(), 'Deposit', " + amount + ")";
            stmt.executeUpdate(transactionQuery);
            
            // Đồng bộ hóa giao dịch đến các slave
            syncTransaction("Deposit", pin, amount);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during deposit: " + e.getMessage(), e);
        }
    }

    // Rút tiền từ tài khoản (ghi vào master)
    @Override
    public synchronized void withdraw(String pin, double amount) throws RemoteException {
        synchronized (accountLocks) {
            if (accountLocks.getOrDefault(pin, false)) {
                throw new RemoteException("Account is locked due to another transaction.");
            }
            accountLocks.put(pin, true);  // Khóa tài khoản
        }

        try (Connection con = masterConnection; 
             Statement stmt = con.createStatement()) {
            // Kiểm tra số dư hiện tại
            String balanceQuery = "SELECT amount FROM bank WHERE pin = '" + pin + "'";
            ResultSet rs = stmt.executeQuery(balanceQuery);
            if (rs.next()) {
                double currentBalance = rs.getDouble("amount");
                if (currentBalance < amount) {
                    throw new RemoteException("Insufficient balance.");
                }

                // Thực hiện rút tiền
                String query = "UPDATE bank SET amount = amount - " + amount + " WHERE pin = '" + pin + "'";
                stmt.executeUpdate(query);

                // Ghi nhật ký giao dịch
                String transactionQuery = "INSERT INTO bank (pin, date, type, amount) VALUES ('" + pin + "', NOW(), 'Withdrawal', " + amount + ")";
                stmt.executeUpdate(transactionQuery);

                // Đồng bộ hóa giao dịch đến các slave
                syncTransaction("Withdrawal", pin, amount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during withdrawal: " + e.getMessage(), e);
        } finally {
            synchronized (accountLocks) {
                accountLocks.remove(pin);  // Mở khóa tài khoản
            }
        }
    }

    // Đăng ký tài khoản mới (ghi vào master)
    @Override
    public void simpleSignUp(String formNo, String cardNumber, String pin) throws RemoteException {
        try (Connection con = masterConnection; 
             Statement stmt = con.createStatement()) {
            // Kiểm tra nếu tài khoản đã tồn tại
            String checkQuery = "SELECT * FROM login WHERE card_number = '" + cardNumber + "'";
            ResultSet rs = stmt.executeQuery(checkQuery);
            if (rs.next()) {
                throw new RemoteException("Account already exists with this card number.");
            }

            // Đăng ký tài khoản mới
            String query = "INSERT INTO login (formno, card_number, pin) VALUES ('" + formNo + "', '" + cardNumber + "', '" + pin + "')";
            stmt.executeUpdate(query);
            
            // Tạo tài khoản ngân hàng với số dư ban đầu
            String accountQuery = "INSERT INTO bank (pin, amount) VALUES ('" + pin + "', 0)";
            stmt.executeUpdate(accountQuery);

            // Đồng bộ hóa tài khoản mới với các slave
            syncSignUp(formNo, cardNumber, pin);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during sign up: " + e.getMessage(), e);
        }
    }

    // Đồng bộ hóa đăng ký với các slave
    private void syncSignUp(String formNo, String cardNumber, String pin) throws RemoteException {
        String[] slaveServers = {"localhost:1243", "localhost:1267"};  // Danh sách các slave

        for (String server : slaveServers) {
            try {
                String[] parts = server.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);

                Registry registry = LocateRegistry.getRegistry(host, port);
                BankService slaveBankService = (BankService) registry.lookup("BankService");

                // Gọi phương thức đăng ký trên các slave
                slaveBankService.simpleSignUp(formNo, cardNumber, pin);
            } catch (Exception e) {
                System.err.println("Failed to sync sign up with slave server: " + server);
                e.printStackTrace();
            }
        }
    }

    // Đồng bộ hóa giao dịch với các slave
    public void syncTransaction(String transactionType, String pin, double amount) throws RemoteException {
        String[] slaveServers = {"localhost:1243", "localhost:1267"};  // Danh sách các slave

        for (String server : slaveServers) {
            try {
                String[] parts = server.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);

                Registry registry = LocateRegistry.getRegistry(host, port);
                BankService slaveBankService = (BankService) registry.lookup("BankService");

                System.out.println("Connected to slave server: " + server);

                // Đồng bộ hóa giao dịch với các slave
                if (transactionType.equals("Deposit")) {
                    slaveBankService.deposit(pin, amount);	
                } else if (transactionType.equals("Withdrawal")) {
                    slaveBankService.withdraw(pin, amount);
                }
            } catch (Exception e) {
                System.err.println("Failed to connect or sync with slave server: " + server);
                e.printStackTrace();
                throw new RemoteException("Synchronization failed with server: " + server + ", error: " + e.getMessage(), e);
            }
        }
    }
}
