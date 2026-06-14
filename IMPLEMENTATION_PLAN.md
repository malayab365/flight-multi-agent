# Implementation Plan: Spring Boot Backend Migration

**Stack**: Java 21 · Spring Boot 3.5.0 · Spring AI 1.0.0 · H2 (dev) / PostgreSQL (prod)  
**Folder**: `springboot-backend-api/` (Python backend untouched)  
**Model**: `anthropic/claude-opus-4-7` via OpenRouter  
**Pattern**: Implement → verify (`mvn test` / smoke test) → seek approval → next phase

---

## Phase 1 — Foundation ✅
**Goal**: Runnable Spring Boot skeleton with config and CORS.

Files to create:
- `pom.xml` — all dependencies declared up front
- `src/main/java/com/flightagent/FlightAgentApplication.java`
- `src/main/java/com/flightagent/config/AppConfig.java` — `@ConfigurationProperties` for `app.*`
- `src/main/java/com/flightagent/config/CorsConfig.java` — allow `localhost:3000`
- `src/main/resources/application.yml` — OpenRouter, H2, CORS, agent settings
- `src/main/resources/application-dev.yml` — H2 in-memory
- `src/main/resources/application-prod.yml` — PostgreSQL placeholders

**Done when**: `mvn clean compile` passes with zero errors.

---

## Phase 2 — Persistence Layer ✅
**Goal**: JPA entities + repositories for bookings and price watches.

Files to create:
- `entity/BookingEntity.java` + embeddables: `PassengerInfo`, `ItineraryInfo`, `PriceInfo`
- `entity/BookingStatus.java` (enum)
- `entity/SeatAssignmentEntity.java`
- `entity/BaggageItemEntity.java`
- `entity/PriceWatchEntity.java` + `WatchStatus.java` (enum)
- `repository/BookingRepository.java`
- `repository/PriceWatchRepository.java`

**Done when**: `@DataJpaTest` smoke test saves and finds a `BookingEntity`.

---

## Phase 3 — Tool Services ✅
**Goal**: All 14 Python `@tool` functions ported to Spring `@Service` methods.

Files to create:
- `tools/SearchToolService.java` — `searchFlights`, `getAirportCode` (Amadeus + sample fallback)
- `tools/BookingToolService.java` — `bookFlight`, `getBooking`, `cancelBooking`, `modifyBooking`
- `tools/AncillaryToolService.java` — `getSeatMap`, `selectSeat`, `addBaggage`
- `tools/PriceToolService.java` — `trackPrice`, `checkPriceWatch`, `getPriceHistory`
- `tools/WeatherToolService.java` — `getDestinationWeather`, `getDestinationInfo`
- `external/AmadeusClient.java` — WebClient wrapper with OAuth2 + sample fallback
- `external/OpenWeatherClient.java` — WebClient wrapper + sample fallback

**Done when**: All 4 JUnit tests pass (mirrors Python pytest scenarios):
- `SearchToolServiceTest` — search returns offers
- `BookingToolServiceTest` — book → cancel with 80% refund
- `AncillaryToolServiceTest` — seat + baggage fees
- `PriceToolServiceTest` — track + check price watch

---

## Phase 4 — REST Controllers ✅
**Goal**: All 15 endpoints exposed, matching Python FastAPI contracts exactly.

Files to create:
- `controller/ChatController.java` — POST `/api/chat`
- `controller/SearchController.java` — POST `/api/search`, GET `/api/airport-code`
- `controller/BookingController.java` — 4 booking endpoints + 2 ancillary endpoints
- `controller/PriceController.java` — 3 price endpoints
- `controller/InfoController.java` — GET `/api/weather/{city}`, GET `/api/destination/{city}`
- All `dto/request/` and `dto/response/` Java records
- `exception/GlobalExceptionHandler.java` — `@ControllerAdvice` for 404/500

**Done when**: `@WebMvcTest` + `MockMvc` tests verify all 15 endpoints return correct HTTP status and JSON shape.

---

## Phase 5 — Agent Orchestration ⬜
**Goal**: Supervisor multi-agent loop + 5 ReAct specialist agents.

Files to create:
- `agent/AgentMessage.java` (record)
- `agent/AgentState.java` — append-only message accumulator
- `agent/RouteDecision.java` (record) — structured LLM output
- `agent/SupervisorNode.java` — `BeanOutputConverter<RouteDecision>` routing
- `agent/AgentOrchestrator.java` — supervisor → specialist loop (recursion limit 25)
- `agent/specialist/SpecialistAgent.java` (interface)
- `agent/specialist/AbstractSpecialistAgent.java` — ReAct loop (think → tool → observe)
- `agent/specialist/SearchSpecialist.java`
- `agent/specialist/BookingSpecialist.java`
- `agent/specialist/AncillarySpecialist.java`
- `agent/specialist/PriceSpecialist.java`
- `agent/specialist/InfoSpecialist.java`

**Done when**: Integration test — POST `/api/chat` with `{"message":"Find flights JFK to LHR"}` returns a coherent response mentioning flight data.

---

## Phase 6 — Hardening ⬜
**Goal**: Production-readiness, error handling, Docker Compose.

Tasks:
- `application-prod.yml` — PostgreSQL config
- `docker-compose.yml` — PostgreSQL service for local prod testing
- `Dockerfile` — multi-stage Maven build
- Jackson serialization tuning (snake_case, null omission)
- Full end-to-end test: Next.js frontend hits Java backend

**Done when**: `docker compose up` runs the full stack; frontend search + booking + chat flows all work.

---

## Approval Gates

After each phase: implementation is verified, then explicit user approval is requested before the next phase starts.
