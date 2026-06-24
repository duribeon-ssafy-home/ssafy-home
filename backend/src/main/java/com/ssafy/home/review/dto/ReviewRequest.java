package com.ssafy.home.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(max = 20, message = "닉네임은 20자 이내로 입력해주세요.")
        String nickname,

        @NotBlank(message = "후기 내용을 입력해주세요.")
        @Size(max = 500, message = "후기는 500자 이내로 입력해주세요.")
        String content
) {}
