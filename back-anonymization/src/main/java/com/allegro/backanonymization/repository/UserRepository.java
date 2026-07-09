package com.allegro.backanonymization.repository;

import com.allegro.backanonymization.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

/**
 * User 엔티티를 위한 레포지토리 인터페이스입니다.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 사용자 ID와 활성 상태로 사용자를 찾습니다.
     *
     * @param userId 사용자 ID
     * @return 찾은 경우 User를 포함하는 Optional
     */
    Optional<User> findByUserIdAndUserStatusTrue(String userId);

    /**
     * 사용자 ID가 존재하는지 확인합니다.
     *
     * @param userId 사용자 ID
     * @return 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByUserId(String userId);

    /**
     * 사용자 ID로 사용자를 찾습니다.
     *
     * @param userId 사용자 ID
     * @return 찾은 경우 User를 포함하는 Optional
     */
    Optional<User> findByUserId(String userId);

}