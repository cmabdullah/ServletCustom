package com.paradox.servlet;

import java.io.OutputStream;

public class MyHttpServletResponse {

    private OutputStream outputStream;

    public MyHttpServletResponse(OutputStream outputStream){
        this.outputStream = outputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }


    public StringBuilder acceptHeader(int dataLength){

        return new StringBuilder().append("HTTP/1.1 200 Ok\r\n")
                .append("Content-Type: ")
                .append("text/html\r\n")
                .append("Connection: Closed\r\n")
                .append("Content-Length: ")
                .append(dataLength)
                .append("\r\n\r\n");

    }

    public StringBuilder errorHeader(int dataLength){

        return new StringBuilder().append("HTTP/1.1 404 Not Found\r\n")
                .append("Content-Type: ")
                .append("text/html\r\n")
                .append("Connection: Closed\r\n")
                .append("Content-Length: ")
                .append(dataLength)
                .append("\r\n\r\n");
    }
}
