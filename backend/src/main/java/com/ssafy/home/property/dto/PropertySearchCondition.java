package com.ssafy.home.property.dto;

import com.ssafy.home.property.entity.RentType;
import com.ssafy.home.property.entity.RoomType;
import java.math.BigDecimal;

public record PropertySearchCondition(
        String sido,
        String gugun,
        String dong,
        RentType rentType,
        RoomType roomType,
        Long minDeposit,
        Long maxDeposit,
        Integer minMonthlyRent,
        Integer maxMonthlyRent,
        BigDecimal minArea,
        BigDecimal maxArea,
        Integer facilityCountMin
) {}
