package com.flightagent.agent.specialist;

import com.flightagent.agent.AgentMessage;
import com.flightagent.agent.AgentState;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSpecialistAgent implements SpecialistAgent {

    protected final ChatClient chatClient;

    protected AbstractSpecialistAgent(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    protected abstract String systemPrompt();

    protected abstract Object[] tools();

    @Override
    public String act(AgentState state) {
        List<Message> messages = toSpringMessages(state);
        String reply = chatClient.prompt()
                .system(systemPrompt())
                .messages(messages)
                .tools(tools())
                .call()
                .content();
        return reply == null ? "" : reply.trim();
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
}
