package bank.management.Server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BankService extends Remote {
    // Method to check balance for a given account pin (read operation, suitable for slave servers)
    double checkBalance(String pin) throws RemoteException;

    // Method to deposit money into an account (write operation, should be handled by the master server)
    void deposit(String pin, double amount) throws RemoteException;

    // Method to withdraw money from an account (write operation, should be handled by the master server)
    void withdraw(String pin, double amount) throws RemoteException;

    // Method to create a new account (write operation, handled by the master server)
    void simpleSignUp(String formno, String cardNumber, String pin) throws RemoteException;

    // Method to validate user login (read operation, suitable for slave servers)
    boolean validateLogin(String cardNumber, String pin) throws RemoteException;
}
