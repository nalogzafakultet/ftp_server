package ftp_server;

import java.beans.DesignMode;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class ServerThread implements Runnable {

	private Socket connectionSocket;
	private int DATA_PORT = 20;
	private BufferedReader incomingStream;
	private PrintWriter outgoingStream;
	private HashMap<String, String> usernameDatabase;
	private String rootDirectory = System.getProperty("user.dir") + "/src/";
	private String workingDirectory = rootDirectory;
	private String currentUser;
	private boolean loggedIn = false;
	private boolean binaryFlag = false;
	private InetAddress host;
	private PASVHandlerThread pasvThread;
	private SharedModel sharedModel = new SharedModel();

	private ServerSocket dataSocket;

	public ServerThread(Socket connectionSocket, HashMap<String, String> database) {
		this.connectionSocket = connectionSocket;
		this.usernameDatabase = database;
		this.host = connectionSocket.getInetAddress();
	}

	public void run() {
		try {
			incomingStream = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
			outgoingStream = new PrintWriter(connectionSocket.getOutputStream(), true);

			if (outgoingStream == null) {
				System.out.println("Outgoing Stream not initialized");
			}

			if (incomingStream == null) {
				System.out.println("Incoming Stream not initialized");
			}

			String message;

			// Prvo se salje pozdravna poruka

			message = "220 Welcome to FTP Server!";
			outgoingStream.println(message);
			String[] commandARGS;
			while (true) {
				// Ceka se na komandu klijenta.

				message = incomingStream.readLine();

				if (message != null) {
					System.out.println("Primljena poruka je: " + message);
					commandARGS = message.split(" ");
					switch (commandARGS[0]) {
					case "USER":
						userName(commandARGS); // NOT IMPLEMENTED
						break;
					case "PASS":
						password(commandARGS);
						break;
					case "TYPE":
						type(commandARGS); // IMPLEMENTED
						break;
					case "LIST":
						list(commandARGS); // DELIMICNO
						break;
					case "PORT":
						port(commandARGS); // IMPLEMENTED
						break;
					case "CWD":
						changeDir(commandARGS); // IMPLEMENTED
						break;
					case "NOOP":
						noop(commandARGS); // IMPLEMENTED
						break;
					case "MKD":
						makeDir(commandARGS); // IMPLEMENTED
						break;
					case "RETR":
						retrieve(commandARGS); // IMPLEMENTED
						break;
					case "PASV":
						pasv(commandARGS); // IMPLEMENTED
						break;
					case "STOR":
						store(commandARGS); // IMPLEMENTED
						break;
					case "CDUP":
						changeToParentDir(commandARGS); // IMPLEMENTED
						break;
					case "RMD":
						removeDirectory(commandARGS); // IMPLEMENTED
						break;
					case "DELE":
						deleteFile(commandARGS); // IMPLEMENTED
						break;
					case "PWD":
						printWorkingDirectory(commandARGS); // IMPLEMENTED
						break;
					case "XPWD":
						printWorkingDirectory(commandARGS); // IMPLEMENTED
						break;
					case "HELP":
						help(commandARGS);
						break;
					case "SYST":
						syst(commandARGS);
						break;
					case "FEAT":
						feat(commandARGS);
						break;
					case "MLSD":
						mlsd(commandARGS);
						break;
					default:
						outgoingStream.println("202 Command not implemented, superfluous at this site");
						break;
					}
				}
				// break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void mlsd(String[] commandArgs) {
		File currentDirectory = new File(workingDirectory);
		ArrayList<File> subDirectories = new ArrayList<>();
		ArrayList<File> filesListed = new ArrayList<>();
		File[] allFiles = currentDirectory.listFiles();

		// Razdvajam fileove i direktorijume da bih mozda izlistao prvo
		// direktorijume
		for (File f : allFiles) {
			if (f.isDirectory()) {
				subDirectories.add(f);
			} else if (f.isFile()) {
				filesListed.add(f);
			}
		}

		// TODO: implementirati po onom njivohom standardu, a ako ne, samo ovako
		// lagano
		while (!sharedModel.isPASVReady()) {

		}
		outgoingStream.print("150 Primaj\r\n");
		System.out.println("150 PRIMAJ");
		outgoingStream.flush();
		for (File f : allFiles) {
			if (sharedModel.isPASVAlive() && sharedModel.isPASVReady()) {
				pasvThread.sendLine(MLSDFormat(f));
				System.out.println(MLSDFormat(f));
			}

		}

		pasvThread.close();
		outgoingStream.print("226 MLSD complete." + "\r\n");
		outgoingStream.flush();
		System.out.println("226 MLSD complete.");

	}

	private void syst(String[] commandArgs) {
		String message = "215 UNIX Type: L8";
		outgoingStream.println(message);
		System.out.println(message);

	}

	private void feat(String[] commandArgs) {
		// String message = "500 FEAT not implemented";
		String m1, m2, m3, m4, m5, m6;
		m1 = "211-Extensions supported:";
		m2 = "MLST size*;create;modify*;perm;media-type";
		m3 = "SIZE";
		m4 = "COMPRESSION";
		m5 = "MDTM";
		m6 = "211 END";

		outgoingStream.println(m1);
		System.out.println(m1);
		outgoingStream.println(m2);
		System.out.println(m2);
		outgoingStream.println(m3);
		System.out.println(m3);
		outgoingStream.println(m4);
		System.out.println(m4);
		outgoingStream.println(m4);
		System.out.println(m4);
		outgoingStream.println(m5);
		System.out.println(m5);
		outgoingStream.println(m6);
		System.out.println(m6);

	}

	private void port(String[] commandARGS) {

		if (commandARGS.length != 7) {
			outgoingStream.println("500 Syntax error.");
		} else {
			String ip = commandARGS[1] + "." + commandARGS[2] + "." + commandARGS[3] + "." + commandARGS[4];

			DATA_PORT = (Integer.parseInt(commandARGS[5]) * 256) + Integer.parseInt(commandARGS[6]);
			outgoingStream.println("200 [PORT] Okay");
		}
	}

	private void pasv(String[] commandARGS) throws IOException, InterruptedException {
		sharedModel.setPASVAlive(false);
		sharedModel.setPASVReady(false);
		System.out.println("Host: " + host.toString());
		String hostString = host.toString().replace("/", "");
		System.out.println("host string je " + hostString);

		// InetAddress hostIP = InetAddress.getByName(hostString);
		StringTokenizer tokenizer = new StringTokenizer(hostString, ".");

		if (dataSocket != null) {
			dataSocket.close();
		}

		Random r = new Random();
		int randomPort = r.nextInt(10000) + 30000;
		dataSocket = new ServerSocket(randomPort);

		// InetSocketAddress h = (InetSocketAddress)
		// (dataSocket.getLocalSocketAddress());

		int port0 = randomPort / 256;
		int port1 = randomPort % 256;
		int host0 = (int) Integer.parseInt(tokenizer.nextToken());
		int host1 = (int) Integer.parseInt(tokenizer.nextToken());
		int host2 = (int) Integer.parseInt(tokenizer.nextToken());
		int host3 = (int) Integer.parseInt(tokenizer.nextToken());
		System.out.printf("227 %d,%d,%d,%d,%d,%d\n", host0, host1, host2, host3, port0, port1);
		outgoingStream.printf("227 %d,%d,%d,%d,%d,%d\n", host0, host1, host2, host3, port0, port1);
		new Thread(pasvThread = new PASVHandlerThread(dataSocket, sharedModel)).start();

	}

	private void help(String[] commandARGS) {
		// TODO Auto-generated method stub

	}

	private void printWorkingDirectory(String[] commandARGS) {
		// Testing purposes
		String message;
		message = "257 " + '"' + toLinuxRelativePath(workingDirectory) + '"';
		System.out.println(message);
		outgoingStream.println(message);

	}

	private void deleteFile(String[] commandARGS) {
		String message;
		String dirPath = "";
		String fileToDelete = "";
		int i;
		for (i = 1; i < commandARGS.length - 1; i++) {
			fileToDelete += commandARGS[i];
			fileToDelete += " ";
		}
		if (i == commandARGS.length - 1) {
			fileToDelete += commandARGS[commandARGS.length - 1];
		}
		if (fileToDelete.contains(File.separator) || workingDirectory.charAt(workingDirectory.length() - 1) == '/'
				|| workingDirectory.charAt(workingDirectory.length() - 1) == '\\') { // CEO
																						// PATH
			dirPath = rootDirectory + toWindowsPath(fileToDelete);
		} else {
			dirPath = workingDirectory + File.separator + fileToDelete;
		}
		System.out.println("DIR PATH JE: " + dirPath);
		if (new File(dirPath).exists()) {
			if (new File(dirPath).isFile()) {
				boolean success = new File(dirPath).delete();

				if (success) {
					message = "250 File removed.";
				} else {
					message = "550 File not removed.";
				}
			} else {
				message = "550 " + '"' + fileToDelete + '"' + " is not a file.";
			}
		} else {
			message = "550 File does not exist.";
		}
		outgoingStream.println(message);
		System.out.println(message);
	}

	private void removeDirectory(String[] commandARGS) {
		String message;
		String dirPath = "";
		String fileToDelete = "";
		int i;
		for (i = 1; i < commandARGS.length - 1; i++) {
			fileToDelete += commandARGS[i];
			fileToDelete += " ";
		}
		if (i == commandARGS.length - 1) {
			fileToDelete += commandARGS[commandARGS.length - 1];
		}
		if (fileToDelete.contains(File.separator) || workingDirectory.charAt(workingDirectory.length() - 1) == '/'
				|| workingDirectory.charAt(workingDirectory.length() - 1) == '\\') { // CEO
																						// PATH
			dirPath = rootDirectory + toWindowsPath(fileToDelete);
		} else {
			dirPath = workingDirectory + File.separator + fileToDelete;
		}
		System.out.println("DIR PATH JE: " + dirPath);
		if (new File(dirPath).exists()) {
			if (new File(dirPath).isDirectory()) {
				try {
					FileUtils.deleteDirectory(new File(dirPath));
				} catch (IOException e) {
				}

				if (!new File(dirPath).exists()) { // Uspesno obrisan
					message = "250 Directory removed.";
				} else {
					message = "550 Directory not removed.";
				}
			} else {
				message = "550 " + '"' + fileToDelete + '"' + " is not a directory.";
			}
		} else {
			message = "550 Directory does not exist.";
		}

		outgoingStream.println(message);
		System.out.println(message);
	}

	private void changeToParentDir(String[] commandARGS) {
		String message;
		if (workingDirectory.equals(rootDirectory)) {
			message = "550 " + '"' + "/ has no parent directory." + '"';
		} else {
			workingDirectory = workingDirectory.substring(0, workingDirectory.lastIndexOf(File.separator));
			if (workingDirectory.length() < rootDirectory.length()) {
				workingDirectory = rootDirectory;
			}
			message = "200 OK. Now in " + '"' + toLinuxRelativePath(workingDirectory) + '"';
		}
		outgoingStream.println(message);
		System.out.println(message);
	}

	private void store(String[] commandARGS) throws IOException {
		String fileName = "";
		int i;
		for (i = 1; i < commandARGS.length - 1; i++) {
			fileName += commandARGS[i];
			fileName += " ";
		}
		if (i == commandARGS.length - 1) {
			fileName += commandARGS[commandARGS.length - 1];
		}
		outgoingStream.print("150 Accepting\r\n");
		System.out.println("150 Accepting\r\n");
		String destinationFile = workingDirectory;
		if (destinationFile.charAt(destinationFile.length() - 1) == '/'
				|| destinationFile.charAt(destinationFile.length() - 1) == '\\') {
			destinationFile += File.separator;
		}
		destinationFile += fileName;

		while (!sharedModel.isPASVReady()) {

		}
		System.out.println("Passive je ready.");
		pasvThread.recieveFile(destinationFile);
		System.out.println("Receiving over.");
		outgoingStream.println("250 Everything okay.");
		pasvThread.close();

	}

	private void retrieve(String[] commandARGS) throws IOException {
		if (commandARGS.length != 2) {
			outgoingStream.println("530 Syntax error.");
			return;
		}
		outgoingStream.print("150 Accepting\r\n");
		System.out.println("150 Accepting\r\n");
		String fileWanted = workingDirectory + File.separator + commandARGS[1];

		while (!sharedModel.isPASVReady()) {

		}

		System.out.println("pre nego sto saljem fajl.");
		pasvThread.sendFile(fileWanted);

		outgoingStream.print("226 File transfer okay.\r\n");
		System.out.print("226 File transfer okay.\r\n");
		pasvThread.close();
	}

	private void makeDir(String[] commandARGS) {
		File dir;
		String dirPath;
		String message;
		String fileToMake = "";
		int i;
		for (i = 1; i < commandARGS.length - 1; i++) {
			fileToMake += commandARGS[i];
			fileToMake += " ";
		}
		if (i == commandARGS.length - 1) {
			fileToMake += commandARGS[commandARGS.length - 1];
		}
		if (fileToMake.contains(File.separator) || workingDirectory.charAt(workingDirectory.length() - 1) == '/'
				|| workingDirectory.charAt(workingDirectory.length() - 1) == '\\') { // CEO
																						// PATH
			dirPath = rootDirectory + toWindowsPath(fileToMake);
		} else {
			dirPath = workingDirectory + File.separator + fileToMake;
		}
		System.out.println("DIR PATH JE: " + dirPath);
		dir = new File(dirPath);
		boolean successful = dir.mkdir();
		if (successful) {
			message = "250 " + '"' + toLinuxRelativePath(dir.getPath()) + '"' + " was created successfully";
			System.out.println(message);
		} else {
			message = "550 " + '"' + toLinuxRelativePath(dir.getPath()) + '"' + " wasn't created for some reason";
		}
		outgoingStream.println(message);
		System.out.println(message);
	}

	private void noop(String[] commandARGS) {
		String message = "200 OK.";
		outgoingStream.println(message);
		System.out.println(message);

	}

	private void changeDir(String[] commandARGS) {
		String oldWD = workingDirectory;
		String message;
		if (commandARGS[1].contains("/")) {
			workingDirectory = rootDirectory;
			if (commandARGS[1].length() != 1)
				workingDirectory = workingDirectory + toWindowsPath(commandARGS[1]);
		} else {
			if (workingDirectory.substring(workingDirectory.length() - 1).contains("\\")) {
				workingDirectory = workingDirectory + commandARGS[1];
			} else {
				workingDirectory = workingDirectory + File.separator + commandARGS[1];
			}
		}
		if (new File(workingDirectory).exists()) {
			message = "250 current directory " + '"' + toLinuxRelativePath(workingDirectory) + '"';
		} else {
			message = "550 Directory " + '"' + toLinuxRelativePath(workingDirectory) + '"'
					+ " does not exist. Working Directory still " + toLinuxRelativePath(oldWD);
			workingDirectory = oldWD;
		}
		outgoingStream.println(message);
		System.out.println(message);

	}

	private void list(String[] commandARGS) {
		File currentDirectory = new File(workingDirectory);
		ArrayList<File> subDirectories = new ArrayList<>();
		ArrayList<File> filesListed = new ArrayList<>();
		File[] allFiles = currentDirectory.listFiles();

		// Razdvajam fileove i direktorijume da bih mozda izlistao prvo
		// direktorijume
		for (File f : allFiles) {
			if (f.isDirectory()) {
				subDirectories.add(f);
			} else if (f.isFile()) {
				filesListed.add(f);
			}
		}

		// TODO: implementirati po onom njivohom standardu, a ako ne, samo ovako
		// lagano
		while (!sharedModel.isPASVReady()) {

		}
		outgoingStream.println("150");

		for (File f : allFiles) {
			if (sharedModel.isPASVAlive() && sharedModel.isPASVReady())
				pasvThread.sendLine(LISTformat(f));
		}

		sharedModel.setPASVAlive(false);
		outgoingStream.println("226 List done.");
		outgoingStream.flush();
		System.out.println("226 List done.");

	}

	private String MLSDFormat(File file) {
		String fileType = file.isDirectory() ? "dir" : "file";
		String output = String.format("Type=%s;Size=%d;Modify=%d;Perm=rw; %s", fileType, file.length(),
				file.lastModified(), file.getName());
		return output;
	}

	private String LISTformat(File file) {
		// String ss = String.format("%s 1 %-10s %-10s %10d Jan 1 1980 %s\r\n",
		// "-rw-rw-rw", "root", "root", file.length(), file.getName());
		String permissions = file.isDirectory() ? "d" : "-";
		permissions += "-rw-rw-rw";
		String ss = String.format("%s %3d %-8s %-8s %8d May 26 %s\r\n", permissions, 1, "nemanja", "nemanja",
				file.length(), file.getName());
		return ss;
	}

	private void type(String[] arguments) {
		String message;
		if (arguments[1].toLowerCase().equals("a")) {
			binaryFlag = false;
		} else if (arguments[1].toLowerCase().equals("i")) {
			binaryFlag = true;
		} else if (arguments[1].toLowerCase().equals("l")) {
			binaryFlag = false;
		}

		message = "200 Binary flag is " + ((binaryFlag == true) ? "on" : "off") + ".";
		outgoingStream.println(message);
		System.out.println(message);

	}

	private void password(String[] arguments) {
		String password = arguments[1];
		String message;
		if (!currentUser.isEmpty()) {
			message = "230 Logged in";
			loggedIn = true;
		} else {
			message = "430 Invalid username or password";
		}
		outgoingStream.println(message);
		System.out.println(message);

	}

	private void userName(String[] arguments) {
		String username = arguments[1];
		String message;
		if (usernameDatabase.containsKey(username)) {
			System.out.println("U bazi je.");
			currentUser = username;
			message = "331 OK. Enter password.";

		} else {
			message = "430 Invalid username or password";
		}
		outgoingStream.println(message);
		System.out.println(message);
	}

	private String toWindowsPath(String linuxPath) {
		return linuxPath.replaceAll("\\\\/", "\\");

	}

	private String toLinuxPath(String windowsPath) {
		return windowsPath.replaceAll("\\\\", "/");
	}

	private String toLinuxRelativePath(String windowsPath) {
		String temp = windowsPath.replaceFirst(Pattern.quote(rootDirectory), "");
		if (temp.equals("")) {
			temp = "/";
		}
		return toLinuxPath(temp);
	}
}
