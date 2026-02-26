package com.groupSWP.centralkitchenplatform.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) throws IOException {
        // Tham số "folder" giúp bạn gom nhóm ảnh trên Cloudinary
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", "central_kitchen_products"
        ));
        // Trả về URL an toàn (https) của ảnh
        return uploadResult.get("secure_url").toString();
    }
}