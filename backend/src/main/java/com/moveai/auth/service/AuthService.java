package com.moveai.auth.service;

import com.moveai.auth.dto.RegisterRequest;
import com.moveai.user.entity.AdminProfile;
import com.moveai.user.entity.MemberProfile;
import com.moveai.user.entity.User;
import com.moveai.user.entity.UserRole;
import com.moveai.user.repository.AdminProfileRepository;
import com.moveai.user.repository.MemberProfileRepository;
import com.moveai.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final AdminProfileRepository adminProfiles;
    private final MemberProfileRepository memberProfiles;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository users,
            AdminProfileRepository adminProfiles,
            MemberProfileRepository memberProfiles,
            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.adminProfiles = adminProfiles;
        this.memberProfiles = memberProfiles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("회원가입 요청이 비어 있습니다.");
        }

        String loginId = normalize(request.loginId());
        String name = normalize(request.name());
        String phone = normalize(request.phone());
        String rawPassword = request.password() == null ? "" : request.password();
        String roleName = normalize(request.role());

        if (loginId.isEmpty()) {
            throw new IllegalArgumentException("로그인 아이디를 입력해 주세요.");
        }
        if (users.existsByLoginId(loginId)) {
            throw new IllegalArgumentException("이미 사용 중인 로그인 아이디입니다.");
        }
        if (rawPassword.isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해 주세요.");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("이름을 입력해 주세요.");
        }
        if (phone.isEmpty()) {
            throw new IllegalArgumentException("휴대폰 번호를 입력해 주세요.");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(roleName.isEmpty() ? "MEMBER" : roleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("권한 값이 올바르지 않습니다. (ADMIN 또는 MEMBER)");
        }

        User user = new User(loginId, passwordEncoder.encode(rawPassword), name, phone, role);
        User savedUser = users.save(user);

        if (role == UserRole.ADMIN) {
            String companyName = normalize(request.companyName());
            String position = normalize(request.position());
            if (companyName.isEmpty() || position.isEmpty()) {
                throw new IllegalArgumentException("관리자 회사명과 직책은 필수입니다.");
            }
            adminProfiles.save(new AdminProfile(savedUser, companyName, position));
        } else {
            String address = normalize(request.address());
            String career = normalize(request.career());
            String affiliation = normalize(request.affiliation());
            memberProfiles.save(new MemberProfile(savedUser, address, career, affiliation));
        }

        return savedUser;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
