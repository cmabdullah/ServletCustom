package com.reactive;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class Data {
	private final SocketChannel socketChannel;
	private Selector selector;
	private final SelectionKey selectionKey;
	private final StringBuilder stringBuilder;

	String message = "Hello World";

	byte[] byteData = message.getBytes();
	StringBuilder sb = new StringBuilder().append("HTTP/1.1 200 Ok\r\n")
			.append("Content-Type: ")
			.append("text/html\r\n")
			.append("Connection: Closed\r\n")
			.append("Content-Length: ")
			.append(byteData.length)
			.append("\r\n\r\n").append(message);

	private final byte[] responseBytes = sb.toString().getBytes();

	private ByteBuffer responseBuffer = ByteBuffer.wrap(responseBytes);

	public Data(SocketChannel socketChannel,
				Selector selector,
				SelectionKey selectionKey) {
		this.socketChannel = socketChannel;
		this.selector = selector;
		this.selectionKey = selectionKey;
		this.stringBuilder = new StringBuilder();

	}

	public void read() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(8192);
		try {
			int length = socketChannel.read(byteBuffer);
			if (length == -1) {
				closeConnection(socketChannel);
			} else {
				byteBuffer.flip();
				stringBuilder.append(StandardCharsets.UTF_8.decode(byteBuffer));
				int position = stringBuilder.indexOf("\r\n\r\n");

				if (position > 0) {
					selectionKey.interestOps(SelectionKey.OP_WRITE);
				}
			}
		} catch (IOException e) {
			closeConnection(socketChannel);
			e.printStackTrace();
		}
	}

	private void closeConnection(SocketChannel socketChannel) {
		try {
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void write() {

		try {
			socketChannel.write(responseBuffer);
			if (responseBuffer.hasRemaining()) {
				closeConnection(socketChannel);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
