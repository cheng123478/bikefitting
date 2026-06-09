package com.starrysky.aries.util.upload;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.file.FileNameUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * FileUploadUtil
 * 文件上传工具类
 *
 * @author adice
 * @version 1.0
 * @since 2025-07-01 11:07:49
 */
@Component
public class FileUploadUtil {
    /**
     * 文件命名的不同方式
     */
    public static final int FILE_NAME_TIME = 1;
    public static final int FILE_NAME_UUID = 2;
    public static final int FILE_NAME_ORIGINAL = 3;

    @Value("${file.resource.location}")
    private String uploadPath;

    /**
     * 上传图片
     *
     * @param file 上传的文件
     * @return 上传后文件的url等信息
     */
    public FileUploadResult uploadPic(String middlePath, MultipartFile file) throws IOException {
        String type = FileTypeUtil.getType(file.getInputStream());
        if ("bmp".equals(type) || "jpg".equals(type) || "png".equals(type) || "gif".equals(type) || "jpeg".equals(type)) {
            return uploadFile(middlePath, file, FILE_NAME_UUID);
        } else {
            FileUploadResult fileUploadResult = new FileUploadResult();
            fileUploadResult.setResult(false);
            fileUploadResult.setMessage("上传图片仅支持bmp、jpg、png、gif、jpeg");
            return fileUploadResult;
        }
    }

    public FileUploadResult uploadPic(String middlePath, byte[] cache) throws IOException {
        if (cache == null || cache.length == 0) {
            FileUploadResult fileUploadResult = new FileUploadResult();
            fileUploadResult.setResult(false);
            fileUploadResult.setMessage("图片数据为空");
            return fileUploadResult;
        }

        String type = FileTypeUtil.getType(new ByteArrayInputStream(cache));
        if ("bmp".equals(type) || "jpg".equals(type) || "png".equals(type) || "gif".equals(type) || "jpeg".equals(type)) {
            File directory = new File(uploadPath + middlePath);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String fileName = nameFileByUUID() + "." + type;
            File localFile = new File(directory.getAbsolutePath() + "/" + fileName);

            java.nio.file.Files.write(localFile.toPath(), cache);

            FileUploadResult fileUploadResult = new FileUploadResult();
            fileUploadResult.setResult(true);
            fileUploadResult.setMessage("上传成功");
            fileUploadResult.setOriginalName(fileName);
            fileUploadResult.setUrl("/" + middlePath + "/" + fileName);
            return fileUploadResult;
        } else {
            FileUploadResult fileUploadResult = new FileUploadResult();
            fileUploadResult.setResult(false);
            fileUploadResult.setMessage("上传图片仅支持bmp、jpg、png、gif、jpeg");
            return fileUploadResult;
        }
    }
    /**
     * 上传视频
     *
     * @param file 上传的文件
     * @return 上传后文件的url等信息
     */
    public FileUploadResult uploadVideo(String middlePath, MultipartFile file) throws IOException {
        String type = FileTypeUtil.getType(file.getInputStream());
        if ("mp4".equals(type) || "avi".equals(type) || "wmv".equals(type)
                || "flv".equals(type) || "3gp".equals(type) || "mov".equals(type)) {
            return uploadFile(middlePath, file, FILE_NAME_TIME);
        } else {
            FileUploadResult fileUploadResult = new FileUploadResult();
            fileUploadResult.setResult(false);
            fileUploadResult.setMessage("上传视频仅支持mp4、avi、wmv、flv、mov、3gp");
            return fileUploadResult;
        }
    }

    /**
     * 上传文件，以月为单位分文件夹存储
     *
     * @param file        上传的文件
     * @param namingStyle 命名方式，目前支持3种：1取当前时间命名，2UUID命名，3上传文件原名命名
     * @return 上传后文件的url
     */
    private FileUploadResult uploadFile(String middlePath, MultipartFile file, int namingStyle) {
        File directory = new File(uploadPath + middlePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        String fileName = file.getOriginalFilename();
        // 获取上传文件后缀
        String suffix = FileNameUtil.extName(file.getOriginalFilename());
        if (namingStyle == FILE_NAME_TIME) {
            fileName = nameFileByTime() + "." + suffix;
        } else if (namingStyle == FILE_NAME_UUID) {
            fileName = nameFileByUUID() + "." + suffix;
        }
        // 指定上传路径
        File localFile = new File(directory.getAbsolutePath() + "/" + fileName);
        // 写入本地文件
        try {
            file.transferTo(localFile);
            FileUploadResult fileUploadResult = new FileUploadResult();
            fileUploadResult.setResult(true);
            fileUploadResult.setMessage("上传成功");
            fileUploadResult.setOriginalName(file.getOriginalFilename());
            fileUploadResult.setUrl("/" + middlePath + "/" + fileName);
            return fileUploadResult;
        } catch (IOException e) {
            FileUploadResult fileUploadResult = new FileUploadResult();
            fileUploadResult.setResult(false);
            fileUploadResult.setMessage("上传失败:" + e.getMessage());
            return fileUploadResult;
        }
    }

    /**
     * 使用当前时间命名
     *
     * @return 文件名
     */
    private static String nameFileByTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 使用UUID命名
     *
     * @return UUID文件名
     */
    private static String nameFileByUUID() {
        String uuid = UUID.randomUUID().toString();
        return uuid.replace("-", "");
    }
}
