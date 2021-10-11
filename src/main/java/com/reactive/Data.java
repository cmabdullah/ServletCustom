package com.reactive;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
	String uri = "";

	private ByteBuffer responseBuffer = prepareDummyByteBuffer();

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

	private ByteBuffer prepareResponseByteBuffer() {
		String networkCallResponse = httpCall(uri);
		byte[] byteResponse = networkCallResponse.getBytes();
		StringBuilder customHeader = prepareHeader(byteResponse.length).append(networkCallResponse);
		byte[] customResponseBytes = customHeader.toString().getBytes();
		return ByteBuffer.wrap(customResponseBytes);
	}

	private ByteBuffer prepareDummyByteBuffer() {
		String message = "Hello World";
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
}
