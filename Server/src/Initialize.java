import java.io.*;
import javax.xml.stream.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

public class Initialize {
    private static String DBLP_Path = "/mnt/dblpXmls";
    private static String DBLP_Backup_Path = "/mnt/dblpBackupXmls";
    /**
     * @Description TODO: 切分 DBLP.xml
     * @return
     * @Author root
     * @Date 2022/12/11 14:35
     * @Version 1.0
     **/
    public static void SplitXml() throws Exception {
        //按块拆分的大类标签
        Set set = new HashSet();
        set.add("article");
        set.add("book");
        set.add("inproceedings");
        set.add("proceedings");
        set.add("incollection");
        set.add("phdthesis");
        set.add("mastersthesis");
        set.add("www");
        set.add("data");

        //输入输出路径
        String inputFile = "/mnt/dblp.xml";
        String outputDir = "/mnt/splitedXmls";

        // 创建一个 XMLInputFactory
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        // 创建一个 XMLStreamReader
        XMLStreamReader reader = inputFactory.createXMLStreamReader(new FileReader(inputFile));

        // 用于记录当前片段的文件名
        String currentFile = null;

        //当前使用的writer流
        XMLStreamWriter currentWriter=null;
        //存储24个writer文件流
        List<XMLStreamWriter> list = new ArrayList<>();

        //随机数，用于随机分配
        Random random1=new Random(10);
        // 开始读取 XML 文档
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    // 如果是dblp标签开始，则创建n个文件流，准备向n个文件输出内容
                    if ("dblp".equals(reader.getLocalName())) {
                        System.out.println("正在切分DBLP.xml文件");
                        for(int i=0;i<24;i++){
                            currentFile = outputDir + "/dblp" + i + ".xml";
                            // 创建 XMLStreamWriter
                            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
                            XMLStreamWriter writer = outputFactory.createXMLStreamWriter(new FileWriter(currentFile));
                            list.add(writer);
                            // 写入片段的开头
                            list.get(i).writeStartDocument();
                            list.get(i).writeStartElement("dblp");
                        }
                        //dblp标签后还有一个回车，会进入characters的分支，为防止currentWriter指针空，暂且将其设置为第一个输出
                        currentWriter=list.get(0);
                    }
                    else if(set.contains(reader.getLocalName())){
                        //设置当前流为随机的一个writer流，目的是以大的标签块为单位随机分配给拆分的xml文件中
                        currentWriter=list.get(random1.nextInt(list.size()));
                        currentWriter.writeStartElement(reader.getLocalName());
                    }
                    else {
                        // 写入元素的开头
                        currentWriter.writeStartElement(reader.getLocalName());
                        // 写入元素的属性
                        for (int i = 0; i < reader.getAttributeCount(); i++) {
                            currentWriter.writeAttribute(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
                        }
                    }
                    break;
                case XMLStreamConstants.CHARACTERS:
                    // 写入元素的文本内容
                    currentWriter.writeCharacters(reader.getText());
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    // 如果是某个元素的结束，则关闭当前片段
                    if ("dblp".equals(reader.getLocalName())) {
                        for(int i=0;i<24;i++){
                            // 写入片段的结尾
                            list.get(i).writeEndElement();
                            list.get(i).writeEndDocument();
                            // 关闭 XMLStreamWriter
                            list.get(i).close();
                            // 重置 XMLStreamWriter
                            currentWriter = null;
                        }
                    }
                    else {
                        // 写入元素的结尾
                        currentWriter.writeEndElement();
                    }
                    break;
            }
        }
        // 关闭 XMLStreamReader
        reader.close();
        System.out.println("DBLP.xml文件切分完成");
    }
    /**
     * @Description TODO: 将切分好的DBLP.xml文件传输给其他服务器
     * @return
     * @Author root
     * @Date 2022/12/11 14:58
     * @Version 1.0
     **/
    public static void sendXml(String fileName,String ipSelected,int portSelected,boolean isBackup) throws Exception{
        // 创建Socket对象
        Socket socket = new Socket(ipSelected, portSelected);

        // 创建输出流
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());

        // 向Server传递文件名称
        outputStream.writeUTF(fileName);
        outputStream.flush();

        // 向Server传递是否为备份文件的信息
        String backupTag;
        if(isBackup==true){
            backupTag="isBackup";
        }
        else{
            backupTag="notBackup";
        }
        outputStream.writeUTF(backupTag);
        outputStream.flush();

        // 文件路径
        String filePath="/mnt/splitedXmls/"+fileName;

        // 读取文件并将文件内容写入输出流
        File file = new File(filePath);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        // 关闭文件输入流和Socket输出流
        fileInputStream.close();
        outputStream.close();
    }

    public static void receiveXml(int portSelected) throws Exception{
        // 创建ServerSocket对象
        ServerSocket serverSocket = new ServerSocket(portSelected);

        // 等待客户端连接
        Socket socket = serverSocket.accept();

        // 创建输入流
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());

        // 接收文件名信息
        String fileName =inputStream.readUTF();

        // 接收备份标识
        String backupTag = inputStream.readUTF();
        // 文件路径
        String filePath;
        // 传输过来的是备份文件
        if(backupTag.equals("isBackup")){
            filePath=DBLP_Path+"/"+String.valueOf(portSelected)+"/"+fileName;
        }
        // 传输过来的不是备份文件
        else {
            filePath=DBLP_Backup_Path+"/"+String.valueOf(portSelected)+"/"+fileName;
        }

        // 创建文件输出流
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);

        // 读取输入流中的数据并写入文件
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            fileOutputStream.write(buffer, 0, bytesRead);
        }

        // 关闭文件输出流和Socket输入流
        fileOutputStream.close();
        inputStream.close();
    }
}