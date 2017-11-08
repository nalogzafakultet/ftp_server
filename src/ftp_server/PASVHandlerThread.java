package ftp_server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class PASVHandlerThread implements Runnable {
	private ServerSocket serverSocket;
	private Socket socket;

	private BufferedReader incomingStream;
	private PrintWriter outgoingStream;
	private SharedModel sharedModel;
	private BufferedOutputStream bos;

	public PASVHandlerThread(ServerSocket serverSocket, SharedModel sharedModel) {
		this.serverSocket = serverSocket;
		this.sharedModel = sharedModel;
		sharedModel.setPASVAlive(true);
	}

	@Override
	public void run() {

		try {
			System.out.println("upalio sam pasv thread na portu " + serverSocket.getLocalPort());
			if (sharedModel.isPASVAlive()) {
				System.out.println("cekam konekciju");
				Socket socket = serverSocket.accept();
				this.socket = socket;
				System.out.println("Passive connection with: " + socket.getInetAddress().getCanonicalHostName()
						+ " on port" + socket.getPort());
				outgoingStream = new PrintWriter(socket.getOutputStream(), true);
				// sendLine("227 " + socket.getPort());
				
				incomingStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
				if (incomingStream == null) {
					System.out.println("PASV Incoming Stream not initialized");
				}

				if (outgoingStream == null) {
					System.out.println("PASV Outgoing Stream not initialized");
				}

				sharedModel.setPASVReady(true);
				System.out.println("Izlazim iz run-a");
			}
		} catch (IOException e) {

			e.printStackTrace();
		}

	}
	
	public void sendFile(String fileName) {
		try {
			File f= new File(fileName);
			InputStream fis = new FileInputStream(f);
			System.out.println("Ima socketa u slanju?: " + (socket == null));
			OutputStream os = socket.getOutputStream();
			
			
			byte[] buf = new byte[(int)f.length()];
	        int len = 0;
	        while ((len = fis.read(buf)) != -1) {
	            os.write(buf, 0, len);
	        }
		
			
			os.close();
			fis.close();
			System.out.println("pozatvarao sve poz");
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void copy(InputStream in, OutputStream out) throws IOException {
		
		byte[] buf = new byte[8192];
        int len = 0;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        
        
    }

	public void close() {

		try {
			serverSocket.close();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void recieveFile(String fileName) {
		try {
			File f= new File(fileName);
			InputStream is = socket.getInputStream();
			System.out.println("Ima socketa u slanju?: " + (socket == null));
			FileOutputStream fos = new FileOutputStream(f);
			
			copy(is, fos);
			
			fos.close();
			is.close();
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean sendLine(String line) {
		outgoingStream.print(line + "\r\n");
		System.out.println("PASV sent " + line);
		outgoingStream.flush();
		return true;

	}

}
