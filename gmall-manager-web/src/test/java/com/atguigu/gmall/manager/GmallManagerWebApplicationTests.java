package com.atguigu.gmall.manager;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallManagerWebApplicationTests {

    @Test
    public void contextLoads() throws IOException, MyException {

        // 配置的fdfs的全局链接地址
        String path = this.getClass().getClassLoader().getResource("tracker.conf").getPath();

        ClientGlobal.init(path);

        TrackerClient trackerClient = new TrackerClient();

        TrackerServer trackerServer = trackerClient.getConnection();

        // 通过 trackerServer 获取 storageClient
        StorageClient storageClient = new StorageClient(trackerServer, null);

        String[] uploadFile = storageClient.upload_file("C:\\Users\\zkd\\Pictures\\Camera Roll\\sp20200820_233044_078.png", "png", null);

        String url = "http://192.168.17.128";

        url += StringUtils.join(uploadFile, "/");

        System.out.println(url);
    }

}
