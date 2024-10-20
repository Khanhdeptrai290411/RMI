package bank.management.Server;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

class BankServiceImpl implements BankService {

    // Kết nối chỉ đến master (ghi)
    private Connection masterConnection;

    // Cơ chế khóa cho các giao dịch đồng thời
    private static final HashMap<String, Boolean> accountLocks = new HashMap<>();

    // Thiết lập kết nối cho master (ghi)
    public BankServiceImpl() throws RemoteException {
        establishConnection();  // Tạo kết nối khi khởi tạo
    }

    // Phương thức thiết lập kết nối
    private void establishConnection() throws RemoteException {
        try {
            if (masterConnection == null || masterConnection.isClosed()) {
                masterConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/bank", "root", "");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("Failed to establish connection to the database: " + e.getMessage());
        }
    }

    // Gửi tiền vào tài khoản (ghi vào master)
    @Override
    public synchronized void deposit(String pin, double amount) throws RemoteException {
        try {
            establishConnection();  // Đảm bảo kết nối đã được thiết lập
            try (Statement stmt = masterConnection.createStatement()) {
                // Thực hiện gửi tiền
                String query = "UPDATE bank SET amount = amount + " + amount + " WHERE pin = '" + pin + "'";
                stmt.executeUpdate(query);

                // Ghi nhật ký giao dịch
                String transactionQuery = "INSERT INTO bank (pin, date, type, amount) VALUES ('" + pin + "', NOW(), 'Deposit', " + amount + ")";
                stmt.executeUpdate(transactionQuery);
            }
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

        try {
            establishConnection();  // Đảm bảo kết nối đã được thiết lập
            try (Statement stmt = masterConnection.createStatement()) {
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
                }
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
        try {
            establishConnection();  // Đảm bảo kết nối đã được thiết lập
            try (Statement stmt = masterConnection.createStatement()) {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during sign up: " + e.getMessage(), e);
        }
    }
    @Override
    public synchronized void transfer(String senderPin, String receiverPin, double amount) throws RemoteException {
        try (Connection con = masterConnection;
             Statement stmt = con.createStatement()) {

            // Kiểm tra số dư của người gửi
            String balanceQuery = "SELECT amount FROM bank WHERE pin = '" + senderPin + "'";
            ResultSet rs = stmt.executeQuery(balanceQuery);
            if (rs.next()) {
                double currentBalance = rs.getDouble("amount");
                if (currentBalance < amount) {
                    throw new RemoteException("Insufficient balance.");
                }

                // Trừ tiền người gửi
                String withdrawQuery = "UPDATE bank SET amount = amount - " + amount + " WHERE pin = '" + senderPin + "'";
                stmt.executeUpdate(withdrawQuery);

                // Ghi nhật ký giao dịch cho người gửi
                String senderLog = "INSERT INTO bank (pin, date, type, amount) VALUES ('" + senderPin + "', NOW(), 'Transport', " + amount + ")";
                stmt.executeUpdate(senderLog);

                // Cộng tiền cho người nhận
                String depositQuery = "UPDATE bank SET amount = amount + " + amount + " WHERE pin = '" + receiverPin + "'";
                stmt.executeUpdate(depositQuery);

                // Ghi nhật ký giao dịch cho người nhận
                String receiverLog = "INSERT INTO bank (pin, date, type, amount) VALUES ('" + receiverPin + "', NOW(), 'Received', " + amount + ")";
                stmt.executeUpdate(receiverLog);

                // Ghi giao dịch vào bảng transport
                String transportQuery = "INSERT INTO transport (sender_pin, receiver_pin, amount) VALUES ('" + senderPin + "', '" + receiverPin + "', " + amount + ")";
                stmt.executeUpdate(transportQuery);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during transfer: " + e.getMessage(), e);
        }
    }

}


