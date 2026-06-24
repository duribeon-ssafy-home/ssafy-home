package com.ssafy.home.location.dto.response;

public record LocationSearchResponse(
        String code,
        String sido,
        String gugun,
        String dong,
        String fullName
) {
}
