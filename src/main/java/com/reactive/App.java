package com.reactive;

import com.reactive.enhanch.SocketProcessorV2;

import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class App {
	public static void main(String[] args) {
		LinkedBlockingQueue<SocketChannel> SOCKET_QUEUE = new LinkedBlockingQueue<>();
		Thread socketAcceptThread = new Thread(new SocketAcceptor(8086, SOCKET_QUEUE));
//		Thread socketProcessThread = new Thread(new SocketProcessor(SOCKET_QUEUE));
		Thread socketProcessThread = new Thread(new SocketProcessorV2(SOCKET_QUEUE));
		socketProcessThread.setName("epoll");
		socketAcceptThread.start();
		socketProcessThread.start();
		System.out.println("on");
	}
}
