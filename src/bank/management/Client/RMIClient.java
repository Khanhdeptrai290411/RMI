package bank.management.Client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import bank.management.Server.BankService;

public class RMIClient extends JFrame implements ActionListener {
    static String pin;

    public RMIClient() {
        setTitle("BANK MANAGEMENT SYSTEM");

        // Tạo giao diện đăng nhập
        JLabel l1 = new JLabel("WELCOME TO BANK");
        l1.setFont(new Font("Osward", Font.BOLD, 38));

        JLabel l2 = new JLabel("Card No:");
        l2.setFont(new Font("Raleway", Font.BOLD, 28));

        JLabel l3 = new JLabel("PIN:");
        l3.setFont(new Font("Raleway", Font.BOLD, 28));

        JTextField tf1 = new JTextField(15);
        JPasswordField pf2 = new JPasswordField(15);

        JButton b1 = new JButton("SIGN IN");
        JButton b2 = new JButton("CLEAR");
        JButton b3 = new JButton("SIGN UP");

        // Set layout and positions
        setLayout(null);
        l1.setBounds(175, 50, 450, 200);
        l2.setBounds(125, 150, 375, 200);
        l3.setBounds(125, 225, 375, 200);
        tf1.setBounds(300, 235, 230, 30);
        pf2.setBounds(300, 310, 230, 30);
        b1.setBounds(300, 400, 100, 30);
        b2.setBounds(430, 400, 100, 30);
        b3.setBounds(300, 450, 230, 30);

        // Add components to the frame
        add(l1);
        add(l2);
        add(l3);
        add(tf1);
        add(pf2);
        add(b1);
        add(b2);
        add(b3);

        // Action for sign in button
        b1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    // Kết nối tới RMI server
                    Registry registry = LocateRegistry.getRegistry("localhost", 1243);
                    BankService bankService = (BankService) registry.lookup("BankService");

                    // Lấy thông tin đăng nhập
                    String cardNumber = tf1.getText();
                    pin = new String(pf2.getPassword());  // Lưu pin vào biến toàn cục

                    // Kiểm tra đăng nhập
                    boolean isValid = bankService.validateLogin(cardNumber, pin);
                    if (isValid) {
                        // Nếu đăng nhập thành công, chuyển sang MainClass
                        new MainClass(pin).setVisible(true);  // Pass the pin to MainClass
                        setVisible(false);  // Ẩn màn hình đăng nhập
                    } else {
                        JOptionPane.showMessageDialog(null, "Invalid Card Number or PIN");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // Hành động cho nút Clear
        b2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                tf1.setText("");
                pf2.setText("");
            }
        });

        // Hành động cho nút Sign Up
        b3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                signupForm();  // Gọi hàm để hiển thị giao diện đăng ký
            }
        });

        // Thiết lập kích thước, vị trí và hiển thị
        setSize(750, 750);
        setLocation(500, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // Hàm tạo giao diện đăng ký
    private void signupForm() {
        JFrame signupFrame = new JFrame("SIGN UP");
        signupFrame.setSize(400, 600);
        signupFrame.setLayout(null);

        JLabel l1 = new JLabel("Sign Up");
        l1.setFont(new Font("Osward", Font.BOLD, 38));
        l1.setBounds(125, 20, 200, 50);
        signupFrame.add(l1);

        JLabel l2 = new JLabel("Form No:");
        l2.setBounds(50, 100, 100, 30);
        JTextField tfFormNo = new JTextField();
        tfFormNo.setBounds(150, 100, 200, 30);
        signupFrame.add(l2);
        signupFrame.add(tfFormNo);

        JLabel l3 = new JLabel("Card Number:");
        l3.setBounds(50, 150, 100, 30);
        JTextField tfCardNumber = new JTextField();
        tfCardNumber.setBounds(150, 150, 200, 30);
        signupFrame.add(l3);
        signupFrame.add(tfCardNumber);

        JLabel l4 = new JLabel("PIN:");
        l4.setBounds(50, 200, 100, 30);
        JPasswordField pfPin = new JPasswordField();
        pfPin.setBounds(150, 200, 200, 30);
        signupFrame.add(l4);
        signupFrame.add(pfPin);

        JButton b1 = new JButton("SUBMIT");
        b1.setBounds(50, 300, 100, 30);
        signupFrame.add(b1);

        // Hành động cho nút SUBMIT
        b1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String formNo = tfFormNo.getText();
                String cardNumber = tfCardNumber.getText();
                String pin = new String(pfPin.getPassword());

                // Log the data being sent
                System.out.println("Attempting to sign up with FormNo: " + formNo + ", CardNumber: " + cardNumber + ", PIN: " + pin);

                // Kết nối tới RMI server
                try {
                    Registry registry = LocateRegistry.getRegistry("localhost", 1243);
                    BankService bankService = (BankService) registry.lookup("BankService");

                    // Gọi phương thức signup
                    bankService.simpleSignUp(formNo, cardNumber, pin);
                    JOptionPane.showMessageDialog(signupFrame, "Sign Up Successful!");

                    signupFrame.setVisible(false);  // Đóng khung đăng ký
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(signupFrame, "Sign Up Failed: " + e.getMessage());
                }
            }
        });


        // Hiển thị khung đăng ký
        signupFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        signupFrame.setLocation(500, 200);
        signupFrame.setVisible(true);
    }

    public static void main(String[] args) {
        new RMIClient();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Không cần thực hiện hành động nào ở đây vì đã có action listener riêng cho các nút
    }
}
