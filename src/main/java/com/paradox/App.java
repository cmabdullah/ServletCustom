package com.paradox;

import com.paradox.request.AcceptRequest;
import com.paradox.request.ProcessRequest;

import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

public class App {

    public static LinkedBlockingQueue<Socket> SOCKET_QUEUE = new LinkedBlockingQueue<>();

    public static void main(String[] args) {
        //System.out.println("hello");

        AcceptRequest acceptRequest = new AcceptRequest(Config.getInstance().getPort());

        Thread acceptRequestThread = new Thread(acceptRequest);
        acceptRequestThread.setName("acceptRequestThread");
        acceptRequestThread.start();

        int requestProcessor = Config.getInstance().getRequestProcessor();

        for (int i = 0; i<requestProcessor; i++){
            ProcessRequest processRequest = new ProcessRequest();
            Thread thread = new Thread(processRequest);
            thread.setName("Custom tomcat "+i);
            thread.start();
        }

    }
}
