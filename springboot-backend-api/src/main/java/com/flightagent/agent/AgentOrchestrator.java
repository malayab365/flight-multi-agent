package com.flightagent.agent;

import com.flightagent.agent.specialist.SpecialistAgent;
import com.flightagent.config.AppConfig;
import com.flightagent.dto.request.ChatMessage;
import com.flightagent.dto.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AgentOrchestrator implements ChatService {

    private final SupervisorNode supervisor;
    private final Map<String, SpecialistAgent> specialists;
    private final int recursionLimit;

    public AgentOrchestrator(SupervisorNode supervisor,
                             List<SpecialistAgent> specialists,
                             AppConfig appConfig) {
        this.supervisor = supervisor;
        this.specialists = specialists.stream()
                .collect(Collectors.toUnmodifiableMap(SpecialistAgent::name, Function.identity()));
        this.recursionLimit = appConfig.agent() == null ? 25 : appConfig.agent().recursionLimit();
    }

    @Override
    public ChatResponse chat(String message, List<ChatMessage> history) {
        AgentState state = new AgentState();
        if (history != null) {
            for (ChatMessage h : history) {
                state.append(toAgentMessage(h));
            }
        }
        state.append(AgentMessage.user(message));

        String lastSpecialist = null;
        int steps = 0;
        while (steps < recursionLimit) {
            RouteDecision decision = supervisor.route(state);
            if (decision.isFinish()) {
                break;
            }
            SpecialistAgent agent = specialists.get(decision.next());
            if (agent == null) {
                log.warn("Supervisor routed to unknown member '{}'; finishing.", decision.next());
                break;
            }
            String reply = agent.act(state);
            state.append(AgentMessage.assistant(reply, agent.name()));
            lastSpecialist = agent.name();
            steps++;
        }

        AgentMessage last = state.lastAssistant();
        if (last == null) {
            return new ChatResponse(
                    "I couldn't produce a response for that request.",
                    "supervisor");
        }
        return new ChatResponse(last.content(),
                lastSpecialist == null ? "supervisor" : lastSpecialist);
    }

    private static AgentMessage toAgentMessage(ChatMessage h) {
        String role = h.role() == null ? "user" : h.role().toLowerCase();
        return switch (role) {
            case "assistant", "ai" -> AgentMessage.assistant(h.content(), null);
            case "system" -> AgentMessage.system(h.content());
            default -> AgentMessage.user(h.content());
        };
    }
}
