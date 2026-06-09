//package com.starrysky.aries.util.upload;
//
//import com.starrysky.aries.entity.User;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * FileUploadController
// * 文件上传控制器
// *
// * @author adice
// * @version 1.0
// * @since 2025-07-01 11:07:49
// */
//@RestController
//@Slf4j
//public class FileUploadController {
//    @Resource
//    private FileUploadUtil fileUploadUtil;
//
//    @PostMapping("/upload/pic")
//    public List<FileUploadResponse> uploadPic(@RequestParam(value = "uploadfiles") MultipartFile[] files,
//                                              @AuthenticationPrincipal User user) {
//        List<FileUploadResponse> list = new ArrayList<>();
//        String middlePath = user.getUsername();
//        try {
//            for (MultipartFile file : files) {
//                list.add(fileUploadUtil.uploadPic(middlePath, file));
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        return list;
//    }
//
//
//    @PostMapping("/upload/video")
//    public List<FileUploadResponse> uploadVideo(@RequestParam(value = "uploadfiles") MultipartFile[] files,
//                                                @AuthenticationPrincipal User user) {
//        List<FileUploadResponse> list = new ArrayList<>();
//        String middlePath = user.getUsername();
//        try {
//            for (MultipartFile file : files) {
//                list.add(fileUploadUtil.uploadVideo(middlePath, file));
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        return list;
//    }
//
//}
