package com.example.server5.domain.repository;

import com.example.server5.domain.entity.User;
import com.example.server5.dto.UserSummaryDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // ✅ JPA 최적화 1: DTO Projection - 필요한 컬럼만 SELECT (엔티티 전체 로드 불필요)
    @Query("SELECT new com.example.server5.dto.UserSummaryDto(u.id, u.name, u.email) FROM User u")
    List<UserSummaryDto> findAllSummary();

    // ✅ JPA 최적화 2: FETCH JOIN - N+1 문제 해결 (User + Orders 한 번에 조회)
    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.orders WHERE u.id = :id")
    Optional<User> findByIdWithOrders(@Param("id") Long id);

    // ✅ JPA 최적화 3: 벌크 업데이트 (개별 UPDATE 대신 한 번에)
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE User u SET u.name = :name WHERE u.id = :id")
    int bulkUpdateName(@Param("id") Long id, @Param("name") String name);
}
