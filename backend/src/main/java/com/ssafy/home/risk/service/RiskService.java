package com.ssafy.home.risk.service;

import com.ssafy.home.property.repository.PropertyRepository;
import com.ssafy.home.report.repository.ReportRepository;
import com.ssafy.home.risk.dto.response.RiskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskService {

    private final PropertyRepository propertyRepository;
    private final ReportRepository reportRepository;

    public RiskResponse analyzeRisk(Long propertyId) {
        throw new UnsupportedOperationException("not implemented");
    }
}
