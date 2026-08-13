package com.moveai.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moveai.report.entity.ReportAudioFile;

public interface ReportAudioFileRepository extends JpaRepository<ReportAudioFile, Long> {}
