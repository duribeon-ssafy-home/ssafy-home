package com.ssafy.home.property.service;

import com.ssafy.home.common.exception.BusinessException;
import com.ssafy.home.common.exception.ErrorCode;
import com.ssafy.home.property.dto.PropertyImageResponse;
import com.ssafy.home.property.dto.PropertyImageUpdateRequest;
import com.ssafy.home.property.entity.DataSource;
import com.ssafy.home.property.entity.Property;
import com.ssafy.home.property.entity.PropertyImage;
import com.ssafy.home.property.repository.PropertyImageRepository;
import com.ssafy.home.property.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PropertyImageService {
    private final PropertyRepository propertyRepository;
    private final PropertyImageRepository propertyImageRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public List<PropertyImageResponse> uploadImages(Long propertyId, Long userId, List<MultipartFile> files){
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(()->new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));

        if (property.getDataSource() == DataSource.PUBLIC) {
            throw new BusinessException(ErrorCode.PUBLIC_PROPERTY_IMAGE_NOT_ALLOWED);
        }

        if (!userId.equals(property.getOwnerId())) {
            throw new BusinessException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }

        int currentCount = propertyImageRepository.countByProperty_PropertyId(propertyId);
        if (currentCount + files.size() > 10) {
            throw new BusinessException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        List<PropertyImageResponse> responses = new ArrayList<>();
        int nextSortOrder = propertyImageRepository.findMaxSortOrderByPropertyId(propertyId) + 1;

        for(MultipartFile file : files){
            String contentType = file.getContentType();
            if(contentType==null || !contentType.startsWith("image/")){
                throw new BusinessException(ErrorCode.INVALID_IMAGE_FORMAT);
            }

            String imageUrl = saveFile(propertyId, file);

            PropertyImage image = PropertyImage.builder()
                    .property(property)
                    .imageUrl(imageUrl)
                    .sortOrder(nextSortOrder++)
                    .build();

            propertyImageRepository.save(image);
            responses.add(PropertyImageResponse.from(image));
        }
        return responses;
    }

    public PropertyImageResponse updateSortOrder(Long propertyId, Long imageId, Long userId, PropertyImageUpdateRequest request) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));

        if (!userId.equals(property.getOwnerId())) {
            throw new BusinessException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }

        PropertyImage target = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        int oldSortOrder = target.getSortOrder();
        int newSortOrder = request.sortOrder();

        if (oldSortOrder != newSortOrder) {
            propertyImageRepository
                    .findByProperty_PropertyIdAndSortOrder(propertyId, newSortOrder)
                    .ifPresent(other -> other.updateSortOrder(oldSortOrder));
            target.updateSortOrder(newSortOrder);
        }

        return PropertyImageResponse.from(target);
    }

    public void deleteImage(Long propertyId, Long imageId, Long userId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROPERTY_NOT_FOUND));

        if (!userId.equals(property.getOwnerId())) {
            throw new BusinessException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }

        PropertyImage image = propertyImageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IMAGE_NOT_FOUND));

        propertyImageRepository.delete(image);
        deleteFile(image.getImageUrl());
    }

    private String saveFile(Long propertyId, MultipartFile file) {
        try {
            Path dirPath = Paths.get(uploadDir, "properties", String.valueOf(propertyId));
            Files.createDirectories(dirPath);

            String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + "." + ext;

            Files.copy(file.getInputStream(), dirPath.resolve(filename));

            return "/images/properties/" + propertyId + "/" + filename;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    private void deleteFile(String imageUrl) {
        try {
            String relativePath = imageUrl.replaceFirst("^/images", "");
            Files.deleteIfExists(Paths.get(uploadDir + relativePath));
        } catch (IOException ignored) {}
    }
}
