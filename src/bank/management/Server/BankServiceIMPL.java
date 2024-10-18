package bank.management.Server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

class BankServiceImpl implements BankService {

    // Khóa tài khoản để ngăn chặn nhiều giao dịch đồng thời trên cùng một tài khoản
    private static final HashMap<String, Boolean> accountLocks = new HashMap<>();

    private Connection connect() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/bank", "root", "12345");
    }

    @Override
    public double checkBalance(String pin) throws RemoteException {
        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            String query = "SELECT amount FROM bank WHERE pin = '" + pin + "'";
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getDouble("amount");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during balance check: " + e.getMessage(), e);
        } catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        return 0;
    }

    public boolean validateLogin(String cardNumber, String pin) throws RemoteException {
        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            String query = "SELECT * FROM login WHERE card_number = '" + cardNumber + "' AND pin = '" + pin + "'";
            ResultSet rs = stmt.executeQuery(query);
            return rs.next();  // If a record is found, login is valid
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during login validation: " + e.getMessage(), e);
        } catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return false;
    }

    @Override
    public synchronized void deposit(String pin, double amount) throws RemoteException {
        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            // Thực hiện gửi tiền
            String query = "UPDATE bank SET amount = amount + " + amount + " WHERE pin = '" + pin + "'";
            stmt.executeUpdate(query);
            
            // Ghi nhật ký giao dịch
            String transactionQuery = "INSERT INTO bank (pin, date, type, amount) VALUES ('" + pin + "', NOW(), 'Deposit', " + amount + ")";
            stmt.executeUpdate(transactionQuery);
            
            // Gọi hàm đồng bộ hóa dữ liệu tới các server con
            syncTransaction("Deposit", pin, amount);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during deposit: " + e.getMessage(), e);
        } catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    }

    @Override
    public synchronized void withdraw(String pin, double amount) throws RemoteException {
        // Khóa tài khoản để ngăn chặn xung đột giao dịch
        synchronized (accountLocks) {
            if (accountLocks.getOrDefault(pin, false)) {
                throw new RemoteException("Account is locked due to another transaction.");
            }
            accountLocks.put(pin, true); // Khóa tài khoản
        }

        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            // Kiểm tra số dư trước khi rút
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

                // Gọi hàm đồng bộ hóa dữ liệu tới các server con
                syncTransaction("Withdrawal", pin, amount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during withdrawal: " + e.getMessage(), e);
        } catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
            synchronized (accountLocks) {
                accountLocks.remove(pin); // Mở khóa tài khoản sau giao dịch
            }
        }
    }

    @Override
    public void simpleSignUp(String formNo, String cardNumber, String pin) throws RemoteException {
        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            // Kiểm tra xem tài khoản đã tồn tại chưa
            String checkQuery = "SELECT * FROM login WHERE card_number = '" + cardNumber + "'";
            ResultSet rs = stmt.executeQuery(checkQuery);
            if (rs.next()) {
                throw new RemoteException("Account already exists with this card number.");
            }

            // Đăng ký tài khoản mới trên server hiện tại
            String query = "INSERT INTO login (formno, card_number, pin) VALUES ('" + formNo + "', '" + cardNumber + "', '" + pin + "')";
            stmt.executeUpdate(query);
            
            // Tạo tài khoản ngân hàng với số dư ban đầu
            String accountQuery = "INSERT INTO bank (pin, amount) VALUES ('" + pin + "', 0)";
            stmt.executeUpdate(accountQuery);

            // Đồng bộ hóa với các server con khác
            syncSignUp(formNo, cardNumber, pin);
            
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during sign up: " + e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("An error occurred during sign up: " + e.getMessage(), e);
        }
    }

    private void syncSignUp(String formNo, String cardNumber, String pin) throws RemoteException {
        String[] slaveServers = {"localhost:1243", "localhost:1267"}; // Danh sách các server con

        for (String server : slaveServers) {
            try {
                String[] parts = server.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);

                Registry registry = LocateRegistry.getRegistry(host, port);
                BankService slaveBankService = (BankService) registry.lookup("BankService");

                // Gọi phương thức đăng ký trên các server con khác
                slaveBankService.simpleSignUp(formNo, cardNumber, pin);
                
            } catch (Exception e) {
                System.err.println("Failed to sync sign up with slave server: " + server);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void syncTransaction(String transactionType, String pin, double amount) throws RemoteException {
        String[] slaveServers = {"localhost:1243", "localhost:1267"}; // Danh sách các server con

        for (String server : slaveServers) {
            try {
                String[] parts = server.split(":");
                String host = parts[0];
                int port = Integer.parseInt(parts[1]);
                
                Registry registry = LocateRegistry.getRegistry(host, port);
                BankService slaveBankService = (BankService) registry.lookup("BankService");

                System.out.println("Connected to slave server: " + server);

                // Đồng bộ hóa giao dịch
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
