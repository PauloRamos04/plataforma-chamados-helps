package com.helps.domain.service;

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
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:./uploads}") String uploadDir) throws IOException {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
        System.out.println("Diretório de upload inicializado em: " + this.fileStorageLocation);
    }

    public String storeFile(MultipartFile file) throws IOException {
        validateFile(file);

        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new IOException("Nome de arquivo inválido: " + originalFileName);
        }

        String fileExtension = getFileExtension(originalFileName);
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/" + uniqueFileName;
    }

    public Resource loadFileAsResource(String fileName) throws MalformedURLException {
        try {
            if (fileName.startsWith("/uploads/")) {
                fileName = fileName.substring("/uploads/".length());
            }

            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return resource;
            } else {
                throw new MalformedURLException("Arquivo não encontrado: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new MalformedURLException("Arquivo não encontrado: " + fileName);
        }
    }

    public void deleteFile(String fileName) throws IOException {
        if (fileName.startsWith("/uploads/")) {
            fileName = fileName.substring("/uploads/".length());
        }

        Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
        Files.deleteIfExists(filePath);
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Arquivo está vazio");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IOException("Arquivo muito grande. Tamanho máximo: 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IOException("Tipo de arquivo não permitido: " + contentType);
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }
}