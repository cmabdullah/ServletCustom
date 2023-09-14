package com.loommigration;

import java.net.Socket;
public class ProcessRequest {
	public boolean startProcessing(Socket socket) {

		try (MyHttpServletRequest myHttpServletRequest = new MyHttpServletRequest(
				socket.getInputStream());
				MyHttpServletResponse myHttpServletResponse = new MyHttpServletResponse(
						socket.getOutputStream())) {

			boolean isProcessingFinished = false;
			int position = myHttpServletRequest.getPosition();
			if (position > 0) {
				String uri = myHttpServletRequest.getUri();

				//http://localhost:8081
				if (uri.equalsIgnoreCase("/")) {
					//indexServlet
					MyHttpServlet myHttpServlet = new IndexServlet();
					myHttpServlet.doGet(myHttpServletRequest, myHttpServletResponse);
					isProcessingFinished = true;
				}
				//http://localhost:8081/rpc?a1=sylet&a2=city
				if (uri.startsWith("/rpc")) {
					//RPCServlet
					MyHttpServlet myHttpServlet = new RPCServlet();
					System.out.println(Thread.currentThread().getName());
					myHttpServlet.doGet(myHttpServletRequest, myHttpServletResponse);
					isProcessingFinished = true;
				}
			}
			return isProcessingFinished;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
