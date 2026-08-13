package com.moveai.user.entity;
import jakarta.persistence.*;
@Entity @Table(name="users")
public class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
 @Column(name="login_id",nullable=false,unique=true) String loginId;
 @Column(name="password_hash",nullable=false) String passwordHash;
 @Column(nullable=false) String name;
 @Column(nullable=false) String phone;
 @Enumerated(EnumType.STRING) @Column(nullable=false) UserRole role;
 @Column(nullable=false) boolean enabled=true;
 protected User(){}
 public User(String l,String p,String n,String ph,UserRole r){loginId=l;passwordHash=p;name=n;phone=ph;role=r;}
 public String getLoginId(){return loginId;} 
 public String getPasswordHash(){return passwordHash;}
 public String getName(){return name;} 
 public UserRole getRole(){return role;}
 public Long getId(){return id;}
}
