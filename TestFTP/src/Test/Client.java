package Test;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by 63263 on 2017/2/27.
 */
public class Client {
    public static void main(String[] args){

        Client client = new Client();
        String location = "D:\\传说中的d盘-----工作\\作业\\大二下学期\\网络课程实践";
        try{
            Socket s = new Socket("localhost",9999);
            BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));

            String reponse;
            String command;
            Socket dataSocket;
            while(true){
                System.out.print("Command:  ");
                Scanner scanner = new Scanner(System.in);
                command = scanner.nextLine();
                if (command.toUpperCase().startsWith("USER")) {
                   client.user(writer,command,reader);
                } else if (command.toUpperCase().startsWith("PASS")) {
                    client.pass(writer,command,reader);
                } else if(command.toUpperCase().startsWith("PWD")){
                    writer.println("PWD");
                    writer.flush();
                    System.out.println(reader.readLine());
                }else if (command.toUpperCase().startsWith("PASV")) {
                    dataSocket = client.pasv(writer,command,reader);
                } else if (command.toUpperCase().startsWith("SIZE")) {
                    writer.println(command);
                    writer.flush();
                    reponse = reader.readLine();
                    System.out.println(reponse);
                } else if (command.toUpperCase().startsWith("REST")) {
                    dataSocket = client.pasv(writer,command,reader);
                    if(dataSocket==null)continue;
                    client.rest(command,writer,reader,dataSocket,location);
                } else if (command.toUpperCase().startsWith("RETR")) {
                    dataSocket = client.pasv(writer,command,reader);
                    if(dataSocket==null)continue;
                    client.retr(command,writer,location,dataSocket,reader);
                } else if (command.toUpperCase().startsWith("STOR")) {
                    dataSocket = client.pasv(writer,command,reader);
                    if(dataSocket==null)continue;
                    client.stor(command,writer,reader,dataSocket,location);
                } else if (command.toUpperCase().startsWith("QUIT")) {
                    writer.println("QUIT");
                    writer.flush();
                    reponse = reader.readLine();
                    System.out.println(reponse);
                    break;
                } else if (command.toUpperCase().startsWith("CWD")) {
                    client.cwd(command,writer,reader);
                } else if (command.toUpperCase().startsWith("LIST")) {
                    dataSocket = client.pasv(writer,command,reader);
                    if(dataSocket==null)continue;
                    client.list(writer,reader,dataSocket);
                }else{
                    System.out.println("Please write legally command!");
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void rest(String command,PrintWriter writer,BufferedReader reader,Socket dataSocket,String location){
        System.out.println("stor  or  retr");
        Scanner scanner = new Scanner(System.in);
        String c2 = scanner.nextLine();
        System.out.print("length:   ");
        long length = scanner.nextLong();
        if(c2.toUpperCase().startsWith("RETR")){
            retr_rest(command,writer,location,dataSocket,reader,length);
        }else if(c2.toUpperCase().startsWith("STOR")){
            writer.println(command);
            writer.flush();
            writer.println(length);
            writer.flush();
            try {
                RandomAccessFile outfile ;
                OutputStream outputStream ;
                try {
                    outfile = new RandomAccessFile(location + "/" + command.substring(4).trim(), "r");
                    outputStream = dataSocket.getOutputStream();
                    byte bytebuffer[] = new byte[1024];
                    try {
                        while (outfile.read(bytebuffer) != -1) {
                            outputStream = dataSocket.getOutputStream();
                            outputStream.write(bytebuffer);
                        }
                        outputStream.close();
                        outfile.close();
//                    tempsocket.close();
                    } catch (Exception e) {
                        e.getMessage();
                    }
                    String reponse = reader.readLine();
                    System.out.println(reponse);
                    dataSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }catch (Exception e ){
                e.getMessage();
            }


        }
    }
    public void cwd(String command,PrintWriter writer,BufferedReader reader){
        String reponse;
        writer.println(command);
        writer.flush();
        try {
            reponse = reader.readLine();
            System.out.println(reponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void stor(String command,PrintWriter writer,BufferedReader reader,Socket dataSocket,String location){
        String filename2=command.substring(4).trim();
        writer.println("STOR "+filename2);
        writer.flush();
        RandomAccessFile outfile = null;
        try {
            outfile = new RandomAccessFile(location+"\\"+filename2,"r");
            OutputStream outputStream = dataSocket.getOutputStream();
            byte bytebuffer2[] = new byte[1024];
            int length2;
            try {
                while ((length2 = outfile.read(bytebuffer2)) != -1) {
                    outputStream.write(bytebuffer2);
                    outputStream.flush();
                }

            }catch (Exception e){
                e.getMessage();
            }finally {
                outputStream.close();
                outfile.close();
                dataSocket.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {

            String reponse = reader.readLine();
            System.out.println(reponse);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void retr(String command,PrintWriter writer,String location,Socket dataSocket,BufferedReader reader) {
        String filename = command.substring(4).trim();
        String reponse;
        writer.println("RETR " + filename);
        writer.flush();
        try {
            reponse = reader.readLine();
            System.out.println(reponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
//            System.out.println(filename);
        RandomAccessFile locationFile = null;
        try {
            locationFile = new RandomAccessFile(location + "\\" + filename, "rw");
            InputStream inputStream = dataSocket.getInputStream();
            byte bytebuffer[] = new byte[1024];
            int length;
            try {
                while ((length = inputStream.read(bytebuffer)) != -1) {
                    System.out.println("copying...");
//                    inputStream.skip(6);
                    locationFile.write(bytebuffer, 0, length);
                }
            } catch (Exception e) {
                e.getMessage();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            reponse = reader.readLine();
            System.out.println(reponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 实现断点续传功能
     * @param command 指令内容
     * @param writer 输出流
     * @param location 下载位置
     * @param dataSocket 数据传输流
     * @param reader 输入流
     * @param skipLength 断点位置
     **/
    public void retr_rest(String command,PrintWriter writer,String location,Socket dataSocket,BufferedReader reader,long skipLength){
        String filename = command.substring(4).trim();
        String reponse;
        writer.println("RETR "+filename);
        writer.flush();
        try {
            reponse = reader.readLine();
            System.out.println(reponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        RandomAccessFile locationFile = null;
        try {
            locationFile = new RandomAccessFile(location+"\\"+filename,"rwd");
            InputStream inputStream = dataSocket.getInputStream();
            byte bytebuffer[] = new byte[1024];
            int length;
            try {
                inputStream.skip(skipLength);
                locationFile.skipBytes((int) skipLength);
                while ((length = inputStream.read(bytebuffer)) != -1) {
                    System.out.println("copying...");
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
            reponse = reader.readLine();
            System.out.println(reponse);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public void list(PrintWriter writer,BufferedReader reader,Socket dataSocket){
        String reponse;
        writer.println("List");
        writer.flush();
        try {
            reponse = reader.readLine();
            if(reponse.startsWith("530")){
                System.out.println(reponse);
            }else{
                System.out.println(reponse);
                DataInputStream dis = new DataInputStream(dataSocket.getInputStream());
                String string = "";
                while ((string = dis.readLine())!=null){
                    String  utf = new String(string.getBytes("ISO-8859-1"),"utf-8");
                    System.out.println(utf);
                }
                reponse = reader.readLine();
                System.out.println(reponse);

                dataSocket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public Socket pasv(PrintWriter writer,String command,BufferedReader reader) throws IOException {
        String reponse;
        writer.println("PASV");
        writer.flush();
            reponse = reader.readLine();
        if(reponse.startsWith("530")){
            System.out.println(reponse);
            return null;
        }else{
            System.out.println(reponse);
            String a[] = reponse.split(",");
            String a1[] = a[0].split("\\(");
            String a2[] = a[5].split("\\)");
            String ip = a1[1]+"."+a[1]+"."+a[2]+"."+a[3];
            int port1 = Integer.valueOf(a[4]) * 256 + Integer.valueOf(a2[0]);
//            System.out.println(ip);
            Socket dataSocket = new Socket(ip,port1);
            return dataSocket;
        }

    }
    public void user(PrintWriter writer,String command,BufferedReader reader){
        writer.println(command);
        writer.flush();
        try {
            String reponse = reader.readLine();
            System.out.println(reponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void pass(PrintWriter writer,String command,BufferedReader reader){
        writer.println(command);
        writer.flush();
        try {
            String reponse = reader.readLine();
            System.out.println(reponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
