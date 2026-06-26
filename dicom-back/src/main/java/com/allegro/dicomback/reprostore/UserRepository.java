package com.allegro.dicomback.reprostore;

import com.allegro.dicomback.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 로그인 및 회원 보안 인증용 조회
    Optional<User> findByUserId(String userId);

    List<User> findByUserStatus(Integer userStatus);

    // 아이디 중복 확인 API용
    boolean existsByUserId(String userId);
}