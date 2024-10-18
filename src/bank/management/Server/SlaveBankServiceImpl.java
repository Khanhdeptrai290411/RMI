package bank.management.Server;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SlaveBankServiceImpl implements BankService {
    
    // Kết nối đến cơ sở dữ liệu con
    private Connection connect() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/bank_con", "root", "12345"); // CSDL con
    }

    @Override
    public double checkBalance(String pin) throws RemoteException {
        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            String query = "SELECT amount FROM bank WHERE pin = '" + pin + "'";
            ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                return rs.getDouble("amount");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Error while checking balance: " + e.getMessage());
        }
        return 0;
    }

    @Override
    public synchronized void deposit(String pin, double amount) throws RemoteException {
        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            String query = "UPDATE bank SET amount = amount + " + amount + " WHERE pin = '" + pin + "'";
            stmt.executeUpdate(query);
            String transactionQuery = "INSERT INTO bank (pin, date, type, amount) VALUES ('" + pin + "', NOW(), 'Deposit', " + amount + ")";
            stmt.executeUpdate(transactionQuery);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("An error occurred during deposit.");
        }
    }

    @Override
    public synchronized void withdraw(String pin, double amount) throws RemoteException {
        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            String balanceQuery = "SELECT amount FROM bank WHERE pin = '" + pin + "'";
            ResultSet rs = stmt.executeQuery(balanceQuery);
            if (rs.next()) {
                double currentBalance = rs.getDouble("amount");
                if (currentBalance < amount) {
                    throw new RemoteException("Insufficient balance.");
                }

                String query = "UPDATE bank SET amount = amount - " + amount + " WHERE pin = '" + pin + "'";
                stmt.executeUpdate(query);
                String transactionQuery = "INSERT INTO bank (pin, date, type, amount) VALUES ('" + pin + "', NOW(), 'Withdrawal', " + amount + ")";
                stmt.executeUpdate(transactionQuery);
            } else {
                throw new RemoteException("Account not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("An error occurred during withdrawal.");
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

            // Đăng ký tài khoản mới
            String query = "INSERT INTO login (formno, card_number, pin) VALUES ('" + formNo + "', '" + cardNumber + "', '" + pin + "')";
            stmt.executeUpdate(query);
            
            // Tạo tài khoản ngân hàng với số dư ban đầu
            String accountQuery = "INSERT INTO bank (pin, amount, date) VALUES ('" + pin + "', 0, NOW())";

            stmt.executeUpdate(accountQuery);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RemoteException("SQL error during sign up: " + e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("An error occurred during sign up: " + e.getMessage(), e);
        }
    }


    @Override
    public void syncTransaction(String transactionType, String pin, double amount) throws RemoteException {
        // Implement synchronization logic if necessary
    }

    @Override
    public boolean validateLogin(String cardNumber, String pin) throws RemoteException {
        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            String query = "SELECT * FROM login WHERE card_number = '" + cardNumber + "' AND pin = '" + pin + "'";
            ResultSet rs = stmt.executeQuery(query);
            return rs.next();  // If found, login is valid
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("An error occurred during login validation.");
        }
    }
}
