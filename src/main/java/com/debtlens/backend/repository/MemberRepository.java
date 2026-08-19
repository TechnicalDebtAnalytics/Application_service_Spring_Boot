package com.debtlens.backend.repository;

import com.debtlens.backend.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    List<Member> findByUserUserId(Long userId);

    List<Member> findByCompanyCompanyId(Long companyId);

    Optional<Member> findByUserUserIdAndCompanyCompanyId(
            Long userId,
            Long companyId
    );

    boolean existsByUserUserIdAndCompanyCompanyId(
            Long userId,
            Long companyId
    );
}
