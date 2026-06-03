# 🏥 AI Healthcare Assistant — Backend

A **production-ready** Spring Boot backend for an AI-powered healthcare assistant.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Build | Maven |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA + Hibernate |
| Security | Spring Security + JWT |
| Cache | Redis 7 |
| AI | Spring AI (OpenAI GPT-4o-mini) |
| Rate Limiting | Bucket4j |
| Docs | SpringDoc OpenAPI (Swagger UI) |
| Containers | Docker + Docker Compose |

---

## API Endpoints

### Authentication (`/api/auth`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/register` | ❌ | Register new user |
| POST | `/login` | ❌ | Login, get tokens |
| POST | `/logout` | ✅ | Logout, revoke token |
| POST | `/refresh` | ❌ | Refresh access token |
| GET | `/me` | ✅ | Get current user profile |
| PUT | `/me` | ✅ | Update name / language |
| PUT | `/me/password` | ✅ | Change password |
| DELETE | `/me` | ✅ | Deactivate own account |

### Chat (`/api/chat`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/message` | ✅ | Send message to AI (with conversation history) |
| GET | `/history` | ✅ | Get paginated chat history |
| DELETE | `/{id}` | ✅ | Delete specific chat message |
| DELETE | `/history/all` | ✅ | Clear all chat history |

### AI Analysis (`/api/ai`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/analyze` | ✅ | Analyze symptoms (returns assessment, remedies, medicines, emergency flag, doctor suggestions) |
| GET | `/reports` | ✅ | Get paginated symptom report history |
| GET | `/reports/{id}` | ✅ | Get a specific symptom report |
| DELETE | `/reports/{id}` | ✅ | Delete a symptom report |

### Doctors (`/api`)

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/doctors` | ✅ | List/search doctors (filter by specialization, location, query) |
| GET | `/doctors/{id}` | ✅ | Get doctor by ID |
| POST | `/doctors` | 🔐 ADMIN | Create a new doctor |
| PUT | `/doctors/{id}` | 🔐 ADMIN | Update doctor details |
| DELETE | `/doctors/{id}` | 🔐 ADMIN | Delete a doctor |
| GET | `/hospitals` | ✅ | List all hospitals |
| GET | `/specializations` | ✅ | List all specializations |

### Admin (`/api/admin`) — ADMIN only

| Method | Endpoint | Description |
|---|---|---|
| GET | `/users` | List all users (paginated) |
| GET | `/users/role/{role}` | Filter users by role |
| GET | `/users/{id}` | Get user by ID |
| PATCH | `/users/{id}/role` | Change user role |
| PATCH | `/users/{id}/status` | Activate / deactivate user |
| DELETE | `/users/{id}` | Permanently delete user |

### WebSocket (`/ws`)

Connect via SockJS + STOMP. Send to `/app/chat`, receive on `/user/queue/chat-response`.

---

## Rate Limiting

| Endpoint Group | Limit |
|---|---|
| Auth (login/register) | 10 req/min per IP |
| AI / Chat | 20 req/min per IP |
| General API | 100 req/min per IP |

---

## Multilingual Support

Pass a `language` field in request bodies or user profile. Supported codes:
`en` (default), `hi`, `es`, `fr`, `de`, `ar`, `zh`, `pt`

---

## Running with Docker

```bash
cp .env.example .env   # Fill in OPENAI_API_KEY, JWT_SECRET, passwords
docker-compose up --build
```

---

## Running Tests

```bash
mvn test
```

Tests use H2 in-memory DB (no PostgreSQL or Redis needed).

---

## Swagger UI

[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Authenticate: copy the `accessToken` from login → click **Authorize** → paste `Bearer <token>`.
