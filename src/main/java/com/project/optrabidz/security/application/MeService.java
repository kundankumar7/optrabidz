package com.project.optrabidz.security.application;

import com.project.optrabidz.common.api.exception.ApiException;
import com.project.optrabidz.common.api.exception.ErrorCode;
import com.project.optrabidz.identity.application.port.IdentityQueryPort;
import com.project.optrabidz.identity.application.query.AccountSnapshot;
import com.project.optrabidz.identity.domain.model.RoleType;
import com.project.optrabidz.participation.domain.repository.AdminRepository;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import com.project.optrabidz.security.application.dto.response.MeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeService {
    private final IdentityQueryPort identityQueryPort;
    private final StartupRepository startupRepository;
    private final InvestorRepository investorRepository;
    private final AdminRepository adminRepository;

    public MeService(IdentityQueryPort identityQueryPort,
                     StartupRepository startupRepository,
                     InvestorRepository investorRepository,
                     AdminRepository adminRepository) {
        this.identityQueryPort = identityQueryPort;
        this.startupRepository = startupRepository;
        this.investorRepository = investorRepository;
        this.adminRepository = adminRepository;
    }

    @Transactional(readOnly = true)
    public MeResponse getCurrentUser(AuthenticatedUserPrincipal principal) {
        AccountSnapshot account = identityQueryPort.findAccountById(principal.getAccountId())
                .orElseThrow(() -> new ApiException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Authenticated account was not found"
                ));

        RoleType role = account.roleType();
        return new MeResponse(
                role,
                account.accountState(),
                account.profileStatus(),
                role,
                actorExists(account.accountId(), role)
        );
    }

    private boolean actorExists(Long accountId, RoleType roleType) {
        return switch (roleType) {
            case STARTUP -> startupRepository.existsByAccountId(accountId);
            case INVESTOR -> investorRepository.existsByAccountId(accountId);
            case ADMIN -> adminRepository.existsActiveByAccountId(accountId);
        };
    }
}
