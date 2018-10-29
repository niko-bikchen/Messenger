package client;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.awt.event.*;

public class MessengerClient {
	private static JFrame myMessenger;
	private static JTextField messageInput;
	private static JTextArea messagesDislpay;
	private static BufferedReader reader;
	private static PrintWriter writer;
	private static String login;
	private static String message;
	private static Socket serverSocket;

	public static void main(String[] args) {
		initialize();
	}

	private static void establishConnection() {
		try {
			serverSocket = new Socket("127.0.0.1", 5000);
			reader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
			writer = new PrintWriter(serverSocket.getOutputStream());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void initialize() {

		myMessenger = new JFrame();
		myMessenger.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				try {
					serverSocket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		myMessenger.setTitle("MyMessager v1.0");
		myMessenger.setBounds(100, 100, 650, 400);
		myMessenger.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		myMessenger.getContentPane().setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel();
		myMessenger.getContentPane().add(panel, BorderLayout.SOUTH);

		messageInput = new JTextField();
		messageInput.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				message = messageInput.getText();
				writer.println(login + ": " + message);
				writer.flush();

				messageInput.setText("");
				messageInput.requestFocus();
			}
		});
		messageInput.setColumns(45);
		panel.add(messageInput);

		JButton sendBtn = new JButton("Send");
		sendBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				message = messageInput.getText();
				writer.println(login + ": " + message);
				writer.flush();

				messageInput.setText("");
				messageInput.requestFocus();
			}
		});
		panel.add(sendBtn);

		messagesDislpay = new JTextArea();
		messagesDislpay.setWrapStyleWord(true);
		messagesDislpay.setEditable(false);
		JScrollPane displayScroll = new JScrollPane(messagesDislpay);
		myMessenger.getContentPane().add(displayScroll, BorderLayout.CENTER);

		myMessenger.setVisible(true);

		login = JOptionPane.showInputDialog("Enter your nickname: ");

		establishConnection();

		Thread thread = new Thread(new ServerListener());
		thread.start();

		writer.println("-----" + login + " have connected-----");
		writer.flush();
	}

	private static class ServerListener implements Runnable {
		@Override
		public void run() {
			String message;
			try {
				while ((message = reader.readLine()) != null) {
					messagesDislpay.append(message + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
