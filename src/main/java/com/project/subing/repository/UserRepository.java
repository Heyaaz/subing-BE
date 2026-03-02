package com.project.subing.repository;

import com.project.subing.domain.user.entity.User;
import com.project.subing.domain.user.entity.UserTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByTier(UserTier tier);

    @Query("SELECT FUNCTION('TO_CHAR', u.createdAt, 'yyyy-MM') AS month, COUNT(u) " +
           "FROM User u WHERE u.createdAt >= :since GROUP BY FUNCTION('TO_CHAR', u.createdAt, 'yyyy-MM')")
    List<Object[]> countUsersByMonthSince(@Param("since") LocalDateTime since);
}
