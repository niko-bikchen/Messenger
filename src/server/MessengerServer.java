package server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class MessengerServer {
	private static ArrayList<PrintWriter> writerStreams;
	private static ArrayList<Socket> sockets;
	private static java.sql.Statement statement;
	private static Connection connection;
	private static int count;

	public static void main(String[] args) {
		initialize();
	}

	private static void initialize() {
		writerStreams = new ArrayList<>();
		sockets = new ArrayList<>();
		try {
			ServerSocket myServerSocket = new ServerSocket(5000);
			while (true) {
				Socket userSocket = myServerSocket.accept();
				PrintWriter writer = new PrintWriter(userSocket.getOutputStream());
				writerStreams.add(writer);
				sockets.add(userSocket);
				setDataBase();
				sendHistory(writer);
				Thread thread = new Thread(new ServerListener(userSocket, count));
				thread.start();
				count++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void sendHistory(PrintWriter writer) {
		try {
			ResultSet resultSet = statement.executeQuery("SELECT time, login, message FROM `messenger`.`chatnew`");
			while (resultSet.next()) {
				writer.print(resultSet.getString(1) + resultSet.getString(2) + ": " + resultSet.getString(3) + "\n");
				writer.flush();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static class TestClass{
		public String myS;
		public TestClass(String s) { myS = s; };
		@Override
		public String toString() { return "TestClass = myS:" + myS; };
	}
	
	public static class Wrapper{
		private TestClass myTestClass;
		public Wrapper(TestClass testClass) { myTestClass = testClass; };
		@Override
		public String toString() { return myTestClass.toString(); }; 
	}
	
	private static class ServerListener implements Runnable {
		BufferedReader reader;
		int innerIndex;
		public ServerListener(Socket socket, int index) {
			TestClass toUpdate = new TestClass("abc");
			Wrapper myWrapper = new Wrapper(toUpdate);	
			System.out.println("My Wrapper 1:" + myWrapper);
			toUpdate.myS = "def";
			System.out.println("My Wrapper 2:" + myWrapper);
			
			innerIndex = index;
			try {
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		private static String myS;
		private static void test(String s)
		{
			myS = s;
		}
		

		@Override
		public void run() {
			String message = "";
			Calendar calendar;
			
				while (!sockets.get(innerIndex).isClosed()) {
					System.out.println("Waiting...");
					try {
						message = reader.readLine();
					} catch (IOException e) {
						if(e.getMessage().equals("Connection reset"))
						{
							System.out.println("Client disconnected...");
							break;
						}
						else
							e.printStackTrace();;
					}
					System.out.println("New mesage" + message);
					calendar = Calendar.getInstance();
					if (!(message.startsWith("-----")))
						sendEveryone(calendar.get(Calendar.HOUR_OF_DAY) + ":" + calendar.get(Calendar.MINUTE) + " ",
								message);
					else {
						for (int i = 0; i < writerStreams.size(); i++) {
							writerStreams.get(i).println(message);
							writerStreams.get(i).flush();
						}
					}
				}
				writerStreams.remove(innerIndex);
				sockets.remove(innerIndex);
			
		}
	}

	private static void sendEveryone(String time, String message) {
		int x = message.indexOf(':');
		String login = message.substring(0, x);
		saveMessage(time, login, message.substring(x + 2, message.length()));
		for (int i = 0; i < writerStreams.size(); i++) {
			writerStreams.get(i).println(time + message);
			writerStreams.get(i).flush();
		}
	}

	private static void saveMessage(String time, String login, String message) {
		try {
			statement.executeUpdate("INSERT INTO `messenger`.`chatnew` (`time`, `login`, `message`) VALUES ('" + time
					+ "' ,'" + login + "', '" + message + "');");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static void setDataBase() {
		String url = "jdbc:mysql://localhost:3307/messenger";
		String login = "root";
		String password = "root";
		try {
			connection = DriverManager.getConnection(url, login, password);
			statement = connection.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
