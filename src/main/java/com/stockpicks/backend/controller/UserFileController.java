package com.stockpicks.backend.controller;

import com.stockpicks.backend.entity.UserFile;
import com.stockpicks.backend.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class UserFileController {

    @Autowired
    private FileService fileService;

    @GetMapping
    public ResponseEntity<List<UserFile>> getAllFiles(Authentication authentication) {
        // Ensure user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        try {
            List<UserFile> files = fileService.getAllActiveFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id, Authentication authentication, @RequestParam(required = false) String token) {
        // Ensure user is authenticated
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        try {
            UserFile userFile = fileService.getFileById(id);
            if (userFile == null || !userFile.isActive()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = fileService.loadFileAsResource(userFile.getFilename());
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(userFile.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + userFile.getOriginalFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
}