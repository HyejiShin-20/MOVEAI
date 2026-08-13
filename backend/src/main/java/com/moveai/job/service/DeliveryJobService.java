package com.moveai.job.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moveai.common.ApiException;
import com.moveai.job.dto.DeliveryJobResponse;
import com.moveai.job.entity.DeliveryJob;
import com.moveai.job.repository.DeliveryJobRepository;

@Service
@Transactional(readOnly = true)
public class DeliveryJobService {

    private final DeliveryJobRepository deliveryJobRepository;

    public DeliveryJobService(DeliveryJobRepository deliveryJobRepository) {
        this.deliveryJobRepository = deliveryJobRepository;
    }

    /**
     * 필터 없이 호출하면 전체를 반환한다 (05B §4-3).
     *
     * <p>시연 중 목록에서 건이 사라지면 곤란하다. 상태가 IN_PROGRESS/DONE 으로 바뀌어도
     * 목록에서 계속 보여야 다시 눌러 시작할 수 있다.
     */
    public List<DeliveryJobResponse.Summary> findAll(String status) {
        return deliveryJobRepository.findForList(status).stream()
                .map(job -> new DeliveryJobResponse.Summary(
                        job.getId(),
                        job.getJobCode(),
                        job.getPlace().getId(),
                        job.getPlace().getName(),
                        job.getRecipientLabel(),
                        job.getAddressText(),
                        job.getItemSummary(),
                        job.getStatus()))
                .toList();
    }

    public DeliveryJobResponse.Detail findDetail(Long jobId) {
        DeliveryJob job = deliveryJobRepository.findById(jobId)
                .orElseThrow(() -> ApiException.notFound("DELIVERY_JOB_NOT_FOUND", "배송 건을 찾을 수 없습니다."));

        return new DeliveryJobResponse.Detail(
                job.getId(),
                job.getJobCode(),
                new DeliveryJobResponse.Ref(job.getPlace().getId(), job.getPlace().getName()),
                new DeliveryJobResponse.Ref(
                        job.getDestinationNode().getId(), job.getDestinationNode().getName()),
                job.getRecipientLabel(),
                job.getAddressText(),
                job.getItemSummary(),
                job.getStatus());
    }
}
