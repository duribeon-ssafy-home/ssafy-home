package com.ssafy.home.location.dto.response;

import com.ssafy.home.location.entity.LegalDong;

public record LocationSearchResponse(
        String code,
        String sido,
        String gugun,
        String dong,
        String fullName
) {

    public static LocationSearchResponse from(LegalDong legalDong) {
        return new LocationSearchResponse(
                legalDong.getCode(),
                legalDong.getSido(),
                legalDong.getGugun(),
                legalDong.getDong(),
                legalDong.getFullName()
        );
    }
}
