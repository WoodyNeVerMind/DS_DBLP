import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class VirtualServer {
    private int port;
    public VirtualServer(int portID){
        this.port=portID;
    }
    public void receiveQuery() {
        try {
            ServerSocket server = new ServerSocket(port);
            while (true) {
                Socket socket = server.accept();
                DataInputStream is = new DataInputStream(socket.getInputStream());
                DataOutputStream os = new DataOutputStream(socket.getOutputStream());

                //接收来自客户端的name信息
                String name = "";
                name = is.readUTF();
                System.out.println("Recieved " + name);
                //接收来自客户端的beginYear信息
                String beginYear = "";
                beginYear = is.readUTF();
                System.out.println("Recieved " + beginYear);
                //接收来自客户端的endYear信息
                String endYear = "";
                endYear = is.readUTF();
                System.out.println("Recieved " + endYear);

                //确定接收到了来自客户端的信息
                if (name.length()>0) {
                    //向客户端发送查询结果信息
                    String queryResult=Query.queryByName(name,beginYear,endYear);
                    os.writeUTF(queryResult);
                    os.flush();
                }
                //关闭Socket链接
                is.close();
                os.close();
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}