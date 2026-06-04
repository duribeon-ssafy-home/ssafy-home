package com.ssafy.home.property.dto;

import com.ssafy.home.property.entity.RentType;
import com.ssafy.home.property.entity.RoomType;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PropertyCreateRequest (
    String title,

    @NotBlank
    String address,

    String roadAddress,
    @NotBlank
    String sido,
    @NotBlank
    String gugun,
    @NotBlank
    String dong,
    BigDecimal latitude,
    BigDecimal longitude,
    RentType rentType,
    RoomType roomType,
    Long deposit,
    Integer monthlyRent,
    BigDecimal area,
    Integer floor,
    Integer buildYear,
    LocalDate dealDate
){}
