package com.atguigu.gulimall.thirdparty;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.PutObjectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

@SpringBootTest
class ThirdPartyApplicationTests {

    @Autowired
    OSSClient ossClient;

    @Test
    public void testUpload() throws ClientException, com.aliyuncs.exceptions.ClientException {
        // 填写Bucket名称，例如examplebucket。
        String bucketName = "gulimall-oos-zyl";

        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
        String objectName = "1234-new.png";
        // 填写本地文件的完整路径
        String filePath= "/Users/zhaoyunlong/Desktop/截图/iShot_2025-12-12_15.12.59.png";


        try {
            // 创建PutObjectRequest对象。
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, new File(filePath));

            // 上传文件。
            ossClient.putObject(putObjectRequest);
            System.out.println("上传完成");
        } catch (Exception oe) {
            oe.printStackTrace();
        }
    }

}
