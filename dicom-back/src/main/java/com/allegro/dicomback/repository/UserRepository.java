package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 상태 값이 활성(True)인 유저만 아이디로 찾기
    Optional<User> findByUserIdAndUserStatusTrue(String userId);

    // 아이디가 있는지 확인
    boolean existsByUserId(String userId);
}