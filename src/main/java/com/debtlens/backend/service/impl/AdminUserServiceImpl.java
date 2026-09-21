package com.debtlens.backend.service.impl;

import com.debtlens.backend.dto.response.AdminUserResponseDTO;
import com.debtlens.backend.entity.Member;
import com.debtlens.backend.entity.Super_Admin;
import com.debtlens.backend.entity.User;
import com.debtlens.backend.repository.MemberRepository;
import com.debtlens.backend.repository.Super_AdminRepository;
import com.debtlens.backend.repository.UserRepository;
import com.debtlens.backend.service.AdminUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final Super_AdminRepository superAdminRepository;
    private final MemberRepository memberRepository;

    public AdminUserServiceImpl(
            UserRepository userRepository,
            Super_AdminRepository superAdminRepository,
            MemberRepository memberRepository
    ) {
        this.userRepository = userRepository;
        this.superAdminRepository = superAdminRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponseDTO> getAllUsers() {

        List<User> users = userRepository.findAll();

        return users.stream()
                .map(user -> {

                    String companyRole = "—";
                    String companyName = "—";

                    /*
                     * Check if the user is a Super Admin
                     * of any company.
                     */
                    List<Super_Admin> superAdminEntries =
                            superAdminRepository.findByUserUserId(
                                    user.getUserId()
                            );

                    if (!superAdminEntries.isEmpty()) {

                        companyRole = "Super Admin";
                        companyName = superAdminEntries
                                .get(0)
                                .getCompany()
                                .getCompanyName();

                    } else {

                        /*
                         * Check if the user is a Member
                         * of any company.
                         */
                        List<Member> memberEntries =
                                memberRepository.findByUserUserId(
                                        user.getUserId()
                                );

                        if (!memberEntries.isEmpty()) {

                            companyRole = "Member";
                            companyName = memberEntries
                                    .get(0)
                                    .getCompany()
                                    .getCompanyName();
                        }
                    }

                    return new AdminUserResponseDTO(
                            user.getUserId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            user.getGithubUsername(),
                            user.getEmailVerified(),
                            companyRole,
                            companyName,
                            user.getCreatedAt()
                    );
                })
                .toList();
    }
}
