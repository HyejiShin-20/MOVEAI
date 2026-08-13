package com.moveai.job.entity;

import com.moveai.place.entity.Place;
import com.moveai.place.entity.PlaceNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * 배송 건. <b>Route 선택의 입력을 제공하는 최소 껍데기</b>이며 배송 관리 기능으로 키우지 않는다
 * (04 §11-8).
 *
 * <p>{@code destinationNode} 가 경로 선택 1단계의 입력이다 (04 §11-3).
 */
@Entity
@Table(name = "delivery_jobs")
public class DeliveryJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_code", nullable = false)
    private String jobCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_node_id")
    private PlaceNode destinationNode;

    @Column(name = "recipient_label")
    private String recipientLabel;

    @Column(name = "address_text")
    private String addressText;

    @Column(name = "item_summary")
    private String itemSummary;

    @Column(nullable = false)
    private String status;

    protected DeliveryJob() {}

    public Long getId() {
        return id;
    }

    public String getJobCode() {
        return jobCode;
    }

    public Place getPlace() {
        return place;
    }

    public PlaceNode getDestinationNode() {
        return destinationNode;
    }

    public String getRecipientLabel() {
        return recipientLabel;
    }

    public String getAddressText() {
        return addressText;
    }

    public String getItemSummary() {
        return itemSummary;
    }

    public String getStatus() {
        return status;
    }
}
