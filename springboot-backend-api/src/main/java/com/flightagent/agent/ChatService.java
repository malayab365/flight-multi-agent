package com.flightagent.agent;

import com.flightagent.dto.request.ChatMessage;
import com.flightagent.dto.response.ChatResponse;

import java.util.List;

public interface ChatService {

    ChatResponse chat(String message, List<ChatMessage> history);
}
