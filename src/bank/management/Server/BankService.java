package bank.management.Server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BankService extends Remote {
    // Các phương thức hiện có
    double checkBalance(String pin) throws RemoteException;
    void deposit(String pin, double amount) throws RemoteException;
    void withdraw(String pin, double amount) throws RemoteException;
    void simpleSignUp(String formno, String cardNumber, String pin) throws RemoteException;
    boolean validateLogin(String cardNumber, String pin) throws RemoteException;

    // Thêm phương thức chuyển tiền
    void transfer(String senderPin, String receiverPin, double amount) throws RemoteException;
}


