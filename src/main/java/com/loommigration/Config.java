package com.loommigration;

public class Config {

	private int port;
	private String host;
	private int requestProcessor;
	private String rpcUrl;

	private static class Holder {
		private static Config instance = new Config(8081, "html",
				Runtime.getRuntime().availableProcessors(),
				"http://localhost:8080/api/v1/product/add");
	}

	private Config(int port, String host, int requestProcessor, String rpcUrl) {
		this.port = port;
		this.host = host;
		this.requestProcessor = requestProcessor;
		this.rpcUrl = rpcUrl;
	}

	public static Config getInstance() {
		return Holder.instance;
	}

	public int getPort() {
		return port;
	}

	public String getHost() {
		return host;
	}

	public int getRequestProcessor() {
		return requestProcessor;
	}

	public String getRpcUrl() {
		return rpcUrl;
	}
}
