package com.ssafy.home.ai.router;

import com.ssafy.home.ai.dto.request.AiChatRequest;
import com.ssafy.home.ai.type.AiIntent;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AiIntentRouter {

    private static final List<String> COMPARE_KEYWORDS = List.of(
            "비교", "추천", "어디", "어느", "나아", "순서", "랭킹", "가장",
            "이 중", "둘 중", "매물", "낮은", "높은", "싼", "비싼"
    );
    private static final List<String> PROPERTY_COMPARE_CONTEXT_KEYWORDS = List.of(
            "추천", "어디", "어느", "나아", "순서", "랭킹", "가장",
            "이 중", "둘 중", "매물", "낮은", "높은", "싼", "비싼"
    );
    private static final List<String> CONTRACT_KEYWORDS = List.of(
            "전세사기", "계약", "등기부", "등기부등본", "특약", "보증보험",
            "확정일자", "전입신고", "깡통전세", "임대인", "권리관계",
            "전세", "월세", "반전세", "임대차", "보증금", "대항력", "우선변제권"
    );
    private static final List<String> CONCEPT_QUESTION_KEYWORDS = List.of(
            "뭐야", "뭔가", "무엇", "뜻", "의미", "개념", "정의", "설명해줘",
            "알려줘", "차이", "어떻게 달라"
    );
    private static final List<String> RISK_EXPLAIN_KEYWORDS = List.of(
            "위험", "위험도", "위험 점수", "주의", "왜", "사유", "이유"
    );

    public AiIntent route(AiChatRequest request) {
        String message = normalize(request.message());
        boolean hasCompareProperties = request.propertyIds() != null && request.propertyIds().size() >= 2;
        boolean hasAnyProperty = request.propertyIds() != null && !request.propertyIds().isEmpty();

        if (isContractKnowledgeQuestion(message)) {
            return AiIntent.CONTRACT_KNOWLEDGE;
        }
        if (hasCompareProperties && isCompareQuestion(message)) {
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

    private boolean isCompareQuestion(String message) {
        return containsAny(message, COMPARE_KEYWORDS);
    }

    private boolean isContractKnowledgeQuestion(String message) {
        return containsAny(message, CONTRACT_KEYWORDS)
                && containsAny(message, CONCEPT_QUESTION_KEYWORDS)
                && !containsAny(message, PROPERTY_COMPARE_CONTEXT_KEYWORDS);
    }
}
