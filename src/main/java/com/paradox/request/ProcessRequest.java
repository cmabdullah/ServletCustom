package com.paradox.request;

import com.paradox.App;
import com.paradox.servlet.MyHttpServlet;
import com.paradox.servlet.MyHttpServletRequest;
import com.paradox.servlet.MyHttpServletResponse;
import com.paradox.servlet.client.AddServlet;
import com.paradox.servlet.client.IndexServlet;

import java.io.IOException;
import java.net.Socket;

public class ProcessRequest extends Thread{

    @Override
    public void run(){

        while (true){

            try{
                Socket socket = App.SOCKET_QUEUE.take();

                MyHttpServletRequest myHttpServletRequest = new MyHttpServletRequest(socket.getInputStream());
                MyHttpServletResponse myHttpServletResponse = new MyHttpServletResponse(socket.getOutputStream());

                int position = myHttpServletRequest.getPosition();

                if (position > 0){
                    String uri = myHttpServletRequest.getUri();

                    if (uri.equalsIgnoreCase("/")){
                        //indexServlet

                        MyHttpServlet myHttpServlet = new IndexServlet();
                        myHttpServlet.doGet(myHttpServletRequest, myHttpServletResponse);
                    }

                    if (uri.startsWith("/addition")){
                        //AddServlet
                        MyHttpServlet myHttpServlet = new AddServlet();
                        myHttpServlet.doGet(myHttpServletRequest, myHttpServletResponse);
                    }

                }


            } catch (InterruptedException |IOException e) {
                System.out.println(e.getLocalizedMessage());
            }
        }

    }
}
