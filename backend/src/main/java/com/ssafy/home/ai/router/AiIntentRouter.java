package com.ssafy.home.ai.router;

import com.ssafy.home.ai.dto.request.AiChatRequest;
import com.ssafy.home.ai.type.AiIntent;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AiIntentRouter {

    private static final List<String> COMPARE_KEYWORDS = List.of(
            "비교", "추천", "어디", "나아", "순서", "랭킹", "가장"
    );
    private static final List<String> CONTRACT_KEYWORDS = List.of(
            "전세사기", "계약", "등기부", "등기부등본", "특약", "보증보험",
            "확정일자", "전입신고", "깡통전세", "임대인", "권리관계"
    );
    private static final List<String> RISK_EXPLAIN_KEYWORDS = List.of(
            "위험", "위험도", "위험 점수", "주의", "왜", "사유", "이유"
    );

    public AiIntent route(AiChatRequest request) {
        String message = normalize(request.message());
        boolean hasCompareProperties = request.propertyIds() != null && request.propertyIds().size() >= 2;
        boolean hasAnyProperty = request.propertyIds() != null && !request.propertyIds().isEmpty();

        if (hasCompareProperties && containsAny(message, COMPARE_KEYWORDS)) {
            return AiIntent.PROPERTY_COMPARE;
        }
        if (hasAnyProperty && containsAny(message, RISK_EXPLAIN_KEYWORDS)) {
            return AiIntent.PROPERTY_RISK_EXPLAIN;
        }
        if (containsAny(message, CONTRACT_KEYWORDS)) {
            return AiIntent.CONTRACT_KNOWLEDGE;
        }
        return AiIntent.GENERAL_REAL_ESTATE;
    }

    private String normalize(String message) {
        return message.trim().toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String message, List<String> keywords) {
        return keywords.stream().anyMatch(message::contains);
    }
}
