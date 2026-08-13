package com.moveai.report.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.moveai.ai.stt.SttClient;
import com.moveai.common.ApiException;
import com.moveai.place.repository.PlaceRepository;
import com.moveai.report.dto.ReportResponse;
import com.moveai.report.entity.FieldReport;
import com.moveai.report.entity.ReportAudioFile;
import com.moveai.report.repository.FieldReportRepository;
import com.moveai.report.repository.ReportAudioFileRepository;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final FieldReportRepository fieldReportRepository;
    private final ReportAudioFileRepository reportAudioFileRepository;
    private final PlaceRepository placeRepository;
    private final SttClient sttClient;
    private final TransactionTemplate transactionTemplate;
    private final Path audioStoragePath;

    public ReportService(
            FieldReportRepository fieldReportRepository,
            ReportAudioFileRepository reportAudioFileRepository,
            PlaceRepository placeRepository,
            SttClient sttClient,
            TransactionTemplate transactionTemplate,
            @Value("${moveai.audio.storage-path}") String audioStoragePath) {
        this.transactionTemplate = transactionTemplate;
        this.fieldReportRepository = fieldReportRepository;
        this.reportAudioFileRepository = reportAudioFileRepository;
        this.placeRepository = placeRepository;
        this.sttClient = sttClient;
        this.audioStoragePath = Path.of(audioStoragePath).toAbsolutePath().normalize();
    }

    /**
     * 녹음 업로드 → STT → 제보 생성.
     *
     * <p>STT 는 외부 호출이라 DB 트랜잭션 밖에서 먼저 끝낸다. 느린 호출을 트랜잭션 안에 두면
     * 커넥션을 잡은 채 수십 초를 기다리게 된다.
     */
    public ReportResponse.Created createFromAudio(
            Long placeId, Long selectedScopeNodeId, MultipartFile audio) {
        if (!placeRepository.existsById(placeId)) {
            throw ApiException.notFound("PLACE_NOT_FOUND", "장소를 찾을 수 없습니다.");
        }
        if (audio == null || audio.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AUDIO_FILE", "음성 파일이 비어 있습니다.");
        }

        byte[] bytes = readBytes(audio);
        String fileName = safeFileName(audio.getOriginalFilename());
        String contentType = audio.getContentType() == null ? "audio/wav" : audio.getContentType();

        SttClient.SttResult result = sttClient.transcribe(bytes, fileName, contentType);
        Path storedPath = store(bytes, fileName);

        return save(placeId, selectedScopeNodeId, result, storedPath, contentType);
    }

    /** 텍스트 직접 입력. STT 가 막혔을 때의 축소 경로다 (05C §7 Phase 5). */
    public ReportResponse.Created createFromText(Long placeId, Long selectedScopeNodeId, String text) {
        if (!placeRepository.existsById(placeId)) {
            throw ApiException.notFound("PLACE_NOT_FOUND", "장소를 찾을 수 없습니다.");
        }
        FieldReport report = fieldReportRepository.save(
                new FieldReport(placeId, selectedScopeNodeId, "TEXT", text));
        return new ReportResponse.Created(report.getId(), report.getRawSttText());
    }

    /** 같은 빈 안에서 부르므로 프록시가 걸리지 않는다. 트랜잭션을 명시적으로 연다. */
    private ReportResponse.Created save(
            Long placeId,
            Long selectedScopeNodeId,
            SttClient.SttResult result,
            Path storedPath,
            String contentType) {
        return transactionTemplate.execute(status -> {
            FieldReport report = fieldReportRepository.save(
                    new FieldReport(placeId, selectedScopeNodeId, "VOICE", result.text()));
            reportAudioFileRepository.save(new ReportAudioFile(
                    report.getId(), storedPath.toString(), contentType, result.durationMs()));
            return new ReportResponse.Created(report.getId(), report.getRawSttText());
        });
    }

    @Transactional
    public ReportResponse.Transcript updateTranscript(Long reportId, String correctedText) {
        FieldReport report = fieldReportRepository.findById(reportId)
                .orElseThrow(() -> ApiException.notFound("REPORT_NOT_FOUND", "제보를 찾을 수 없습니다."));
        report.correctTranscript(correctedText);
        return new ReportResponse.Transcript(report.getId(), report.getCorrectedSttText());
    }

    private byte[] readBytes(MultipartFile audio) {
        try {
            return audio.getBytes();
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AUDIO_FILE", "음성 파일을 읽지 못했습니다.");
        }
    }

    private Path store(byte[] bytes, String fileName) {
        try {
            Path directory = audioStoragePath.resolve(LocalDate.now().toString());
            Files.createDirectories(directory);
            Path target = directory.resolve(UUID.randomUUID() + "_" + fileName);
            Files.write(target, bytes);
            return target;
        } catch (IOException exception) {
            log.warn("audio store failed: error_type={}", exception.getClass().getSimpleName());
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "AUDIO_STORE_FAILED", "음성 파일 저장에 실패했습니다.");
        }
    }

    /** 업로드 파일명을 그대로 경로에 쓰지 않는다. */
    private static String safeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "recording.wav";
        }
        String name = Path.of(original).getFileName().toString();
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
