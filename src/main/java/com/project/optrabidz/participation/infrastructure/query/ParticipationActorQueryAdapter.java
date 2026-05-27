package com.project.optrabidz.participation.infrastructure.query;

import com.project.optrabidz.classification.application.port.out.ParticipationActorQueryPort;
import com.project.optrabidz.participation.domain.repository.InvestorRepository;
import com.project.optrabidz.participation.domain.repository.StartupRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ParticipationActorQueryAdapter implements ParticipationActorQueryPort {
    private final StartupRepository startupRepository;
    private final InvestorRepository investorRepository;

    public ParticipationActorQueryAdapter(StartupRepository startupRepository,
                                          InvestorRepository investorRepository) {
        this.startupRepository = startupRepository;
        this.investorRepository = investorRepository;
    }

    @Override
    public Optional<Long> findStartupIdByAccountId(Long accountId) {
        return startupRepository.findByAccountId(accountId)
                .map(startup -> startup.getStartupId());
    }

    @Override
    public Optional<Long> findInvestorIdByAccountId(Long accountId) {
        return investorRepository.findByAccountId(accountId)
                .map(investor -> investor.getInvestorId());
    }
}
