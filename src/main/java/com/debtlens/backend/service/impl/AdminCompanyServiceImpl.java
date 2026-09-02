package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.response.AdminCompanyResponseDTO;
import com.debtlens.backend.entity.Company;
import com.debtlens.backend.repository.CompanyRepository;
import com.debtlens.backend.repository.MemberRepository;
import com.debtlens.backend.repository.RepositoryRepository;
import com.debtlens.backend.service.AdminCompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminCompanyServiceImpl implements AdminCompanyService {

    private final CompanyRepository companyRepository;
    private final MemberRepository memberRepository;
    private final RepositoryRepository repositoryRepository;

    public AdminCompanyServiceImpl(
            CompanyRepository companyRepository,
            MemberRepository memberRepository,
            RepositoryRepository repositoryRepository
    ) {
        this.companyRepository = companyRepository;
        this.memberRepository = memberRepository;
        this.repositoryRepository = repositoryRepository;
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
}