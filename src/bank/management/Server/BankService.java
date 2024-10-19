package bank.management.Server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BankService extends Remote {
    // Kiểm tra số dư (chỉ đọc, thực hiện qua Slave)
    double checkBalance(String pin) throws RemoteException;

    // Gửi tiền (ghi dữ liệu vào Master)
    void deposit(String pin, double amount) throws RemoteException;

    // Rút tiền (ghi dữ liệu vào Master)
    void withdraw(String pin, double amount) throws RemoteException;

    // Đăng ký tài khoản mới (ghi dữ liệu vào Master)
    void simpleSignUp(String formno, String cardNumber, String pin) throws RemoteException;

    // Xác thực đăng nhập (chỉ đọc, thực hiện qua Slave)
    boolean validateLogin(String cardNumber, String pin) throws RemoteException;
}
