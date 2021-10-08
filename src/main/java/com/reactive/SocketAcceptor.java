package com.reactive;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;

public class SocketAcceptor implements Runnable {

	private int port;
	private ServerSocketChannel serverSocketChannel;
	private Queue<SocketChannel> connectionQueue;

	public SocketAcceptor() {

	}

	public SocketAcceptor(int port, Queue<SocketChannel> connectionQueue) {
		this.port = port;
		this.connectionQueue = connectionQueue;
	}

	@Override
	public void run() {

		try {
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.bind(new InetSocketAddress(port));
		} catch (IOException e) {
			e.printStackTrace();
		}

		int count = 0;
		while (true) {
			try {
				SocketChannel socketChannel = serverSocketChannel.accept();
				connectionQueue.add(socketChannel);
				System.out.println("accepted new request "+ ++count);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
