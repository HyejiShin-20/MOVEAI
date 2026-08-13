package com.moveai.user.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "admin_profiles")
public class AdminProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(nullable = false)
    private String position;

    protected AdminProfile() {}

    public AdminProfile(User user, String companyName, String position) {
        this.user = user;
        this.userId = user.getId();
        this.companyName = companyName;
        this.position = position;
    }

    public Long getUserId() { return userId; }
    public User getUser() { return user; }
    public String getCompanyName() { return companyName; }
    public String getPosition() { return position; }
}
