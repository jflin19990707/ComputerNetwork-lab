import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;



enum ResponseType {
    /**
     * HTML
     */
    HTML {
        @Override
        public String toString()
        {
            return "text/html";
        }
    },
    /**
     * JPG
     */
    JPG {
        @Override
        public String toString()
        {
            return "image/jpg";
        }
    },
    /**
     * TEXT
     */
    TEXT {
        @Override
        public String toString()
        {
            return "text/txt";
        }
    };
}


/*
 * 服务器线程处理类
 */
public class ServerThread extends Thread {

    final int POST = 2;
    final int GET = 1;

    // 和本线程相关的Socket
    Socket socket = null;

    public ServerThread(Socket socket){
        this.socket = socket;
    }

    //生成要发送的信息
    public void resMeg(int state, ResponseType rtype,File reqFile, OutputStream os) throws IOException {
        PrintWriter pw = new PrintWriter(os);
        Reader reader = null;

        int tempchar;
        String body = "";
        String res = "";
        FileInputStream instream = null;

//        System.out.println(state);
        if(state==404){
            body = "<html><body>404</body></html>\r\n";
        }else {
            reader = new InputStreamReader(new FileInputStream(reqFile));
            while ((tempchar = reader.read()) != -1) {
                body += (char) tempchar;
            }
        }

        res += "HTTP/1.1 ";
        res += String.valueOf(state);
        res += " OK\r\n";
        res += "Content-Type: text/html; charset=UTF-8\r\n";
        res += "Content-Length: ";
        res += String.valueOf(body.length());
        res +=  " \r\n";
        res += "\r\n";
        res += body;
        //System.out.println(res);

        pw = new PrintWriter(os);
        //向外写出infoOut
        pw.write(res);
        //写出
        pw.flush();//调用flush()方法将缓冲输出
    }

    public void resImgMeg(int state, ResponseType rtype, File imageFile, OutputStream os) throws IOException
    {
        String res = "";
        res += "HTTP/1.1 ";
        res += String.valueOf(state);
        res += " OK\r\n";
        res += "Content-Type: " + rtype + "; charset=UTF-8\r\n";
        res += "Content-Length: ";
        res += String.valueOf(imageFile.length());
        res +=  " \r\n";
        res += "\r\n";
        os.write(res.getBytes(StandardCharsets.UTF_8));
        Files.copy(imageFile.toPath(), os);
    }

    //线程执行的操作，响应客户端的请求
    @Override
    public void run(){
        // else, go on
        InputStream is=null;
        InputStreamReader isr=null;
        BufferedReader br=null;
        OutputStream os=null;
        PrintWriter pw=null;
        try {
            //获取输入流，并读取客户端信息
            is = socket.getInputStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String info = br.readLine();
            String infoOut = null;
            String body = null;
            int state = 0;
            ResponseType rtype = ResponseType.HTML;

            File reqFile = null;

            //解析info
            if(info.startsWith("GET")){
//                System.out.println("GET");
                System.out.println("信息: "+info);
                String[] list = info.split(" ");
                String address = routerMap(list[1]);

                File directory = new File(".");     
                String filePath = directory.getCanonicalPath() + address;
//                System.out.println(filePath);
                if(filePath.indexOf(".txt") != -1) {
                    rtype = ResponseType.TEXT;
                }else if(filePath.indexOf(".jpg") != -1){
                    rtype = ResponseType.JPG;
                }else {
                    rtype = ResponseType.HTML;
                }
                reqFile = new File(filePath);
                //设置状态
                if(reqFile.exists() && filePath.indexOf(".")!=-1){
                    state = 200;
                }
                else{
                    state = 404;
                }
            }else if(info.startsWith("POST")){
                System.out.println("信息: "+info);
                String[] list = info.split(" ");
                String address = list[1];
                if(address.endsWith("dopost")){
                    state = 200;
                    rtype = ResponseType.HTML;
                    int tmp;
                    int Length = 0;
                    //循环读取客户端的信息
                    while(true){
                        info = br.readLine();
//                        System.out.println("say: " + info);
                        if("".equals(info)) {
                            break;
                        }
                        if(info.startsWith("Content-Length:")){
                            list = info.split(" ");
                            Length = Integer.parseInt(list[1]);
//                            System.out.println("len" + Length);
                        }
                    }
                    //读取用户名和密码
                    String form = "";
                    for(int i=0; i<Length; i++){
                        tmp = br.read();
                        form += (char)tmp;
                        System.out.print((char) tmp);
                    }
                    System.out.println("");

                    list = form.split("&");
                    String[] userName = list[0].split("=");
                    String[] passWord = list[1].split("=");
                    if(userName.length==2 && passWord.length==2 && userName[1].substring(userName[1].length()-4,userName[1].length()).equals(passWord[1])){
                        System.out.println("A client log in sucessfully!");
                        File directory = new File(".");
                        String filePath = directory.getCanonicalPath() + "/web/success.html";
                        reqFile = new File(filePath);
                    }else{
                        System.out.println("A client log in failed!");
                        File directory = new File(".");
                        String filePath = directory.getCanonicalPath() + "/web/fail.html";
                        reqFile = new File(filePath);
                    }
                }else {
                    state = 404;
                }
            }

            socket.shutdownInput();//关闭输入流
            //获取输出流，响应客户端的请求
            os = socket.getOutputStream();
            // HTML or TEXT
            if(rtype == ResponseType.HTML || rtype == ResponseType.TEXT) {
                resMeg(state, rtype, reqFile, os);
            }
            // IMG
            else {
                resImgMeg(state, rtype, reqFile, os);
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }finally{
            //关闭资源
            try {
                if(pw!=null) {
                    pw.close();
                }
                if(os!=null) {
                    os.close();
                }
                if(br!=null) {
                    br.close();
                }
                if(isr!=null) {
                    isr.close();
                }
                if(is!=null) {
                    is.close();
                }
                if(socket!=null) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String routerMap(String fileName)
    {
        return fileName.replace("home", "web");
    }
}
