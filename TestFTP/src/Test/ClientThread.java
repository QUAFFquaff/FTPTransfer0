package Test;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Created by 63263 on 2017/2/28.
 */
public class ClientThread extends Thread{

    private Socket s;
    private String dir;
    private String pdir;
    public ClientThread(Socket s,String F_DIR) {
        this.s = s;
        this.dir = F_DIR;
    }

    @Override
    public void run() {
        InputStream inputStream;
        Logger log = Logger.getLogger("");
        boolean login = false;
        try {

            String command;
            String username = null;
            String password = null;
            Socket tempsocket = null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream(), Charset.forName("UTF-8")));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));

            String clientIp = s.getInetAddress().toString().substring(1);//

            while (true) {
                try {
                    command = reader.readLine();
                    if (command == "exit") break;
                } catch (Exception e) {
                    writer.println("331 Failed to get command");
                    writer.flush();
                    log.info(e.getMessage());
                    break;
                }
                if (command.toUpperCase().startsWith("USER")) {
                    username = command.substring(4).trim();//去除空格和前四位
                    writer.println("331 Password required for " + username);
                    writer.flush();
                } else if (command.toUpperCase().startsWith("PASS")) {
                    password = command.substring(4).trim();
                    if(password.startsWith("p")){
                        writer.println("230 Logged on");
                        writer.flush();
                        login = true;
                    }else{
                        writer.println("530 Login or password incorrect!");
                        writer.flush();
                        login = false;
                    }
                }else if(!login){
                    writer.println("530 Please log in with USER and PASS first.");
                    writer.flush();
                }
                else if(command.toUpperCase().startsWith("PWD")){
                    writer.println("257 "+dir+" is current directory");
                    writer.flush();
                }  else if (command.toUpperCase().startsWith("PASV")) {
                    tempsocket = pasv(clientIp, username, password, writer, tempsocket);
                } else if (command.toUpperCase().startsWith("SIZE")) {
                    size(command, writer, clientIp);
                } else if (command.toUpperCase().startsWith("REST")) {
                    System.out.println("00000000011");
                    long length = Long.valueOf(reader.readLine());
                    rest_stor(command,length,writer,tempsocket);
                } else if (command.toUpperCase().startsWith("RETR")) {
                    retr(command,tempsocket,writer);
                } else if (command.toUpperCase().startsWith("STOR")) {
                    stor(command, tempsocket, writer);
                } else if (command.toUpperCase().startsWith("QUIT")) {
                    writer.println("221 Goodbye");
                    writer.flush();
                    break;
                } else if (command.toUpperCase().startsWith("CWD")) {

                    cwd(command, writer);
                } else if (command.toUpperCase().startsWith("LIST")) {
                    list( writer, tempsocket);
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void rest_stor(String command,long skipLength,PrintWriter writer,Socket tempSocket){
        String filename = command.substring(4).trim();
        System.out.println("00000000000");
        RandomAccessFile locationFile = null;
        try {
            System.out.println("new file");
            locationFile = new RandomAccessFile(dir+"\\"+filename,"rwd");
            InputStream inputStream = tempSocket.getInputStream();
            byte bytebuffer[] = new byte[1024];
            int length;
            try {
                inputStream.skip(skipLength);
                locationFile.skipBytes((int) skipLength);
                while ((length = inputStream.read(bytebuffer)) != -1) {
                    locationFile.write(bytebuffer, 0, length);
                }
            }catch (Exception e) {
                e.getMessage();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            writer.println("150 Opening data channel for file transfer.");
            writer.flush();
            tempSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Over");
    }
    public void stor(String command,Socket tempsocket,PrintWriter writer) throws IOException {
        String filename = command.substring(4).trim();
        RandomAccessFile outfile = new RandomAccessFile(dir+"\\"+filename,"rw");
        OutputStream outputStream = tempsocket.getOutputStream();
//        InputStream outputStream = tempsocket.getInputStream();
        byte bytebuffer[] = new byte[1024];
//        int length;
        try {
            while (outfile.read(bytebuffer) != -1) {
                outputStream.write(bytebuffer);
                outputStream.flush();
            }
        }catch (Exception e){
            e.getMessage();
        }finally {
            outputStream.close();
            outfile.close();
        }

        writer.println("150 Opening data channel for file transfer.");
        writer.flush();
        tempsocket.close();
    }
    public void retr(String command,Socket tempsocket,PrintWriter writer){
        String file = command.substring(4).trim();
        System.out.println(file);
        if ("".equals(file)) {
            writer.println("501 Syntax error");
            writer.flush();
        } else {
            try{
                RandomAccessFile outfile = null;
                OutputStream outputStream = null;
                try {
                    writer.println("150 Oping data channel for file transfer");
                    writer.flush();
                    try {
                        outfile = new RandomAccessFile(dir + "/" + file, "r");
                        outputStream = tempsocket.getOutputStream();

                    } catch (FileNotFoundException e) {
                        e.getMessage();
                    }
                } catch (IOException e) {
                    e.getMessage();
                }
                byte bytebuffer[] = new byte[1024];
                int length;
                try {
                    while ((length = outfile.read(bytebuffer)) != -1) {
                        outputStream = tempsocket.getOutputStream();
                        outputStream.write(bytebuffer);
                    }
                    outputStream.close();
                    outfile.close();
                    tempsocket.close();
                } catch (Exception e) {
                    e.getMessage();
                }
                writer.println("226 Transfer OK");
                writer.flush();
            }catch (Exception e){
                writer.println("503 Bad sequence of commands.");
                writer.flush();

            }

        }
    }
    public void size(String command,PrintWriter writer,String clientIp){

        System.out.println("用户" + clientIp +  "     执行SIZE命令");
        String filename = command.substring(4).trim();
        File file = new File(dir+"\\"+filename);
        writer.println("file: "+filename+" length:  "+file.length());
        writer.flush();
        System.out.println("用户" + clientIp +  "     SIZE命令完成");
    }
    public Socket pasv(String clientIp,String username,String password,PrintWriter writer,Socket tempsocket){
        ServerSocket ss;
        int port_high;
        int port_low;
        while(true){//新建端口失败后可以继续创建，直到创建成功
            Random generator = new Random();
            port_high = 1 + generator.nextInt(20);
            port_low = 100 + generator.nextInt(1000);
            try {
                ss = new ServerSocket(port_high * 256 +port_low);//生成端口、响应码
                break;
            }catch (IOException e){
                e.getMessage();
            }
        }
        System.out.println("用户" + clientIp + "." + username +"   密码： "+password+ "执行PASV命令");
        InetAddress i = null;
        try{
            i = InetAddress.getLocalHost();
        }catch (UnknownHostException e){
            e.printStackTrace();
        }
        writer.println("227 Entering Passive Mode ("+i.getHostAddress().replace(".",",")+","+port_high+","+port_low+")");//返回客户端指令
        writer.flush();
        try {
            tempsocket = ss.accept();
            ss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempsocket;
    }
    public void cwd(String command, PrintWriter writer){
        String str = command.substring(3).trim();
        String tmpDir = dir +"/" + str;
        System.out.println(tmpDir);
        if("".equals(str)){
            writer.println("250 Broken client detected, missing argument to CWD");
            writer.flush();
        }else {
            File file = new File(tmpDir);
            if(file.exists()){
//                writer.println("location changed to "+tmpDir);
                dir = dir + "\\" +str;
                if("/".equals(pdir)){
                    pdir = pdir +str;
                }else{
                    pdir = pdir +"/"+str;
                }
                writer.println("250 CWD successful. /"+pdir+"/ is current directory");
                writer.flush();
            }else{
                writer.println("550 CWD failed. /"+pdir+"/: directory not found.");
                writer.flush();
            }
        }
    }
    public void list(PrintWriter writer,Socket tempsocket){
        try{
            writer.println("150 Opening data channel for directory list.");
            writer.flush();
            PrintWriter pwr = null;
            try{
                pwr = new PrintWriter(tempsocket.getOutputStream(),true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            FtpUtil.getDetailList(pwr,dir);
            try{
                tempsocket.close();
                pwr.close();
            }catch (Exception e){
                e.getMessage();
            }
            writer.println("226 Transfer OK");
            writer.flush();
        }catch (Exception e){
            writer.println("503 Bad sequence of commands");
            writer.flush();
        }

    }



}


class FtpUtil {
    public static void getDetailList(PrintWriter writer, String path) {
        File file = new File(path);
        if (!file.isDirectory()) {
            writer.println("500 No such file or directory./r/n");
        }
        File[] files = file.listFiles();
        String modifyDate;
        for (int i = 0; i < files.length; i++) {
            modifyDate = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(new Date(files[i].lastModified()));
//            writer.println(" " + modifyDate + "   ");
            if (files[i].isDirectory()) {
                writer.println("     " + modifyDate + " " + files[i].getName());
            } else {
                writer.println("     " + files[i].length() + " " + modifyDate + " " + files[i].getName());
            }
            writer.flush();
        }
        writer.println("total:" + files.length);
    }
}