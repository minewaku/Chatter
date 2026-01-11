package com.minewaku.chatter.adapter.mapper;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.minewaku.chatter.adapter.exception.FileUploadException;
import com.minewaku.chatter.domain.value.file.InputImage;

@Component
public class MultipartFileMapper {
    public InputImage toInputImage(MultipartFile file) {
        try {
            return new InputImage(
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getInputStream()
            );
        } catch (IOException e) {
            throw new FileUploadException("Cannot read file content", e);
        }
    }
}
