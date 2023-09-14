package com.paradox.servlet.client;

import com.paradox.Config;
import com.paradox.servlet.MyHttpServlet;
import com.paradox.servlet.MyHttpServletRequest;
import com.paradox.servlet.MyHttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class RPCServlet extends MyHttpServlet {

	@Override
	public void doGet(MyHttpServletRequest myHttpServletRequest, MyHttpServletResponse myHttpServletResponse) {
		StringBuilder response;
		byte[] data;

		try {
			String host = Config.getInstance().getHost();
			String uri = myHttpServletRequest.getUri();

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(baos);

			String httpResponse = httpCall();

			data = httpResponse.getBytes(StandardCharsets.UTF_8);

			response = myHttpServletResponse.acceptHeader(data.length);

			myHttpServletResponse.getOutputStream().write(response.toString().getBytes());
			myHttpServletResponse.getOutputStream().write(data);
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
		}
	}

	private String httpCall() {
		try {
			String url = "http://localhost:8080/api/v1/product/add?a1=cm&a2=khulna";
			System.out.println("calling url " + url);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(new URI(url))
					.GET()
					.build();
			HttpResponse<String> response = HttpClient.newHttpClient()
					.send(request, HttpResponse.BodyHandlers.ofString());
			System.out.println(response.body());
			return response.body();

		} catch (IOException | InterruptedException | URISyntaxException e) {
			e.printStackTrace();
		}
		return "";
	}
}
