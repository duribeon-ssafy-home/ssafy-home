package com.ssafy.home.ai.agent;

import com.ssafy.home.ai.dto.response.AiChatResponse;
import com.ssafy.home.ai.dto.response.AiCompareSourceResponse;
import com.ssafy.home.ai.type.AiIntent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class VerifierAgent {

    public AiChatResponse verify(
            AiIntent intent,
            String answer,
            List<AiCompareSourceResponse> sources,
            List<String> warnings
    ) {
        List<String> verifiedWarnings = new ArrayList<>();
        if (warnings != null) {
            verifiedWarnings.addAll(warnings);
        }

        if (requiresDocumentGrounding(intent) && (sources == null || sources.isEmpty())) {
            verifiedWarnings.add("문서 근거가 아직 연결되지 않아 계약/법률성 답변은 일반 안내 수준으로 제한했습니다.");
        }

        String verifiedAnswer = softenUnsafeClaims(answer);
        return new AiChatResponse(intent, verifiedAnswer, null, sources, true, verifiedWarnings);
    }

    private boolean requiresDocumentGrounding(AiIntent intent) {
        return intent == AiIntent.CONTRACT_KNOWLEDGE || intent == AiIntent.PROPERTY_RISK_EXPLAIN;
    }

    private String softenUnsafeClaims(String answer) {
        return answer
                .replace("100% 안전", "상대적으로 안전")
                .replace("무조건 안전", "현재 근거 기준으로 비교적 안전")
                .replace("무조건 위험", "주의가 필요한 상태");
    }
}
