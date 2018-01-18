package Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by 63263 on 2017/2/27.
 *等待客户端发送请求，建立新的线程处理
 */
public class Sever {
    public static void main(String[] args) throws IOException {
        final String F_DIR = "D:\\TOEFL";//根路径 
        Logger log = Logger.getLogger("");
//        final int PORT = 22;//监听端口号
        try {
            ServerSocket ss = new ServerSocket(8888);//建立Socket等待链接
            log.info("connecting");
            while(true){
                Socket s = ss.accept();//链接成功
                log.setLevel(Level.INFO);
                log.info("success");
                new ClientThread(s,F_DIR).start();//创建新的线程响应客户端请求
            }
        }catch (Exception e){
            e.getMessage();
        }

    }
}
