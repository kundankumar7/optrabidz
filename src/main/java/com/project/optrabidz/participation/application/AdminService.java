package com.project.optrabidz.participation.application;

import com.project.optrabidz.participation.application.exception.ActiveAdminAlreadyExistsException;
import com.project.optrabidz.participation.application.exception.ActiveAdminNotFoundException;
import com.project.optrabidz.participation.application.exception.AdminAuthorityAlreadyGrantedException;
import com.project.optrabidz.participation.application.port.AdminAuthorityQueryPort;
import com.project.optrabidz.participation.application.port.AdminProvisioningPort;
import com.project.optrabidz.participation.domain.model.Admin;
import com.project.optrabidz.participation.domain.repository.AdminRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Optional;

@Service
public class AdminService implements AdminProvisioningPort, AdminAuthorityQueryPort {
    private final AdminRepository adminRepository;

    public AdminService(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional
    public Admin createActiveAdmin(Long accountId, String publicDisplayName, String organizationLabel) {
        Assert.notNull(accountId, "accountId must not be null");

        if (adminRepository.existsActiveAdmin()) {
            throw new ActiveAdminAlreadyExistsException();
        }

        if (adminRepository.existsByAccountId(accountId)) {
            throw new AdminAuthorityAlreadyGrantedException(accountId);
        }

        return adminRepository.save(Admin.grant(accountId, publicDisplayName, organizationLabel));
    }

    @Override
    @Transactional
    public Admin revokeActiveAdmin(Long revokedByAccountId, String reason) {
        Admin activeAdmin = adminRepository.findActiveAdmin()
                .orElseThrow(ActiveAdminNotFoundException::new);

        activeAdmin.revoke(revokedByAccountId, reason);
        return adminRepository.save(activeAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean activeAdminExists() {
        return adminRepository.existsActiveAdmin();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveAdmin(Long accountId) {
        return accountId != null && adminRepository.existsActiveByAccountId(accountId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Admin> findActiveAdmin() {
        return adminRepository.findActiveAdmin();
    }
}
