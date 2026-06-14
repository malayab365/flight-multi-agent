package com.flightagent.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SupervisorNode {

    static final String SYSTEM_PROMPT = """
            You are the supervisor of a flight-assistant team. Given the conversation \
            so far, decide which team member should act next, or FINISH when the \
            user's request is fully handled.

            Members:
            - search_agent: find flights, resolve airport/city codes.
            - booking_agent: create, retrieve, cancel, or modify bookings.
            - ancillary_agent: seat selection and baggage add-ons.
            - price_agent: price tracking, fare alerts, price history.
            - info_agent: destination weather and travel info.

            Route to exactly one member at a time. When the last specialist has fully \
            answered the user, respond with FINISH.
            """;

    private static final List<String> MEMBERS = List.of(
            "search_agent", "booking_agent", "ancillary_agent", "price_agent", "info_agent");

    private final ChatClient chatClient;
    private final BeanOutputConverter<RouteDecision> routeConverter =
            new BeanOutputConverter<>(RouteDecision.class);

    public SupervisorNode(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public RouteDecision route(AgentState state) {
        List<Message> messages = toSpringMessages(state);
        RouteDecision decision = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .messages(messages)
                .call()
                .entity(routeConverter);
        if (decision == null || decision.next() == null || !isValidRoute(decision.next())) {
            return new RouteDecision(RouteDecision.FINISH);
        }
        return decision;
    }

    private static boolean isValidRoute(String next) {
        return RouteDecision.FINISH.equalsIgnoreCase(next.trim()) || MEMBERS.contains(next);
    }

    private static List<Message> toSpringMessages(AgentState state) {
        List<Message> out = new ArrayList<>(state.size());
        for (AgentMessage m : state.messages()) {
            switch (m.role()) {
                case USER -> out.add(new UserMessage(m.content()));
                case SYSTEM -> out.add(new SystemMessage(m.content()));
                case ASSISTANT -> {
                    String prefix = m.name() == null ? "" : "[" + m.name() + "] ";
                    out.add(new AssistantMessage(prefix + m.content()));
                }
            }
        }
        return out;
    }

    public static List<String> members() {
        return MEMBERS;
    }
}
