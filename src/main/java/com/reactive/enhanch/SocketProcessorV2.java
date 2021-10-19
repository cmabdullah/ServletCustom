package com.reactive.enhanch;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public class SocketProcessorV2 implements Runnable {

	private int port;
	private ServerSocketChannel serverSocketChannel;
	private Queue<SocketChannel> connectionQueue;
	private Selector selector;

	public SocketProcessorV2(Queue<SocketChannel> connectionQueue) {
		try {
			this.selector = Selector.open();
			this.connectionQueue = connectionQueue;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		while (true) {
			try {
				SocketChannel socketChannel = connectionQueue.poll();
				if (Objects.nonNull(socketChannel)) {
					socketChannel.configureBlocking(false);
					SelectionKey requestSelectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
					String requestId = String.valueOf(Math.random()).substring(2,7);
					Processor data = new ServerRequestProcessor(socketChannel, selector, requestSelectionKey, requestId);
					requestSelectionKey.attach(data);
				}

				int availableChannelForProcess = selector.selectNow();
				if (availableChannelForProcess == 0) {
					continue;
				}

//				if (selector.select(100) == 0) // Did something happen on some registered Channels during the last 100ms?
//					continue; // No, wait some more

				Set<SelectionKey> selectionKeys = selector.selectedKeys();

				selectionKeys.forEach(selectionKey -> {
					if (selectionKey.isValid()) {
						int interest = selectionKey.interestOps();
						Processor processor = (Processor) selectionKey.attachment();
						String requestIde = processor.getRequestId();
						System.out.println("requestIde "+requestIde +" is entering mode -> " +interest + " with class "+processor.getClass());
						switch (interest) {
							case SelectionKey.OP_READ:
								processor.read();//1
								break;
							case SelectionKey.OP_WRITE:
								processor.write();//4
								break;
							case SelectionKey.OP_CONNECT:
								processor.connect(selectionKey);//8
								break;
							default:
								break;
						}
					}
				});

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
