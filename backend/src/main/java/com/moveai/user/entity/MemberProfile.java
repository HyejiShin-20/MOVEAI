package com.moveai.user.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "member_profiles")
public class MemberProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private String address;

    @Column
    private String career;

    @Column
    private String affiliation;

    protected MemberProfile() {}

    public MemberProfile(User user, String address, String career, String affiliation) {
        this.user = user;
        this.userId = user.getId();
        this.address = address;
        this.career = career;
        this.affiliation = affiliation;
    }

    public Long getUserId() { return userId; }
    public User getUser() { return user; }
    public String getAddress() { return address; }
    public String getCareer() { return career; }
    public String getAffiliation() { return affiliation; }
}
