package com.reactive.enhanch;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class ServerRequestProcessor implements Processor {

	SocketChannel socketChannel;
	Selector selector;
	SelectionKey selectionKey;
	private StringBuilder acceptedRequestFromBrowserToServer;
	private StringBuilder webClientResponse;
	private ByteBuffer serverResponseBuffer;
	private String requestId;
	private WebClientRequestProcessor webClientRequestProcessor;

	public ServerRequestProcessor(SocketChannel socketChannel,
								  Selector selector,
								  SelectionKey selectionKey, String requestId) {
		this.socketChannel = socketChannel;
		this.selector = selector;
		this.selectionKey = selectionKey;
		acceptedRequestFromBrowserToServer = new StringBuilder();
		webClientResponse = new StringBuilder();
		this.requestId = requestId;
	}

	public ServerRequestProcessor() {
	}

	@Override
	public void read() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(8192);
		try {
			int length = socketChannel.read(byteBuffer);
			if (length == -1) {
				closeConnection(socketChannel);
			} else {
				byteBuffer.flip();
				acceptedRequestFromBrowserToServer.append(StandardCharsets.UTF_8.decode(byteBuffer));
				int position = acceptedRequestFromBrowserToServer.indexOf("\r\n\r\n");
				String urlPath = "";
				if (position > 0){
					String[] request = acceptedRequestFromBrowserToServer.substring(0,position).split("\r\n");
					String firstLine = request[0];
					String[] firstLineArray = firstLine.split(" ");
					urlPath = firstLineArray[1];
				}

				URL finalUri = new URL("http://localhost:8080"+urlPath);
				System.out.println("webclient calling url "+finalUri.toString() + " Request id : "+requestId);
				selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_READ); // We are already connected: remove interest in CONNECT event
				webClientRequestProcessor = new WebClientRequestProcessor(finalUri,  urlPath, selector,requestId , this);

			}
		} catch (IOException e) {
			closeConnection(socketChannel);
			e.printStackTrace();
		}
	}

	@Override
	public void write() {
		try {
			socketChannel.write(serverResponseBuffer);
			if (serverResponseBuffer.hasRemaining()) {
				closeConnection(socketChannel);
				System.out.println("Connection closed for request ID : "+requestId);
				selectionKey.channel();
			}
		} catch (IOException e) {
			closeConnection(socketChannel);
			e.printStackTrace();
		}
	}

	@Override
	public void connect(SelectionKey selectionKey) {

	}

	private void closeConnection(SocketChannel socketChannel) {
		try {
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void updateResponse(String response) {
		webClientResponse.append(response).append("<br/>");
		changeStateToWritable();
	}

	private void changeStateToWritable() {

		serverResponseBuffer = ByteBuffer.wrap(
				prepareHeader(webClientResponse.length())
						.append(webClientResponse.toString()).toString().getBytes());

		selectionKey.interestOps(SelectionKey.OP_WRITE);
	}

	private StringBuilder prepareHeader(int dataLength) {

		return new StringBuilder().append("HTTP/1.1 200 Ok\r\n")
				.append("Content-Type: ")
				.append("text/html\r\n")
				.append("Connection: Closed\r\n")
				.append("Content-Length: ")
				.append(dataLength)
				.append("\r\n\r\n");
	}

	@Override
	public String getRequestId() {
		return requestId;
	}
}
