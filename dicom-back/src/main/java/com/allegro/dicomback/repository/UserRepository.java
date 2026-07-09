package com.allegro.dicomback.repository;

import com.allegro.dicomback.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * {@link User} 엔티티를 위한 레포지토리 인터페이스입니다.
 * 표준 CRUD 및 사용자 정의 쿼리 작업을 제공하기 위해 JpaRepository를 확장합니다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 사용자 ID로 활성 사용자를 찾습니다.
     * 상태가 true(활성)인 경우에만 사용자를 반환합니다.
     *
     * @param userId 검색할 사용자 ID
     * @return 활성 {@link User}를 포함하는 {@link Optional}, 찾을 수 없거나 비활성 상태이면 비어 있음
     */
    Optional<User> findByUserIdAndUserStatusTrue(String userId);

    /**
     * 주어진 사용자 ID로 사용자가 존재하는지 확인합니다.
     *
     * @param userId 확인할 사용자 ID
     * @return 해당 ID의 사용자가 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByUserId(String userId);
}