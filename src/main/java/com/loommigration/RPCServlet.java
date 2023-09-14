package com.loommigration;

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
	public void doGet(
			MyHttpServletRequest myHttpServletRequest,
			MyHttpServletResponse myHttpServletResponse) {
		StringBuilder response;
		byte[] data;
		try {
			String host = Config.getInstance().getHost();
			String uri = myHttpServletRequest.getUri();
			//?a1=sylet&a2=city
			String lastPartOfThisUrl = uri.substring(uri.indexOf('?'));
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(baos);
			String httpResponse = httpCall(lastPartOfThisUrl);
			data = httpResponse.getBytes(StandardCharsets.UTF_8);
			response = myHttpServletResponse.acceptHeader(data.length);
			myHttpServletResponse.getOutputStream().write(response.toString().getBytes());
			myHttpServletResponse.getOutputStream().write(data);
		} catch (IOException e) {
			System.out.println(e.getLocalizedMessage());
		}
	}

	private String httpCall(String lastPartOfThisUrl) {

		//http://localhost:8080/api/v1/product/add?a1=sylet&a2=city
		String url = Config.getInstance().getRpcUrl() + lastPartOfThisUrl;
		System.out.println("calling url " + url);
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
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
