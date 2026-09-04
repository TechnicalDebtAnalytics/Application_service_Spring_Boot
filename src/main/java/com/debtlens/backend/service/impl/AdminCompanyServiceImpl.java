package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.response.AdminCompanyResponseDTO;
import com.debtlens.backend.dto.response.AdminUserResponseDTO;
import com.debtlens.backend.entity.Company;
import com.debtlens.backend.entity.Member;
import com.debtlens.backend.entity.Super_Admin;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.exception.ResourceNotFoundException;
import com.debtlens.backend.repository.CompanyRepository;
import com.debtlens.backend.repository.MemberRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import com.debtlens.backend.service.AdminCompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminCompanyServiceImpl implements AdminCompanyService {

    private final CompanyRepository companyRepository;
    private final MemberRepository memberRepository;
    private final RepositoryRepository repositoryRepository;
    private final Super_AdminRepository superAdminRepository;

    public AdminCompanyServiceImpl(
            CompanyRepository companyRepository,
            MemberRepository memberRepository,
            RepositoryRepository repositoryRepository,
            Super_AdminRepository superAdminRepository
    ) {
        this.companyRepository = companyRepository;
        this.memberRepository = memberRepository;
        this.repositoryRepository = repositoryRepository;
        this.superAdminRepository = superAdminRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminCompanyResponseDTO> getAllCompanies() {

        List<Company> companies = companyRepository.findAll();

        return companies.stream()
                .map(company -> {

                    String superAdminName =
                            company.getCreatedBy().getFirstName()
                                    + " "
                                    + company.getCreatedBy().getLastName();

                    String superAdminEmail =
                            company.getCreatedBy().getEmail();

                    int totalRepositories =
                            repositoryRepository
                                    .findByCompanyCompanyId(
                                            company.getCompanyId()
                                    )
                                    .size();

                    int totalMembers =
                            memberRepository
                                    .findByCompanyCompanyId(
                                            company.getCompanyId()
                                    )
                                    .size();

                    return new AdminCompanyResponseDTO(
                            company.getCompanyId(),
                            company.getCompanyName(),
                            company.getGithubOrganizationUrl(),
                            superAdminName,
                            superAdminEmail,
                            totalRepositories,
                            totalMembers,
                            company.getCreatedAt()
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponseDTO> getCompanyUsers(Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found with ID: " + companyId));

        List<AdminUserResponseDTO> result = new ArrayList<>();
        Set<Long> processedUserIds = new HashSet<>();

        // 1. Super Admins of this company
        List<Super_Admin> superAdmins = superAdminRepository.findByCompanyCompanyId(companyId);
        for (Super_Admin sa : superAdmins) {
            User user = sa.getUser();
            if (user != null && !processedUserIds.contains(user.getUserId())) {
                processedUserIds.add(user.getUserId());
                result.add(new AdminUserResponseDTO(
                        user.getUserId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getGithubUsername(),
                        user.getEmailVerified(),
                        "Super Admin",
                        company.getCompanyName(),
                        sa.getCreatedAt() != null ? sa.getCreatedAt() : user.getCreatedAt()
                ));
            }
        }

        // 2. Members of this company
        List<Member> members = memberRepository.findByCompanyCompanyId(companyId);
        for (Member m : members) {
            User user = m.getUser();
            if (user != null && !processedUserIds.contains(user.getUserId())) {
                processedUserIds.add(user.getUserId());
                result.add(new AdminUserResponseDTO(
                        user.getUserId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getGithubUsername(),
                        user.getEmailVerified(),
                        "Member",
                        company.getCompanyName(),
                        m.getCreatedAt() != null ? m.getCreatedAt() : user.getCreatedAt()
                ));
            }
        }

        return result;
    }
}