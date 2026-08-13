package com.moveai.job.service;

import com.moveai.job.entity.Job;
import com.moveai.job.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public List<Job> findAll() {
        return jobRepository.findAll();
    }

    public Job findById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다: " + id));
    }

    @Transactional
    public Job create(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("작업 이름은 필수입니다.");
        }
        return jobRepository.save(new Job(name));
    }

    @Transactional
    public Job cancel(Long id) {
        Job job = findById(id);
        job.setStatus("CANCELLED");
        job.setProgress(0);
        return jobRepository.save(job);
    }
}
