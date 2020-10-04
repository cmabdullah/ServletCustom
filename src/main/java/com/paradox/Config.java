package com.paradox;

public class Config {

    private int port;
    private String host;
    private int requestProcessor;

    private static class Holder{
        private static Config instance = new Config (8081, "html", Runtime.getRuntime().availableProcessors());
    }

    private Config (int port, String host, int requestProcessor){
        this.port = port;
        this.host = host;
        this.requestProcessor = requestProcessor;
    }

    public static Config getInstance(){
        return Holder.instance;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getRequestProcessor() {
        return requestProcessor;
    }

    public void setRequestProcessor(int requestProcessor) {
        this.requestProcessor = requestProcessor;
    }
}
