package com.ssafy.home.ai.service;

import com.ssafy.home.ai.agent.VerifierAgent;
import com.ssafy.home.ai.dto.request.AiChatRequest;
import com.ssafy.home.ai.dto.request.AiCompareRequest;
import com.ssafy.home.ai.dto.response.AiChatResponse;
import com.ssafy.home.ai.dto.response.AiCompareResponse;
import com.ssafy.home.ai.router.AiIntentRouter;
import com.ssafy.home.ai.type.AiIntent;
import static com.ssafy.home.ai.agent.VerifierAgent.stripMarkdown;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AiOrchestratorService {

    private final AiIntentRouter intentRouter;
    private final AiCompareService aiCompareService;
    private final RagKnowledgeService ragKnowledgeService;
    private final GeneralLlmService generalLlmService;
    private final VerifierAgent verifierAgent;

    public AiChatResponse chat(AiChatRequest request, Long userId) {
        AiIntent intent = intentRouter.route(request);

        if (intent == AiIntent.PROPERTY_COMPARE) {
            AiCompareResponse compare = aiCompareService.compare(
                    new AiCompareRequest(request.propertyIds(), request.message()),
                    userId
            );
            return new AiChatResponse(intent, stripMarkdown(compare.answer()), compare, compare.sources(), true, List.of());
        }

        if (intent == AiIntent.CONTRACT_KNOWLEDGE || intent == AiIntent.PROPERTY_RISK_EXPLAIN) {
            RagKnowledgeService.RagAnswer ragAnswer = ragKnowledgeService.answer(request.message());
            return verifierAgent.verify(intent, ragAnswer.answer(), ragAnswer.sources(), ragAnswer.warnings());
        }

        String answer = generalLlmService.answer(request.message());
        return verifierAgent.verify(intent, answer, List.of(), List.of());
    }
}
