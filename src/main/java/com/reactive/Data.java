package com.reactive;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class Data {
	private final SocketChannel socketChannel;
	private Selector selector;
	private final SelectionKey selectionKey;
	private StringBuilder stringBuilder;
	private String clientResponseString;
	String uri = "";
	SelectionKey clientKey;
	SocketChannel clientSocketChannel;

	private ByteBuffer responseBuffer = prepareDummyByteBuffer("Hello World");
	private ByteBuffer clientRequestWriteBuff;

	public Data(SocketChannel socketChannel,
				Selector selector,
				SelectionKey selectionKey) {
		this.socketChannel = socketChannel;
		this.selector = selector;
		this.selectionKey = selectionKey;
		this.stringBuilder = new StringBuilder();
		this.clientResponseString = new String();

		byte[] message = new String("GET / HTTP/1.0\r\n\r\n").getBytes();
		this.clientRequestWriteBuff = ByteBuffer.wrap(message);
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


	public void readV2() {//http://localhost:8086/add?a1=cm&a2=khan
		ByteBuffer byteBuffer = ByteBuffer.allocate(8192);
		try {
			int length = socketChannel.read(byteBuffer);
			if (length == -1) {
				closeConnection(socketChannel);
			} else {
				byteBuffer.flip();
				stringBuilder.append(StandardCharsets.UTF_8.decode(byteBuffer));
				int position = stringBuilder.indexOf("\r\n\r\n");
				String[] request = stringBuilder.substring(0, position).split("\r\n");
				String firstLine = request[0];
				String[] firstLineArray = firstLine.split(" ");
				uri = firstLineArray[1];

				//controller
				// how this implementation can be done in non-blocking way?
				responseBuffer = prepareResponseByteBuffer();

				if (position > 0) {
					selectionKey.interestOps(SelectionKey.OP_WRITE);
				}
			}
		} catch (IOException e) {
			closeConnection(socketChannel);
			e.printStackTrace();
		}
	}

	public void readV3(SelectionKey selectionKey1) {//http://localhost:8086/add?a1=cm&a2=khan

		if (selectionKey1.equals(clientKey)){
			try {
				ByteBuffer readBuff = MappedByteBuffer.allocate(1500);
//				String res = "";
//				while (clientSocketChannel.read(readBuff) != -1) {
//					String temp = new String(readBuff.array()).trim();
//					res = res + temp;
//					readBuff.clear();
//				}
//				String[] response = res.split("\r\n");
//				System.out.println("The result is : " + response[6]);


				int length = clientSocketChannel.read(readBuff);
				if (length == -1) {

					//closeConnection(clientSocketChannel);
				} else {
					readBuff.flip();
					stringBuilder.append(StandardCharsets.UTF_8.decode(readBuff));
				}
				String str = stringBuilder.toString();
				String[] response = str.split("\r\n");

				System.out.println("The result is : " + response[response.length-1]);

				clientResponseString = response[response.length-1];

				responseBuffer = prepareDummyByteBuffer(clientResponseString);



				System.out.println("response received");
				selectionKey.interestOps(SelectionKey.OP_WRITE);
				System.out.println("selection key in write mode");



//				gatewayResponseWriteBuff = ByteBuffer.wrap(resultString.getBytes());
			} catch (IOException e) {
				//close connection
				closeConnection(clientSocketChannel);
				e.printStackTrace();
			}
//
//			//close connection
			closeConnection(clientSocketChannel);

		} else if (clientKey == null)

		{

			ByteBuffer byteBuffer = ByteBuffer.allocate(8192);
			try {
				int length = socketChannel.read(byteBuffer);
				if (length == -1) {
					closeConnection(socketChannel);
				} else {
					byteBuffer.flip();
					stringBuilder.append(StandardCharsets.UTF_8.decode(byteBuffer));
					int position = stringBuilder.indexOf("\r\n\r\n");
					String[] request = stringBuilder.substring(0, position).split("\r\n");
					String firstLine = request[0];
					String[] firstLineArray = firstLine.split(" ");
					uri = firstLineArray[1];

					//controller
					// how this implementation can be done in non-blocking way?
					//responseBuffer = prepareResponseByteBuffer();

					SocketAddress inetSocketAddress = new InetSocketAddress("localhost", 8080);

//				SocketAddress socketAddress = new InetSocketAddress("localhost", 8080);
					clientSocketChannel = SocketChannel.open();
					clientSocketChannel.configureBlocking(false);

					this.clientKey = clientSocketChannel.register(selector, SelectionKey.OP_CONNECT);
					clientKey.attach(this);
					System.out.println("Initiating connection");

					if (clientSocketChannel.connect(inetSocketAddress))
						System.out.println("Connected"); // Connected right-away: nothing else to do
					else
						System.out.println("connecting...");

//				if (position > 0) {
//					selectionKey.interestOps(SelectionKey.OP_WRITE);
//				}
//				System.out.println("connecting");
				}
			} catch (IOException e) {
				closeConnection(socketChannel);
				e.printStackTrace();
			}

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

	public void writeV3(SelectionKey selectionKey1) {

		if (selectionKey1.equals(clientKey)) {

			try {
				clientSocketChannel.write(clientRequestWriteBuff);
				System.out.println("write request header");

				if (!clientRequestWriteBuff.hasRemaining()) {
					System.out.println("switching to read mode to read data from upstream");
					clientKey.interestOps(SelectionKey.OP_READ);
					//selectionKey.interestOps(SelectionKey.OP_READ);
				}
			} catch (IOException e){
				e.printStackTrace();
			}

//			ByteBuffer byteBuffer = ByteBuffer.allocate(8192);
//			try {
//				int length = clientSocketChannel.read(byteBuffer);
//				if (length == -1) {
//					closeConnection(clientSocketChannel);
//				} else {
//					byteBuffer.flip();
//					clientResponseStringBuilder.append(StandardCharsets.UTF_8.decode(byteBuffer));
//					responseBuffer = prepareDummyByteBuffer(clientResponseStringBuilder.toString());
//					if (clientResponseStringBuilder.length() > 0) {
//						selectionKey.interestOps(SelectionKey.OP_WRITE);
//					}
//				}
//			} catch (IOException e) {
//				closeConnection(clientSocketChannel);
//				e.printStackTrace();
//			}

		} else {
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

	private ByteBuffer prepareResponseByteBuffer() {
		String networkCallResponse = httpCall(uri);
		byte[] byteResponse = networkCallResponse.getBytes();
		StringBuilder customHeader = prepareHeader(byteResponse.length).append(networkCallResponse);
		byte[] customResponseBytes = customHeader.toString().getBytes();
		return ByteBuffer.wrap(customResponseBytes);
	}

	private ByteBuffer prepareDummyByteBuffer(String message) {
		byte[] byteData = message.getBytes();
		StringBuilder sb = prepareHeader(byteData.length).append(message);
		byte[] responseBytes = sb.toString().getBytes();
		return ByteBuffer.wrap(responseBytes);
	}

	private String httpCall(String uri) {
		try {
			String url = "http://localhost:8080//api/v1/product" + uri;
			System.out.println("calling url " + url);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(new URI(url))
					.GET()
					.build();
			HttpResponse<String> response = HttpClient.newHttpClient()
					.send(request, HttpResponse.BodyHandlers.ofString());
			System.out.println(response.body());
			uri = "";
			return response.body();

		} catch (IOException | InterruptedException | URISyntaxException e) {
			e.printStackTrace();
		}
		return "";
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

	public void connect(SelectionKey selectionKey1) {
//		clientKey.interestOps(SelectionKey.OP_WRITE);
//		System.out.println("connected");

		if (clientSocketChannel != null && selectionKey1.equals(clientKey)) {

			System.out.print("Connected through select() on " + clientKey.channel() + " -> ");
			try {
				if (clientSocketChannel.finishConnect()) { // Finish connection process
					System.out.println("done!");
					//this.clientKey.interestOps(clientKey.interestOps() & ~SelectionKey.OP_CONNECT); // We are already connected: remove interest in CONNECT event

				} else
					System.out.println("unfinished...");

			} catch (IOException e) {
				e.printStackTrace();
			}

			selectionKey1.interestOps(SelectionKey.OP_WRITE);
		}
	}
}
