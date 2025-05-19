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
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:./uploads}") String uploadDir) throws IOException {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
        System.out.println("Diretório de upload inicializado em: " + this.fileStorageLocation);
    }

    public String storeFile(MultipartFile file) throws IOException {
        // Verificação de segurança para o nome do arquivo
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new IOException("Nome de arquivo inválido: " + originalFileName);
        }

        // Gerar nome único para o arquivo (UUID + extensão original)
        String fileExtension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileExtension = originalFileName.substring(lastDotIndex);
        }

        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName);

        System.out.println("Salvando arquivo em: " + targetLocation);

        // Copia o arquivo para o destino
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // MUITO IMPORTANTE: Retorna o caminho correto para ser armazenado no banco de dados
        // Este caminho deve ser consistente com sua configuração de recursos estáticos
        return "/uploads/" + uniqueFileName;
    }

    public Resource loadFileAsResource(String fileName) throws MalformedURLException {
        try {
            // Remove qualquer prefixo de caminho como "/uploads/" se presente
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
}