package ftp_server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class ServerMain {


	private ServerSocket serverSocket;
	private final int CONENCTION_PORT = 4000;
	private final String HOSTNAME = "localhost";
	private Socket connectionSocket;
	private HashMap<String, String> usernameDatabase;
	private final String pathToDatabase = System.getProperty("user.dir") + "/src/" 
			+ "usernames.txt";
	private Scanner reader;
	

	public ServerMain() {
		// Inicijalizacija baze podataka korisnika
		if (!initDatabase()) {
			System.out.println("Couldn't initialize database. Aborting program.");
			System.exit(1);
		}



		// Pomocni korisnicki meni
		helpMenu();
		reader = new Scanner(System.in);
		try {
			while (true) {
				int prompt = Integer.parseInt(reader.nextLine());
				if (prompt == 1) {
					initServer();
				}
				if (prompt == 2) {
					addUser();

				}
				if (prompt == 3) {
					System.out.println("Exiting application.");
					System.exit(0);
				}
				helpMenu();
			}
		} catch (NumberFormatException e) {
			System.err.println("Number Required.");
		}

		reader.close();

	}

	private void initServer() {
		try {
			serverSocket = new ServerSocket(CONENCTION_PORT);
		} catch (IOException e) {
			System.out.println("Couldnt initialize server socket. Aborting program.");
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Server up and running on port: " + this.CONENCTION_PORT);

		while (true) {
			try {
				connectionSocket = serverSocket.accept();
			} catch (IOException e) {
				System.out.println("Couldn't connect to connection socket. Aborting program.");
				e.printStackTrace();
				System.exit(1);
			}
			System.out.println("Connection successful: [" + connectionSocket.getInetAddress() + ":"
					+ connectionSocket.getLocalPort() + "]");
			new Thread(new ServerThread(connectionSocket, usernameDatabase)).start();
			System.out.println("Thread initialized.");
		}
	}

	private boolean initDatabase() {
		usernameDatabase = new HashMap<>();

		Scanner sc = null;
		try {
			sc = new Scanner(new File(pathToDatabase));
			while (sc.hasNextLine()) {
				String[] line = sc.nextLine().split(",");
				usernameDatabase.put(line[0], line[1]);
				System.out.println(line[0] + " added to database.");
			}
			sc.close();
			return true;
		} catch (FileNotFoundException e1) {
			System.out.println("Error loading database, exiting application.");
			e1.printStackTrace();
			System.exit(1);
			return false;
		}

	}

	private boolean addUser() {
		System.out.print("Enter username: ");
		String username = reader.next();
		if (usernameDatabase.containsKey(username)) {
			System.err.println("Username already exists in database. Choose another one.");
			reader.nextLine();
			return false;
		} else {
			System.out.print("Enter password: ");
			usernameDatabase.put(username, reader.next());
			reader.nextLine();
			System.out.println("Username " + username + " added to the database.");
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(new File(pathToDatabase));
				for (String u : usernameDatabase.keySet()) {
					pw.println(u + "," + usernameDatabase.get(u));
				}
				pw.close();
				return true;
			} catch (FileNotFoundException e) {
				System.out.println("Could not open the file to write the username. Aborting");
				return false;
			}
		}
	}

	private void helpMenu() {
		System.out.println("1 - Initialize server");
		System.out.println("2 - Add user to database.");
		System.out.println("3 - Exit Application");
	}

	public static void main(String[] args) {
		new ServerMain();
	}

}
