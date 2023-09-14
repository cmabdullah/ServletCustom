package com.loommigration;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AcceptRequest extends Thread {

	private final int port;

	public AcceptRequest(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			while (true) {
				try {
					Socket socket = serverSocket.accept();
					App.SOCKET_QUEUE.put(socket);
				} catch (InterruptedException e) {
					System.out.println(e.getLocalizedMessage());
				}
			}
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
		}
	}
}
