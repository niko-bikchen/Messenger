/**
 * @author Mykola Bikchentaev
 * This file implements server for the messenger 
 */
package server;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class MessengerServer {
	private static HashMap<Integer, PrintWriter> writerStreams;
	private static HashMap<Integer, Socket> sockets;
	private static java.sql.Statement statement;
	private static Connection connection;
	private static int count;

	public static void main(String[] args) {
		initialize();
	}

	private static void initialize() {
		writerStreams = new HashMap<>();
		sockets = new HashMap<>();
		try {
			ServerSocket myServerSocket = new ServerSocket(5000);
			while (true) {
				Socket userSocket = myServerSocket.accept();
				PrintWriter writer = new PrintWriter(userSocket.getOutputStream());
				if (writerStreams.containsKey(count)) {
					while (writerStreams.get(count) != null)
						count++;
				}
				writerStreams.put(count, writer);
				sockets.put(count, userSocket);
				setDataBase();
				sendHistory(writer);
				Thread thread = new Thread(new ServerListener(userSocket, count));
				thread.start();
				System.out.println("Client number " + count + " have connected");
				count++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends the newly connected user all the messages that were sent before he
	 * connected
	 * 
	 * @param writer
	 *            output stream of the newly connected user
	 */
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

	private static class ServerListener implements Runnable {
		BufferedReader reader;
		int innerIndex;

		public ServerListener(Socket socket, int index) {
			innerIndex = index;
			try {
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			String message = "";
			Calendar calendar;

			while (!sockets.get(innerIndex).isClosed()) {
				try {
					message = reader.readLine();
				} catch (IOException e) {
					if (e.getMessage().equals("Connection reset")) {
						System.out.println("Client number " + innerIndex + " disconnected");
						break;
					} else
						e.printStackTrace();
					;
				}
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
			count--;
		}
	}

	/**
	 * This method sends message to all members of the chat
	 * 
	 * @param time
	 *            time when the message was sent
	 * @param message
	 *            message to send
	 */
	private static void sendEveryone(String time, String message) {
		int x = message.indexOf(':');
		String login = message.substring(0, x);
		saveMessage(time, login, message.substring(x + 2, message.length()));
		for (int i = 0; i < writerStreams.size(); i++) {
			writerStreams.get(i).println(time + message);
			writerStreams.get(i).flush();
		}
	}

	/**
	 * This method saves message into the database
	 * 
	 * @param time
	 *            time when the message was sent
	 * @param login
	 *            login of the user who sent the message
	 * @param message
	 *            the message
	 */
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
