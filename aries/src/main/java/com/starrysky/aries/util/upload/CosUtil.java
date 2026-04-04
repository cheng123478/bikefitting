package com.starrysky.aries.util.upload;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.StorageClass;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferManagerConfiguration;
import com.qcloud.cos.transfer.Upload;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 上传代码到云端
 *
 * @author adi
 * @version 1.0
 * @since 2025/12/4 11:26
 */

@Component
@Slf4j
public class CosUtil {
    @Value("${txCloud.cos.secretId}")
    private String secretId;
    @Value("${txCloud.cos.secretKey}")
    private String secretKey;
    @Value("${txCloud.cos.bucket}")
    private String bucket;
    @Value("${txCloud.cos.region}")
    private String region;

    private COSClient cosClient;
    private TransferManager transferManager;
    private ExecutorService threadPool;

    @PostConstruct
    public void init() {
        log.info("正在初始化腾讯云 COS 客户端...");
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setRegion(new Region(region));
        // 1. 初始化基础客户端
        this.cosClient = new COSClient(cred, clientConfig);
        // 2. 初始化线程池 (根据业务量调整核心线程数)
        this.threadPool = Executors.newFixedThreadPool(16);
        // 3. 初始化传输管理器
        this.transferManager = new TransferManager(cosClient, threadPool);
        // 4. 配置分块上传参数
        TransferManagerConfiguration conf = new TransferManagerConfiguration();
        conf.setMultipartUploadThreshold(5 * 1024 * 1024); // 5MB以上开启分块
        conf.setMinimumUploadPartSize(1024 * 1024);    // 分块最小1MB
        this.transferManager.setConfiguration(conf);
        log.info("腾讯云 COS 客户端初始化完成。");
    }

    @PreDestroy
    public void shutdown() {
        if (transferManager != null) {
            // 参数为 true 表示同时关闭底层的 cosClient
            transferManager.shutdownNow(true);
            log.info("腾讯云 COS 传输管理器已关闭。");
        }
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }

    /**
     * 上传文件
     *
     * @param file 本地文件
     * @param key  对象在存储桶中的唯一标识
     * @return 文件访问地址
     */
    public String upload(File file, String key) {
        PutObjectRequest request = new PutObjectRequest(bucket, key, file);
        return executeUpload(request, key);
    }

    /**
     * 上传输入流
     *
     * @param inputStream  文件输入流
     * @param streamLength 文件长度
     * @param key          象在存储桶中的唯一标识
     * @return 文件访问地址
     */
    public String upload(InputStream inputStream, long streamLength, String key) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(streamLength);
        PutObjectRequest request = new PutObjectRequest(bucket, key, inputStream, metadata);
        return executeUpload(request, key);
    }

    /**
     * 执行上传任务的通用逻辑
     *
     * @param request 上传请求
     * @param key     文件在存储桶中的完整路径
     * @return 文件访问地址
     */
    private String executeUpload(PutObjectRequest request, String key) {
        request.setStorageClass(StorageClass.Standard);
        try {
            Upload upload = transferManager.upload(request);
            // 同步等待结果
            upload.waitForUploadResult();
            log.info("文件上传成功: {}", key);
            return getUrl(key);
        } catch (Exception e) {
            log.error("COS 文件上传异常! Key: {}", key, e);
            throw new RuntimeException("文件上传至云端失败");
        }
    }

    /**
     * 删除文件
     *
     * @param key 文件在存储桶中的完整路径
     */
    public void delete(String key) throws CosClientException {
        try {
            cosClient.deleteObject(bucket, key);
            log.info("文件删除成功: {}", key);
        } catch (Exception e) {
            log.error("COS 文件删除失败! Key: {}", key, e);
        }
    }


    /**
     * 获取文件访问地址
     *
     * @param key 文件在存储桶中的完整路径
     * @return 文件访问地址
     */
    public String getUrl(String key) {
        // key 规范化：如果以 / 开头则去掉，防止生成两个 //
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        return cosClient.getObjectUrl(bucket, key).toString();
    }

}


