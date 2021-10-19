package com.reactive.enhanch;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class WebClientRequestProcessor implements Processor {

	private SocketChannel webClientSocketChannel;
	private Selector selector;
	private SelectionKey selectionKey;
	private StringBuilder stringBuilder;
	private ServerRequestProcessor serverRequestProcessor;
	private URL url;
	private String urlPath;
	private String requestId;
	private ByteBuffer webClientRequestBuffer;
	private SelectionKey clientKey;

	public WebClientRequestProcessor(URL url, String urlPath,Selector selector, String requestId,
									 ServerRequestProcessor serverRequestProcessor) {
		this.url = url;
		this.urlPath  = urlPath;
		this.selector = selector;
		this.stringBuilder = new StringBuilder();
		this.requestId = requestId;
		this.serverRequestProcessor = serverRequestProcessor;
		prepareRequest(url, urlPath);
	}

	private void prepareRequest(URL url, String urlPath) {
		try {
			String host = url.getHost(), port = String.valueOf(url.getPort()), path = url.getPath();
			SocketAddress inetSocketAddress = new InetSocketAddress(host, Integer.parseInt(port));
			webClientSocketChannel = SocketChannel.open();
			webClientSocketChannel.configureBlocking(false);
			if (webClientSocketChannel.connect(inetSocketAddress)) {
				System.out.println("Connected"); // Connected right-away: nothing else to do
			} else {
				System.out.println("connecting... to "+requestId);
			}
			clientKey = webClientSocketChannel.register(selector, SelectionKey.OP_CONNECT);
			clientKey.attach(this);
			webClientRequestBuffer = prepareByteBufferForClientRequest(urlPath, host, port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ByteBuffer prepareByteBufferForClientRequest(String path, String host, String port) {
		byte[] message = new String("GET " + path + " HTTP/1.0\r\nHost:" + host + ":" + port + " \r\n\r\n").getBytes();
		return ByteBuffer.wrap(message);
	}

	@Override
	public void read() { // hear actually blocking until getting response

		System.out.println("start upstream response processing for request ID "+requestId);
		ByteBuffer readBuff = MappedByteBuffer.allocate(1500);

		// create mono or flask

		try {
			int length = webClientSocketChannel.read(readBuff);
			System.out.println("response length size is : "+length+ " for request ID "+requestId);
			if (length == -1) {
				closeConnection(webClientSocketChannel);
			} else if (length > 0){
				readBuff.flip();
				stringBuilder.append(StandardCharsets.UTF_8.decode(readBuff));
			}
			String str = stringBuilder.toString();


			//block thread hear until get response
//			String res = "";
//			while (webClientSocketChannel.read(readBuff) != -1) {
//				String temp = new String(readBuff.array()).trim();
//				res = res + temp;
//				readBuff.clear();
//			}
//			String[] response = res.split("\r\n");
//			System.out.println("The result is : " + response[6]);
//			String str = response[6];

			System.out.println("response from upstream  for request ID : "+requestId+" is \n" +str);

			serverRequestProcessor.updateResponse(str);
			System.out.println("closing webclient connection for request ID "+requestId);
			closeConnection(webClientSocketChannel);
			clientKey.cancel();//cancel subscription
			//this.selectionKey.interestOps(SelectionKey.OP_WRITE);

		} catch (IOException e) {
			closeConnection(webClientSocketChannel);
		}


	}

	@Override
	public void write() {
		try {
			webClientSocketChannel.write(webClientRequestBuffer);
			System.out.println("writing request header to call Upstream through webClient with RequestID "+ requestId);
			if (!webClientRequestBuffer.hasRemaining()) {
				System.out.println("switching to client read mode to read data from upstream for RequestID "+ requestId);
				this.selectionKey.interestOps(SelectionKey.OP_READ);
			}
		} catch (IOException e) {
			closeConnection(webClientSocketChannel);
			e.printStackTrace();
		}
	}

	@Override
	public void connect(SelectionKey selectionKey) {

		System.out.print("Connected through select() on " + selectionKey.channel() + " -> ");
		try {
			webClientSocketChannel = (SocketChannel) selectionKey.channel();
			this.selectionKey = selectionKey;

			if (selectionKey.isConnectable() && !webClientSocketChannel.isConnected()) {
				if (webClientSocketChannel.finishConnect()) { // Finish connection process
					System.out.println("done! with request ID " + requestId);
//				selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_CONNECT); // We are already connected: remove interest in CONNECT event
				} else {
					System.out.println("unfinished...");
				}
			}
			selectionKey.interestOps(SelectionKey.OP_WRITE);
			// TODO: else if (k.isReadable()) { ...

		} catch (IOException e) {
			closeConnection(webClientSocketChannel);
			e.printStackTrace();
		}
	}

	@Override
	public String getRequestId() {
		return requestId;
	}

	private void closeConnection(SocketChannel socketChannel) {
		try {
			socketChannel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
