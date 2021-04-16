package com.changgou.util;

import com.changgou.file.FastDFSFile;
import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/*****
 * 文件操作
 * 文件上传
 * 文件删除
 * 文件下载
 * 文件信息获取
 * storage信息获取
 * tracker信息获取
 */
public class FastDFSUtil {

    /****
     * 加载Tracker连接信息
     * static 静态代码块在类加载时就执行了
     */
    static{
        try{
            //查找classpath下的文件路径
            String fileName = new ClassPathResource("fdfs_client.conf").getPath();
            //加载Tracker连接信息
            ClientGlobal.init(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
    }

    /****
     * 文件上传
     * @param fastDFSFile 上传的文件信息封装
     */
    public static String[] upload(FastDFSFile fastDFSFile) throws IOException, MyException {
        // 附加参数
        NameValuePair[] meta_list = new NameValuePair[1];
        meta_list[0] = new NameValuePair("author", fastDFSFile.getAuthor());

        // 获取TrackerServer
        TrackerServer trackerServer = getTrackerServer();
        // 获取StorageClient
        StorageClient storageClient = getStorageClient(trackerServer);
        /***
         * 通过StorageClient访问Storage, 实现文件上传，并且获取文件上传后的存储信息
         * 1. 上传文件的字节数组
         * 2. 文件的扩展名 jpg
         * 3. 附加参数 比如 作者名称：wxx
         *
         * uploads[]
         *      uploads[0]: 文件上传到Storage中存储的组的名称
         *      uploads[1]: 文件存储到Storage上的文件名称 ： M00/02/44/ssdfsdf/sf.jpg
         */
        String[] uploads = storageClient.upload_file(fastDFSFile.getContent(), fastDFSFile.getExt(), meta_list);
        return uploads;
    }


    /***
     * 获取文件信息
     * @param groupName 文件的组名
     * @param remoteFileName 文件的存储路径名称，M00/00/00/sdfsdfffsfsfss.jpg
     */
    public static FileInfo getFile(String groupName, String remoteFileName) throws IOException, MyException {
        // 获取TrackerServer
        TrackerServer trackerServer = getTrackerServer();
        // 获取StorageClient
        StorageClient storageClient = getStorageClient(trackerServer);
        // 获取文件信息
        return storageClient.get_file_info(groupName, remoteFileName);
    }


    /***
     * 文件下载
     * @param groupName 文件的组名
     * @param remoteFileName 文件的存储路径名称，M00/00/00/sdfsdfffsfsfss.jpg
     */
    public static InputStream downloadFile(String groupName, String remoteFileName) throws IOException, MyException {
        // 获取TrackerServer
        TrackerServer trackerServer = getTrackerServer();
        // 获取StorageClient
        StorageClient storageClient = getStorageClient(trackerServer);
        // 获取文件信息

        //文件下载，将字节数组转换为InputStream输入流
        byte[] buffer = storageClient.download_file(groupName, remoteFileName);
        return new ByteArrayInputStream(buffer);
    }

    /***
     * 删除文件
     * @param groupName 文件的组名
     * @param remoteFileName 文件的存储路径名称，M00/00/00/sdfsdfffsfsfss.jpg
     */
    public static void deleteFile(String groupName, String remoteFileName) throws IOException, MyException {
        // 获取TrackerServer
        TrackerServer trackerServer = getTrackerServer();
        // 获取StorageClient
        StorageClient storageClient = getStorageClient(trackerServer);
        // 获取文件信息

        // 删除文件
        storageClient.delete_file(groupName, remoteFileName);
    }

    /***
     * 获取Storage信息
     * @return
     * @throws IOException
     */
    public static StorageServer getStorage() throws IOException {
        // 创建一个TrackerClient对象，访问TrackerServer
        TrackerClient trackerClient = new TrackerClient();
        //通过TrackerClient获取TrackerServer的链接对象
        TrackerServer trackerServer = trackerClient.getConnection();

        //获取Storage信息
        return trackerClient.getStoreStorage(trackerServer);
    }

    /**
     * 获取所有Storage组的IP和端口 信息
     * @param groupName
     * @param remoteFileName
     * @return
     */
    public static ServerInfo[] getServerInfo(String groupName, String remoteFileName) throws IOException {
        // 创建一个TrackerClient对象，访问TrackerServer
        TrackerClient trackerClient = new TrackerClient();
        //通过TrackerClient获取TrackerServer的链接对象
        TrackerServer trackerServer = trackerClient.getConnection();

        return trackerClient.getFetchStorages(trackerServer, groupName, remoteFileName);
    }

    /***
     * 获取Tracker信息
     * @return
     */

    public static String getTracker() throws IOException {
        // 获取TrackerServer
        TrackerServer trackerServer = getTrackerServer();

        // Tracker 的IP， HTTP端口
        String ip = trackerServer.getInetSocketAddress().getHostString();
        int tracker_http_port = ClientGlobal.getG_tracker_http_port();
        String url = "http://" + ip + ":" + tracker_http_port;
        return url;
    }

    /****
     * 获取TrackerServer
     * @return
     * @throws IOException
     */
    public static TrackerServer getTrackerServer() throws IOException {
        // 创建一个TrackerClient对象，访问TrackerServer
        TrackerClient trackerClient = new TrackerClient();
        //通过TrackerClient获取TrackerServer的链接对象
        TrackerServer trackerServer = trackerClient.getConnection();
        return trackerServer;
    }

    /****
     * 获取StorageClient
     * @param trackerServer
     * @return
     */
    public static StorageClient getStorageClient(TrackerServer trackerServer){
        //通过TrackerServer的链接信息可以获取Storage的链接信息，创建StorageClient对象存储storage的链接信息
        StorageClient storageClient = new StorageClient(trackerServer, null);
        return storageClient;
    }

    public static void main(String[] args) throws IOException, MyException {
        // 获取文件信息测试
//        FileInfo fileInfo = getFile("group1", "M00/00/00/wKhwimATvf6AdvG6ABbNo5OdfIQ859.JPG");
//        System.out.println(fileInfo.getSourceIpAddr());
//        System.out.println(fileInfo.getFileSize());
//

        //将文件写入本地磁盘中
//        InputStream is = downloadFile("group1", "M00/00/00/wKhwimATvf6AdvG6ABbNo5OdfIQ859.JPG");
//        FileOutputStream os = new FileOutputStream("D:/1.jpg");
//        byte[] buffer = new byte[1024];
//        while(is.read(buffer) != -1){
//            os.write(buffer);
//        }
//        os.flush();
//        os.close();
//        is.close();

        // 删除文件测试
//        deleteFile("group1", "M00/00/00/wKhwimATzt-ARUkLABqyX-tKj7k844.JPG");

        // 获取Storage信息
//        StorageServer storage = getStorage();
//        System.out.println(storage.getInetSocketAddress().getHostName());
//        System.out.println(storage.getInetSocketAddress().getPort());

        // 获取Storage的IP和端口信息
//        ServerInfo[] groups = getServerInfo("group1", "");
//        for(ServerInfo group: groups){
//            System.out.println(group.getIpAddr());
//            System.out.println(group.getPort());
//        }

        // 获取Tracker信息
        System.out.println(getTracker());
    }
}
