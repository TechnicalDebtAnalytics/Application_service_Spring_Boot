package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Super_Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Super_AdminRepository extends JpaRepository<Super_Admin, Long> {

    List<Super_Admin> findByUserUserId(Long userId);

    Optional<Super_Admin> findByUserUserIdAndCompanyCompanyId(Long userId, Long companyId);

    boolean existsByUserUserIdAndCompanyCompanyId(Long userId, Long companyId);

    boolean existsByUserUserId(Long userId);
}
