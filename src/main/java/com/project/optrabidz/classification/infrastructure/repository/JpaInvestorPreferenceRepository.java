package com.project.optrabidz.classification.infrastructure.repository;

import com.project.optrabidz.classification.infrastructure.entity.InvestorPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaInvestorPreferenceRepository extends JpaRepository<InvestorPreferenceEntity, Long> {
    List<InvestorPreferenceEntity> findByInvestorIdOrderByPreferenceTypeAscPreferenceValueAsc(Long investorId);

    void deleteByInvestorId(Long investorId);
}
