package com.stockpicks.backend.service;

import com.stockpicks.backend.entity.UserFile;
import com.stockpicks.backend.repository.UserFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    @Autowired
    private UserFileRepository userFileRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public UserFile uploadFile(MultipartFile file, String description, String uploaderEmail) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        // Validate file type (Excel files only)
        String contentType = file.getContentType();
        if (!isExcelFile(contentType)) {
            throw new IllegalArgumentException("Only Excel files are allowed (.xlsx, .xls)");
        }

        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

        // Save file to disk
        Path filePath = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Save file metadata to database
        UserFile userFile = new UserFile(
            uniqueFilename,
            originalFilename,
            contentType,
            file.getSize(),
            filePath.toString(),
            description,
            uploaderEmail
        );

        return userFileRepository.save(userFile);
    }

    public List<UserFile> getAllActiveFiles() {
        return userFileRepository.findAllActiveFiles();
    }

    public UserFile getFileById(Long id) {
        return userFileRepository.findById(id).orElse(null);
    }

    public Resource loadFileAsResource(String filename) throws MalformedURLException {
        Path filePath = Paths.get(uploadDir).resolve(filename);
        Resource resource = new UrlResource(filePath.toUri());
        
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("File not found: " + filename);
        }
    }

    public void deleteFile(Long id) {
        UserFile userFile = userFileRepository.findById(id).orElse(null);
        if (userFile != null) {
            // Soft delete - mark as inactive
            userFile.setActive(false);
            userFileRepository.save(userFile);
            
            // Optionally delete physical file
            try {
                Path filePath = Paths.get(userFile.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log error but don't fail the deletion
                System.err.println("Error deleting physical file: " + e.getMessage());
            }
        }
    }

    private boolean isExcelFile(String contentType) {
        return contentType != null && (
            contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") || // .xlsx
            contentType.equals("application/vnd.ms-excel") // .xls
        );
    }
}