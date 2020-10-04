package com.paradox.servlet.client;

import com.paradox.Config;
import com.paradox.servlet.MyHttpServlet;
import com.paradox.servlet.MyHttpServletRequest;
import com.paradox.servlet.MyHttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class AddServlet extends MyHttpServlet {

    @Override
    public void doGet(MyHttpServletRequest myHttpServletRequest, MyHttpServletResponse myHttpServletResponse){

        //addition?num1=10&num2=23

        StringBuilder response;
        byte[] data;



        try{

            String host = Config.getInstance().getHost();
            String uri = myHttpServletRequest.getUri();
            //addition?num1=10&num2=23
            int n1 = uri.indexOf("num1")+5;
            int amp = uri.indexOf("&");
            int n2 = uri.indexOf("num2")+5;

            String number1 = uri.substring(n1,amp);
            String number2 = uri.substring(n2);

            int add = Integer.parseInt(number1)+Integer.parseInt(number2);

            uri = uri.substring(0,9) + ".html";

            Path path = Paths.get(host + File.separator + uri.substring(1));


            if (path.toFile().isFile()){

                List<String> list = Files.readAllLines(path);

                String resp = "<h1> addition result : " + add + " </h1>";

                list.set(2, resp);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);


                for(String elements : list){
                    out.writeBytes(elements);
                }

                data = baos.toByteArray();

                response = myHttpServletResponse.acceptHeader(data.length);

            }else {
                data = "resources not found".getBytes();
                response = myHttpServletResponse.errorHeader(data.length);
            }

            //
            // 
            myHttpServletResponse.getOutputStream().write(response.toString().getBytes());
            myHttpServletResponse.getOutputStream().write(data);


        } catch (NumberFormatException e){

            try{

                data = "resources not found".getBytes();
                response = myHttpServletResponse.errorHeader(data.length);
                myHttpServletResponse.getOutputStream().write(response.toString().getBytes());
                myHttpServletResponse.getOutputStream().write(data);

                System.out.println(e.getLocalizedMessage());

            }catch (IOException ex){
                System.out.println(ex.getLocalizedMessage());
            }

            System.out.println(e.getLocalizedMessage());
        }

        catch (IOException e){
            System.out.println(e.getLocalizedMessage());
        }
    }
}
