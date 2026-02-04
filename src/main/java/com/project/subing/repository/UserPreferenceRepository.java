package com.project.subing.repository;

import com.project.subing.domain.preference.entity.UserPreference;
import com.project.subing.domain.preference.enums.ProfileType;
import com.project.subing.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    /**
     * 사용자 ID로 프로필 조회
     */
    Optional<UserPreference> findByUserId(Long userId);

    /**
     * 사용자로 프로필 조회
     */
    Optional<UserPreference> findByUser(User user);

    /**
     * 사용자 프로필 존재 여부 확인
     */
    boolean existsByUserId(Long userId);

    /**
     * 특정 프로필 타입의 사용자 목록 조회
     */
    List<UserPreference> findByProfileType(ProfileType profileType);

    /**
     * 사용자 프로필 Soft Delete
     */
    @Modifying
    @Query("UPDATE UserPreference up SET up.delYn = 'Y' WHERE up.user.id = :userId")
    void softDeleteByUserId(@Param("userId") Long userId);
}
