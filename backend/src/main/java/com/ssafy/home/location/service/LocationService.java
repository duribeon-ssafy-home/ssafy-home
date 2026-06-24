package com.ssafy.home.location.service;

import com.ssafy.home.location.dto.response.LocationSearchResponse;
import com.ssafy.home.location.repository.LegalDongRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private static final int MIN_KEYWORD_LENGTH = 2;
    private static final int MAX_RESULTS = 10;

    private final LegalDongRepository legalDongRepository;

    public List<LocationSearchResponse> search(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword.length() < MIN_KEYWORD_LENGTH) {
            return List.of();
        }

        return legalDongRepository
                .searchActiveByKeyword(normalizedKeyword, PageRequest.of(0, MAX_RESULTS))
                .stream()
                .map(LocationSearchResponse::from)
                .toList();
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }
}
