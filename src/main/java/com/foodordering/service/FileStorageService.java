package com.foodordering.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.foodordering.exception.BadRequestException;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Tidak dapat membuat folder upload: " + uploadDir, e);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Pilih file foto terlebih dahulu");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "foto.jpg" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot).toLowerCase();
        }
        if (!ext.matches("\\.(jpg|jpeg|png|webp)$")) {
            throw new BadRequestException("Format foto harus JPG, PNG, atau WEBP");
        }
        String datePart = LocalDate.now().toString();
        String name = "food-" + System.currentTimeMillis() + ext;
        Path folder = root.resolve(datePart);
        try {
            Files.createDirectories(folder);
            Path target = folder.resolve(name);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + datePart + "/" + name;
        } catch (IOException e) {
            throw new BadRequestException("Gagal menyimpan foto: " + e.getMessage());
        }
    }

    public String store(MultipartFile file, String subfolderName) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Pilih file foto terlebih dahulu");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "foto.jpg" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot).toLowerCase();
        }
        if (!ext.matches("\\.(jpg|jpeg|png|webp)$")) {
            throw new BadRequestException("Format foto harus JPG, PNG, atau WEBP");
        }
        String safeName = subfolderName.replaceAll("[^a-zA-Z0-9-_]", "-");
        String name = safeName + "-" + System.currentTimeMillis() + ext;
        try {
            Files.createDirectories(root);
            Path target = root.resolve(name);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + name;
        } catch (IOException e) {
            throw new BadRequestException("Gagal menyimpan foto: " + e.getMessage());
        }
    }

    public void deleteIfUploaded(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
            return;
        }
        try {
            Path target = root.resolve(imageUrl.substring("/uploads/".length())).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * True bila URL upload benar-benar menunjuk file foto yang layak pakai.
     * Stub hasil upload yang hilang atau file PLACEHOLDER 1x1 dianggap tidak layak.
     */
    public boolean isUsableUpload(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
            return false;
        }
        try {
            Path target = root.resolve(imageUrl.substring("/uploads/".length())).normalize();
            return target.startsWith(root) && Files.isRegularFile(target) && Files.size(target) > 1024;
        } catch (IOException e) {
            return false;
        }
    }

    public String storeAvatar(MultipartFile file, long userId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Pilih file foto terlebih dahulu");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "foto.jpg" : file.getOriginalFilename());
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot).toLowerCase();
        }
        if (!ext.matches("\\.(jpg|jpeg|png|webp)$")) {
            throw new BadRequestException("Format foto harus JPG, PNG, atau WEBP");
        }
        String name = "avatar-" + userId + "-" + System.currentTimeMillis() + ext;
        try {
            Files.createDirectories(root);
            Path target = root.resolve(name);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + name;
        } catch (IOException e) {
            throw new BadRequestException("Gagal menyimpan foto: " + e.getMessage());
        }
    }
}