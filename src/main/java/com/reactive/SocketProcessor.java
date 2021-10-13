package com.reactive;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public class SocketProcessor implements Runnable {

	private int port;
	private ServerSocketChannel serverSocketChannel;
	private Queue<SocketChannel> connectionQueue;
	private Selector selector;

	public SocketProcessor(Queue<SocketChannel> connectionQueue) {
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
					Data data = new Data(socketChannel, selector, requestSelectionKey);
					requestSelectionKey.attach(data);
				}

//				int availableChannelForProcess = selector.selectNow();
//				if (availableChannelForProcess == 0) {
//					continue;
//				}

				if (selector.select(100) == 0) // Did something happen on some registered Channels during the last 100ms?
					continue; // No, wait some more

				Set<SelectionKey> selectionKeys = selector.selectedKeys();

				selectionKeys.forEach(selectionKey -> {
					if (selectionKey.isValid()) {
						int interest = selectionKey.interestOps();
						Data data = (Data) selectionKey.attachment();
						switch (interest) {
							case SelectionKey.OP_READ:
//								data.read();
//								data.readV2();
								data.readV3(selectionKey);
								break;
							case SelectionKey.OP_WRITE:
//								data.write();
//								data.writeV2();
								data.writeV3(selectionKey);
								break;
								case SelectionKey.OP_CONNECT:
									data.connect(selectionKey);
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
