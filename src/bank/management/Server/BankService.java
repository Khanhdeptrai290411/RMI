package bank.management.Server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BankService extends Remote {
    // Existing methods
    double checkBalance(String pin) throws RemoteException;
    void deposit(String pin, double amount) throws RemoteException;
    void withdraw(String pin, double amount) throws RemoteException;
    void simpleSignUp(String formno, String cardNumber, String pin) throws RemoteException;

    // New method for synchronization
    void syncTransaction(String transactionType, String pin, double amount) throws RemoteException;

    // New method for login validation
    boolean validateLogin(String cardNumber, String pin) throws RemoteException;
}

