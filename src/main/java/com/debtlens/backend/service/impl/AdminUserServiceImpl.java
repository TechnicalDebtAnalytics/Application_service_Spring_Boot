package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.response.AdminUserAffiliationDTO;
import com.debtlens.backend.dto.response.AdminUserResponseDTO;
import com.debtlens.backend.entity.Member;
import com.debtlens.backend.entity.Super_Admin;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.repository.MemberRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import com.debtlens.backend.repository.UserRepository;
import com.debtlens.backend.service.AdminUserService;
import com.debtlens.backend.exception.BadRequestException;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AdminUserServiceImpl implements AdminUserService {
    private final UserRepository userRepository;
    private final Super_AdminRepository superAdminRepository;
    private final MemberRepository memberRepository;

    public AdminUserServiceImpl(UserRepository userRepository, Super_AdminRepository superAdminRepository,
                                MemberRepository memberRepository) {
        this.userRepository = userRepository;
        this.superAdminRepository = superAdminRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponseDTO> getAllUsers(String query, Long companyId, String companyRole,
                                                   Boolean emailVerified, Pageable pageable) {
        if (companyRole != null && !companyRole.isBlank()
                && !companyRole.equalsIgnoreCase("SUPER_ADMIN") && !companyRole.equalsIgnoreCase("MEMBER")) {
            throw new BadRequestException("Unknown company role: " + companyRole);
        }
        return mapUsers(userRepository.findAll(
                userSpecification(query, companyId, companyRole, emailVerified), pageable), null);
    }

    public Page<AdminUserResponseDTO> mapUsers(Page<User> page, Long restrictToCompanyId) {
        List<Long> userIds = page.getContent().stream().map(User::getUserId).toList();
        Map<Long, Map<Long, AdminUserAffiliationDTO>> affiliations = new LinkedHashMap<>();
        if (!userIds.isEmpty()) {
            for (Super_Admin admin : superAdminRepository.findByUserUserIdIn(userIds)) {
                Long companyId = admin.getCompany().getCompanyId();
                if (restrictToCompanyId == null || restrictToCompanyId.equals(companyId)) {
                    affiliations.computeIfAbsent(admin.getUser().getUserId(), ignored -> new LinkedHashMap<>())
                            .put(companyId, new AdminUserAffiliationDTO(companyId,
                                    admin.getCompany().getCompanyName(), "Super Admin"));
                }
            }
            for (Member member : memberRepository.findByUserUserIdIn(userIds)) {
                Long companyId = member.getCompany().getCompanyId();
                if (restrictToCompanyId == null || restrictToCompanyId.equals(companyId)) {
                    affiliations.computeIfAbsent(member.getUser().getUserId(), ignored -> new LinkedHashMap<>())
                            .putIfAbsent(companyId, new AdminUserAffiliationDTO(companyId,
                                    member.getCompany().getCompanyName(), "Member"));
                }
            }
        }

        List<AdminUserResponseDTO> content = page.getContent().stream().map(user -> {
            List<AdminUserAffiliationDTO> values = new ArrayList<>(
                    affiliations.getOrDefault(user.getUserId(), Map.of()).values());
            values.sort(Comparator.comparing(AdminUserAffiliationDTO::companyName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
            return new AdminUserResponseDTO(user.getUserId(), user.getFirstName(), user.getLastName(),
                    user.getEmail(), user.getGithubUsername(), user.getEmailVerified(), values, user.getCreatedAt());
        }).toList();
        return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
    }

    public static Specification<User> userSpecification(String query, Long companyId, String companyRole,
                                                         Boolean emailVerified) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("githubUsername")), pattern)));
            }
            if (emailVerified != null) predicates.add(cb.equal(root.get("emailVerified"), emailVerified));

            String role = companyRole == null ? "" : companyRole.trim().toUpperCase(Locale.ROOT);
            if (companyId != null || !role.isBlank()) {
                List<Predicate> rolePredicates = new ArrayList<>();
                if (role.isBlank() || role.equals("SUPER_ADMIN")) {
                    Subquery<Long> sq = criteriaQuery.subquery(Long.class);
                    var admin = sq.from(Super_Admin.class);
                    List<Predicate> conditions = new ArrayList<>();
                    conditions.add(cb.equal(admin.get("user").get("userId"), root.get("userId")));
                    if (companyId != null) conditions.add(cb.equal(admin.get("company").get("companyId"), companyId));
                    sq.select(admin.get("superAdminId")).where(conditions.toArray(Predicate[]::new));
                    rolePredicates.add(cb.exists(sq));
                }
                if (role.isBlank() || role.equals("MEMBER")) {
                    Subquery<Long> sq = criteriaQuery.subquery(Long.class);
                    var member = sq.from(Member.class);
                    List<Predicate> conditions = new ArrayList<>();
                    conditions.add(cb.equal(member.get("user").get("userId"), root.get("userId")));
                    if (companyId != null) conditions.add(cb.equal(member.get("company").get("companyId"), companyId));
                    sq.select(member.get("memberId")).where(conditions.toArray(Predicate[]::new));
                    rolePredicates.add(cb.exists(sq));
                }
                predicates.add(rolePredicates.isEmpty() ? cb.disjunction() : cb.or(rolePredicates.toArray(Predicate[]::new)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
