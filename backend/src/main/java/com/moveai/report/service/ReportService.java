package com.moveai.report.service;

import com.moveai.report.entity.Report;
import com.moveai.report.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public List<Report> findAll() {
        return reportRepository.findAll();
    }

    public Report findById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다: " + id));
    }

    @Transactional
    public Report create(Long placeId, String title, String content) {
        if (placeId == null) {
            throw new IllegalArgumentException("장소 ID는 필수입니다.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        return reportRepository.save(new Report(placeId, title, content));
    }

    @Transactional
    public Report update(Long id, String title, String content, String status) {
        Report report = findById(id);
        if (title != null && !title.isBlank()) report.setTitle(title);
        if (content != null) report.setContent(content);
        if (status != null && !status.isBlank()) report.setStatus(status);
        return reportRepository.save(report);
    }

    @Transactional
    public Report submit(Long id) {
        Report report = findById(id);
        report.setStatus("SUBMITTED");
        return reportRepository.save(report);
    }
}
