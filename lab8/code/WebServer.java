import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;


public class WebServer{

    final int PORT = 6269;
    private ServerSocket serverSocket;


    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("服务器端口:" + PORT);
//            System.out.println("用户名:" + "3170106269");
//            System.out.println("密码:" + "6269");
            System.out.println("http://127.0.0.1:6269/home/test.html");

            Socket socket = null;
            int count = 0;
            System.out.println("---服务器即将启动，等待客户端的连接---");

            while (true) {
                socket = serverSocket.accept();
                ServerThread serverThread = new ServerThread(socket);
                serverThread.start();
                count++;//统计客户端的数量
                System.out.println("客户端的数量：" + count);
                InetAddress address = socket.getInetAddress();
                System.out.println("当前客户端的IP：" + address.getHostAddress());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}

