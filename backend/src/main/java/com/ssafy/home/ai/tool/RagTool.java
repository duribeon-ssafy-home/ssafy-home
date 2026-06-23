package com.ssafy.home.ai.tool;

import com.ssafy.home.ai.client.RagServiceClient;
import org.springframework.ai.tool.annotation.Tool;

public class RagTool {

    private final RagServiceClient ragServiceClient;

    public RagTool(RagServiceClient ragServiceClient) {
        this.ragServiceClient = ragServiceClient;
    }

    @Tool(description = """
            전세사기 예방, 임대차보호법, 계약 체크리스트, 등기부등본 읽는 법,
            전세보증보험, 임차권등기명령 등 법률·계약 문서 기반 지식을 검색합니다.
            계약 관련 질문이나 법적 절차 질문에 사용하세요.
            """)
    public String searchLegalKnowledge(String question) {
        return ragServiceClient.chat(question);
    }
}
