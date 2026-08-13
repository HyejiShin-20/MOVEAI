package com.moveai.report.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class AudioStorageService {
    private final Path audioRoot;

    public AudioStorageService(
            @Value("${move-ai.storage.root}") String root,
            @Value("${move-ai.storage.audio-directory}") String audioDirectory) throws IOException {
        this.audioRoot = Paths.get(root).resolve(audioDirectory).toAbsolutePath().normalize();
        Files.createDirectories(this.audioRoot);
    }

    public StoredAudio save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("음성 파일이 비어 있습니다.");
        String original = file.getOriginalFilename() == null ? "audio" : file.getOriginalFilename();
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot).replaceAll("[^A-Za-z0-9.]", "");
        String storedName = UUID.randomUUID() + ext;
        Path target = audioRoot.resolve(storedName).normalize();
        if (!target.startsWith(audioRoot)) throw new IllegalArgumentException("잘못된 파일 경로입니다.");
        file.transferTo(target);
        return new StoredAudio(storedName, original, file.getSize(), file.getContentType());
    }
}
