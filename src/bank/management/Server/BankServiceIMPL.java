package bank.management.Server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

class BankServiceImpl implements BankService {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean validateLogin(String cardNumber, String pin) throws RemoteException {
        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            String query = "SELECT * FROM login WHERE card_number = '" + cardNumber + "' AND pin = '" + pin + "'";
            ResultSet rs = stmt.executeQuery(query);
            return rs.next();  // If a record is found, login is valid
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;  // Return false if the login is invalid
    }

    @Override
    public void deposit(String pin, double amount) throws RemoteException {
        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            String query = "UPDATE bank SET amount = amount + " + amount + " WHERE pin = '" + pin + "'";
            stmt.executeUpdate(query);
            String transactionQuery = "INSERT INTO bank (pin, date, type, amount) VALUES ('" + pin + "', NOW(), 'Deposit', " + amount + ")";
            stmt.executeUpdate(transactionQuery);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("An error occurred during deposit.");  // Ném ra lỗi nếu có lỗi xảy ra
        }
    }

    @Override
    public void withdraw(String pin, double amount) throws RemoteException {
        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            // Kiểm tra số dư trước khi rút
            String balanceQuery = "SELECT amount FROM bank WHERE pin = '" + pin + "'";
            ResultSet rs = stmt.executeQuery(balanceQuery);
            if (rs.next()) {
                double currentBalance = rs.getDouble("amount");
                if (currentBalance < amount) {
                    throw new RemoteException("Insufficient balance.");  // Ném ra lỗi nếu không đủ số dư
                }
            }

            // Nếu đủ số dư, thực hiện rút tiền
            String query = "UPDATE bank SET amount = amount - " + amount + " WHERE pin = '" + pin + "'";
            stmt.executeUpdate(query);
            String transactionQuery = "INSERT INTO bank (pin, date, type, amount) VALUES ('" + pin + "', NOW(), 'Withdrawal', " + amount + ")";
            stmt.executeUpdate(transactionQuery);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("An error occurred during withdrawal.");  // Ném ra lỗi nếu có lỗi xảy ra
        }
    }

    @Override
    public void simpleSignUp(String formNo, String cardNumber, String pin) throws RemoteException {
        try (Connection con = connect(); Statement stmt = con.createStatement()) {
            // Kiểm tra xem tài khoản đã tồn tại chưa
            String checkQuery = "SELECT * FROM login WHERE card_number = '" + cardNumber + "'";
            ResultSet rs = stmt.executeQuery(checkQuery);
            if (rs.next()) {
                throw new RemoteException("Account already exists with this card number.");  // Ném ra lỗi nếu tài khoản đã tồn tại
            }
            
            String query = "INSERT INTO login (form_no, card_number, pin) VALUES ('" + formNo + "', '" + cardNumber + "', '" + pin + "')";
            stmt.executeUpdate(query);
            
            // Tạo tài khoản ngân hàng với số dư ban đầu
            String accountQuery = "INSERT INTO bank (pin, amount) VALUES ('" + pin + "', 0)";
            stmt.executeUpdate(accountQuery);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("An error occurred during sign up.");  // Ném ra lỗi nếu có lỗi xảy ra
        }
    }
}