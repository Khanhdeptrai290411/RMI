package bank.management.Client;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import bank.management.Server.BankService;

class MainClass extends JFrame implements ActionListener {
    JButton b1, b2, b3, b4, b5, b6, b7;
    String pin;

    MainClass(String pin) {
        this.pin = pin;

        ImageIcon i1 = new ImageIcon(ClassLoader.getSystemResource("icon/atm2.png"));
        Image i2 = i1.getImage().getScaledInstance(1550, 830, Image.SCALE_DEFAULT);
        ImageIcon i3 = new ImageIcon(i2);
        JLabel l3 = new JLabel(i3);
        l3.setBounds(0, 0, 1550, 830);
        add(l3);
        setTitle(this.pin);
        JLabel label = new JLabel("Please Select Your Transaction");
        label.setBounds(430, 180, 700, 35);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("System", Font.BOLD, 28));
        l3.add(label);

        b1 = new JButton("DEPOSIT");
        b1.setForeground(Color.WHITE);
        b1.setBackground(new Color(65, 125, 128));
        b1.setBounds(410, 274, 150, 35);
        b1.addActionListener(this);
        l3.add(b1);

        b2 = new JButton("CASH WITHDRAWAL");
        b2.setForeground(Color.WHITE);
        b2.setBackground(new Color(65, 125, 128));
        b2.setBounds(700, 274, 150, 35);
        b2.addActionListener(this);
        l3.add(b2);
        b4 = new JButton("TRANSFER");
        b4.setForeground(Color.WHITE);
        b4.setBackground(new Color(65, 125, 128));
        b4.setBounds(410, 406, 150, 35);
        b4.addActionListener(this);
        l3.add(b4);


        b5 = new JButton("PIN CHANGE");
        b5.setForeground(Color.WHITE);
        b5.setBackground(new Color(65, 125, 128));
        b5.setBounds(410, 362, 150, 35);
        b5.addActionListener(this);
        l3.add(b5);

        b6 = new JButton("BALANCE ENQUIRY");
        b6.setForeground(Color.WHITE);
        b6.setBackground(new Color(65, 125, 128));
        b6.setBounds(700, 362, 150, 35);
        b6.addActionListener(this);
        l3.add(b6);

        b7 = new JButton("EXIT");
        b7.setForeground(Color.WHITE);
        b7.setBackground(new Color(65, 125, 128));
        b7.setBounds(700, 406, 150, 35);
        b7.addActionListener(this);
        l3.add(b7);

        setLayout(null);
        setSize(1550, 1080);
        setLocation(0, 0);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            // Kết nối đến RMIServerSlave
            Registry slaveRegistry = LocateRegistry.getRegistry("localhost", 1243);  // Kết nối tới Slave Server
            BankService slaveBankService = (BankService) slaveRegistry.lookup("BankService");

            if (e.getSource() == b1) { // Nạp tiền (Ghi qua Slave Server, Slave sẽ gọi tới Master)
                String amountStr = JOptionPane.showInputDialog("Enter amount to deposit:");
                if (amountStr != null && !amountStr.isEmpty()) {
                    double amount = Double.parseDouble(amountStr);
                    if (amount > 0) {
                        // Gửi yêu cầu nạp tiền qua Slave Server (Slave sẽ gọi Master)
                        slaveBankService.deposit(pin, amount);
                        JOptionPane.showMessageDialog(null, "Rs. " + amount + " Deposited Successfully");
                    } else {
                        JOptionPane.showMessageDialog(null, "Invalid amount entered.");
                    }
                }

            } else if (e.getSource() == b2) { // Rút tiền (Ghi qua Slave Server, Slave sẽ gọi tới Master)
                String amountStr = JOptionPane.showInputDialog("Enter amount to withdraw:");
                if (amountStr != null && !amountStr.isEmpty()) {
                    double amount = Double.parseDouble(amountStr);
                    try {
                        // Gửi yêu cầu rút tiền qua Slave Server (Slave sẽ gọi Master)
                        slaveBankService.withdraw(pin, amount);
                        JOptionPane.showMessageDialog(null, "Rs. " + amount + " Withdrawn Successfully");
                    } catch (RemoteException ex) {
                        JOptionPane.showMessageDialog(null, "Not enough money to withdraw");
                    }
                }

            } else if (e.getSource() == b6) { // Kiểm tra số dư (Chỉ đọc - thực hiện qua Slave)
                // Thực hiện kiểm tra số dư qua Slave Server
                double balance = slaveBankService.checkBalance(pin);
                JOptionPane.showMessageDialog(null, "Your Current Balance is Rs. " + balance);

            } else if (e.getSource() == b7) {
                System.exit(0);
            }else if (e.getSource() == b4) { // Chuyển tiền
                String receiverPin = JOptionPane.showInputDialog("Enter receiver PIN:");
                String amountStr = JOptionPane.showInputDialog("Enter amount to transfer:");

                if (receiverPin != null && !receiverPin.isEmpty() && amountStr != null && !amountStr.isEmpty()) {
                    double amount = Double.parseDouble(amountStr);
                    try {
                        // Thực hiện chuyển tiền qua Slave Server (Slave sẽ gọi Master)
                        slaveBankService.transfer(pin, receiverPin, amount);
                        JOptionPane.showMessageDialog(null, "Rs. " + amount + " Transferred Successfully");
                    } catch (RemoteException ex) {
                        JOptionPane.showMessageDialog(null, "Error during transfer: " + ex.getMessage());
                    }
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "An error occurred: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new MainClass("");
    }
}
