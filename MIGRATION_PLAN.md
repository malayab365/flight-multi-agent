# Migration Plan: Python Backend → Java Spring Boot

## Context
The backend is a FastAPI + LangGraph multi-agent flight booking system. The goal is to
replace it with a Java 21 / Spring Boot 4.1.0 application that exposes identical REST
endpoints (so the existing Next.js frontend needs zero changes) while swapping the
Python-specific orchestration framework for a hand-written state machine backed by
Spring AI.

---

## Framework Choice: Spring AI 1.0.x (over LangChain4j)
- OpenRouter exposes an OpenAI-compatible API → plugs directly into
  `spring.ai.openai.base-url=https://openrouter.ai/api/v1` with no adapter code.
- `BeanOutputConverter<RouteDecision>` replaces LangChain's `with_structured_output(Route)`.
- No stable Java equivalent of `langgraph.StateGraph` exists in either library;
  the supervisor loop must be hand-written regardless.
- Spring AI integrates natively with Spring Boot autoconfiguration, Spring Data JPA, and WebMvcTest.

---

## Project Structure

```
backend-java/
├── pom.xml
└── src/
    ├── main/java/com/flightagent/
    │   ├── FlightAgentApplication.java
    │   ├── config/          AppConfig, CorsConfig, SpringAiConfig
    │   ├── controller/      ChatController, SearchController, BookingController,
    │   │                    AncillaryController, PriceController, InfoController
    │   ├── dto/             request/ + response/ (Java records, one per Pydantic model)
    │   ├── agent/           AgentOrchestrator, SupervisorNode, AgentState,
    │   │                    AgentMessage, RouteDecision
    │   │   └── specialist/  SpecialistAgent (interface), AbstractSpecialistAgent,
    │   │                    SearchSpecialist, BookingSpecialist, AncillarySpecialist,
    │   │                    PriceSpecialist, InfoSpecialist
    │   ├── tools/           SearchToolService, BookingToolService, AncillaryToolService,
    │   │                    PriceToolService, WeatherToolService
    │   ├── entity/          BookingEntity (@Embeddable PassengerInfo, ItineraryInfo,
    │   │                    PriceInfo), SeatAssignmentEntity, BaggageItemEntity,
    │   │                    PriceWatchEntity
    │   ├── repository/      BookingRepository, PriceWatchRepository (JpaRepository)
    │   └── external/        AmadeusClient, OpenWeatherClient (WebClient)
    └── main/resources/
        ├── application.yml
        ├── application-dev.yml   (H2 in-memory)
        └── application-prod.yml  (PostgreSQL)
```

---

## Key Dependencies (pom.xml)

- `spring-boot-starter-web` — REST controllers + Tomcat
- `spring-boot-starter-data-jpa` — JPA + Hibernate
- `spring-boot-starter-validation` — `@Valid` on DTOs
- `spring-boot-starter-webflux` — `WebClient` for Amadeus + OpenWeatherMap
- `spring-ai-openai-spring-boot-starter` (version 1.0.x, via spring-ai-bom)
- `com.h2database:h2` (dev/test scope)
- `org.postgresql:postgresql` (prod scope)
- `org.projectlombok:lombok`
- `spring-boot-starter-test` (test scope)

---

## application.yml (key properties)

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENROUTER_API_KEY}
      base-url: https://openrouter.ai/api/v1
      chat.options:
        model: ${OPENROUTER_MODEL:openai/gpt-4o}
        temperature: 0.0
        max-tokens: 2048
  datasource:
    url: jdbc:h2:mem:flightdb;DB_CLOSE_DELAY=-1
  jpa.hibernate.ddl-auto: create-drop

app:
  amadeus: { client-id: ${AMADEUS_CLIENT_ID:}, client-secret: ${AMADEUS_CLIENT_SECRET:} }
  weather:  { api-key: ${OPENWEATHER_API_KEY:} }
  cors:     { allowed-origins: "http://localhost:3000" }
  agent:    { recursion-limit: 25 }
```

---

## Agent Orchestration (the hardest part)

### AgentState.java
Mutable container replicating `TypedDict + add_messages` reducer.
Only exposes `addMessage(AgentMessage)` — never a setter — enforcing append-only semantics.

```java
public class AgentState {
    private final List<AgentMessage> messages = new ArrayList<>();
    private String next;
    public void addMessage(AgentMessage msg) { messages.add(msg); }
    // getMessages(), getNext(), setNext()
}
```

### SupervisorNode.java
Replaces `_supervisor_node` in `graph.py`. Uses `BeanOutputConverter<RouteDecision>`
to enforce structured JSON output from the LLM:

```java
@Component
public class SupervisorNode {
    private final ChatClient chatClient;
    private final BeanOutputConverter<RouteDecision> converter = new BeanOutputConverter<>(RouteDecision.class);

    public String route(AgentState state) {
        // Build messages: supervisor system prompt + state.getMessages()
        // Append converter.getFormat() hint to system prompt
        // chatClient.prompt().messages(...).call().content()
        // converter.convert(raw) → RouteDecision.next()
    }
}
```

### AgentOrchestrator.java
Replaces the `StateGraph` compile + `app.invoke()` in `graph.py`.
Implements the supervisor → conditional_edge → specialist → supervisor loop:

```java
@Service
public class AgentOrchestrator {
    // for (int step = 0; step < recursionLimit; step++) {
    //   String next = supervisor.route(state);
    //   if ("FINISH".equals(next) || next == null) break;
    //   AgentMessage response = specialists.get(next).run(state.getMessages());
    //   state.addMessage(response);
    // }
}
```

### AbstractSpecialistAgent.java
Implements the ReAct loop (replaces `create_react_agent` from langgraph):

```java
// for (int step = 0; step < MAX_REACT_STEPS; step++) {
//   ChatResponse response = chatClient.prompt()
//       .messages(messages).functions(toolNames).call().chatResponse();
//   AssistantMessage msg = response.getResult().getOutput();
//   if (msg.getToolCalls().isEmpty()) return finalAnswer;
//   for (ToolCall call : msg.getToolCalls()) {
//       messages.add(new ToolResponseMessage(executeTool(call)));
//   }
// }
```

Each of the 5 specialist classes (e.g., `SearchSpecialist`) extends this and
injects their corresponding `*ToolService.getFunctionCallbacks()`.

---

## Python Tool → Java Service Mapping

| Python `@tool` | Java method | Service class |
|---|---|---|
| `search_flights` | `searchFlights(SearchFlightsInput)` | `SearchToolService` |
| `get_airport_code` | `getAirportCode(GetAirportCodeInput)` | `SearchToolService` |
| `book_flight` | `bookFlight(BookFlightInput)` | `BookingToolService` |
| `get_booking` | `getBooking(GetBookingInput)` | `BookingToolService` |
| `cancel_booking` | `cancelBooking(CancelBookingInput)` | `BookingToolService` |
| `modify_booking` | `modifyBooking(ModifyBookingInput)` | `BookingToolService` |
| `get_seat_map` | `getSeatMap(GetSeatMapInput)` | `AncillaryToolService` |
| `select_seat` | `selectSeat(SelectSeatInput)` | `AncillaryToolService` |
| `add_baggage` | `addBaggage(AddBaggageInput)` | `AncillaryToolService` |
| `track_price` | `trackPrice(TrackPriceInput)` | `PriceToolService` |
| `check_price_watch` | `checkPriceWatch(CheckPriceWatchInput)` | `PriceToolService` |
| `get_price_history` | `getPriceHistory(GetPriceHistoryInput)` | `PriceToolService` |
| `get_destination_weather` | `getDestinationWeather(GetWeatherInput)` | `WeatherToolService` |
| `get_destination_info` | `getDestinationInfo(GetDestinationInfoInput)` | `WeatherToolService` |

Tools return `String` (raw JSON) to the LLM; controllers deserialize to `Map<String, Object>`
and return it — Jackson re-serializes cleanly.

Each service method is registered via `FunctionCallback.builder()` so Spring AI passes
the correct JSON schema to the LLM and routes tool call results back automatically.

---

## JPA Entities

**BookingEntity** (`bookings` table)
- PK: `booking_id` (String, `BK-XXXXXXXX` format)
- `@Embeddable` value objects: `PassengerInfo(name, email)`, `ItineraryInfo(origin, destination, departureDate)`, `PriceInfo(total BigDecimal, currency)`
- `@Enumerated` `BookingStatus`: CONFIRMED / CANCELLED / MODIFIED
- `@OneToOne` `SeatAssignmentEntity` (nullable)
- `@OneToMany` `List<BaggageItemEntity>`
- `createdAt`, `cancelledAt`, `modifiedAt` as `Instant`
- `@Version` for optimistic locking (replaces Python `threading.Lock`)

**PriceWatchEntity** (`price_watches` table)
- PK: `watch_id` (String, `PW-XXXXXXXX`)
- `@Enumerated` `WatchStatus`: ACTIVE / TRIGGERED / EXPIRED

**ID generation**: `UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase()`
preserving the `BK-` / `PW-` prefix format.

---

## Endpoint Preservation (all 17 must stay identical)

| Method | Path | Controller | Tool service |
|---|---|---|---|
| POST | `/api/chat` | `ChatController` | `AgentOrchestrator` |
| POST | `/api/search` | `SearchController` | `SearchToolService` |
| GET | `/api/airport-code?city=` | `SearchController` | `SearchToolService` |
| POST | `/api/book` | `BookingController` | `BookingToolService` |
| GET | `/api/booking/{id}` | `BookingController` | `BookingToolService` |
| POST | `/api/booking/{id}/cancel` | `BookingController` | `BookingToolService` |
| POST | `/api/booking/{id}/modify` | `BookingController` | `BookingToolService` |
| GET | `/api/seat-map/{offerId}` | `AncillaryController` | `AncillaryToolService` |
| POST | `/api/booking/{id}/seat` | `AncillaryController` | `AncillaryToolService` |
| POST | `/api/booking/{id}/baggage` | `AncillaryController` | `AncillaryToolService` |
| POST | `/api/price/track` | `PriceController` | `PriceToolService` |
| GET | `/api/price/watch/{id}` | `PriceController` | `PriceToolService` |
| GET | `/api/price/history` | `PriceController` | `PriceToolService` |
| GET | `/api/weather/{city}` | `InfoController` | `WeatherToolService` |
| GET | `/api/destination/{city}` | `InfoController` | `WeatherToolService` |

CORS: `CorsConfig` allows `http://localhost:3000` on `/api/**` for all methods.

Missing-booking `404` handled by `@ControllerAdvice` or inline `ResponseStatusException`.

---

## Migration Sequencing (10 days)

| Phase | Days | Deliverable | Done when |
|---|---|---|---|
| 1 Foundation | 1–2 | `pom.xml`, `application.yml`, app class, `CorsConfig` | `mvn spring-boot:run` starts without errors |
| 2 Persistence | 2–3 | All `entity/` + `repository/` + H2 dev profile | JPA smoke test: save + findById passes |
| 3 Tool Services | 3–5 | All 5 `*ToolService` + `AmadeusClient` + `OpenWeatherClient` (sample fallbacks) | All 4 JUnit tests pass (mirrors pytest suite) |
| 4 Controllers | 5–6 | All 6 controllers | `MockMvc` tests verify all 17 endpoint shapes |
| 5 Agent Layer | 6–9 | `AgentState`, `SupervisorNode`, `AbstractSpecialistAgent`, 5 specialists, `AgentOrchestrator` | POST `/api/chat` returns coherent response |
| 6 Hardening | 9–10 | Prod profile (PostgreSQL), Docker Compose, error handling, Jackson tuning | Full end-to-end integration test passes |

---

## Critical Source Files to Reference

| Python file | Java counterpart(s) |
|---|---|
| `backend/src/graph.py` | `AgentOrchestrator.java`, `SupervisorNode.java` |
| `backend/src/agents/specialists.py` | `AbstractSpecialistAgent.java` + 5 specialist classes |
| `backend/src/tools/booking_tools.py` | `BookingToolService.java` (all fees + refund logic) |
| `backend/src/tools/ancillary_tools.py` | `AncillaryToolService.java` (fee maps) |
| `backend/src/tools/price_tools.py` | `PriceToolService.java` (hash-based quartiles, random check) |
| `backend/src/api.py` | All 6 controllers (endpoint shapes + error handling) |
| `backend/tests/test_tools.py` | 4 JUnit test classes in `src/test/` |

---

## Verification

1. **Unit**: `mvn test` — all 4 tool-service JUnit tests pass (no LLM, no network; H2 in-memory)
2. **Controller**: `MockMvc` tests for all 17 endpoints with mocked services
3. **Integration (agent layer)**: POST `/api/chat` with `{"message": "Find flights JFK to LHR on 2026-08-01"}` returns a response containing flight data from the search agent
4. **Frontend compat**: Start existing Next.js frontend (`npm run dev` in `frontend/`) against Java backend on `:8000`; verify search, booking, and chat flows work end-to-end
5. **Offline mode**: Run with no Amadeus/weather keys set — all tools must fall back to sample data (same as Python)
