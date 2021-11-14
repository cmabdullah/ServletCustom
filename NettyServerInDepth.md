
## App
```java
package com.reactive;

import com.reactive.enhanch.SocketProcessorV2;

import java.nio.channels.SocketChannel;
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
```

## SocketAcceptor
```java
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
```

## SocketProcessorV2
```java
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
						// System.out.println("requestIde "+requestIde +" is entering mode -> " +interest + " with class "+processor.getClass());
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
```

## Processor
```java
package com.reactive.enhanch;

import java.nio.channels.SelectionKey;

public interface Processor {
	void read();
	void write();
	default void connect(SelectionKey selectionKey){
		System.out.println("Calling connector");
	}

	String getRequestId();
}

```
## ServerRequestProcessor
```java
package com.reactive.enhanch;

import java.io.IOException;
import java.net.URL;
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

				if (urlPath.equals("favicon.ico")){
					closeConnection(socketChannel);
					return;
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

```

## WebClientRequestProcessor
```java
package com.reactive.enhanch;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
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
			} else if (length == 0){
				// response is not prepared yet
				// this.selectionKey.interestOps(SelectionKey.OP_READ);
				// https://stackoverflow.com/questions/34490207/the-difference-of-socketchannel-read-in-async-and-sync-mode
			}

			else if (length > 0){
				readBuff.flip();
				stringBuilder.append(StandardCharsets.UTF_8.decode(readBuff));

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
			}

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
```