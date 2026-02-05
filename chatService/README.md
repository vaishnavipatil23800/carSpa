# 🤖 SERVICE 8 — Chat Service (FINAL BACKEND SERVICE)

**Role:** AI-powered chat assistant powered by OpenAI GPT.
Two modes: user support + admin business intelligence with live data.

> ⚠️ **Prerequisites:**
> - Service 1 (Eureka) running on 8761
> - Service 3 (API Gateway) running on 8080
> - Services 4 + 6 running for admin live data (optional but recommended)
> - Valid OpenAI API key

---

## 📐 How It Works

```
User → POST /api/chat/user  → ChatService (user mode)
                                   │
                                   ├── user system prompt (pricing, how-to-book, etc.)
                                   ├── conversation history (last 6 pairs from frontend)
                                   ├── new user message
                                   └── OpenAI GPT-3.5-turbo ──► reply

Admin → POST /api/chat/admin → ChatService (admin mode)
                                   │
                                   ├── admin system prompt
                                   ├── LIVE DATA from booking-service (stats)
                                   ├── LIVE DATA from payment-service (revenue)
                                   ├── conversation history
                                   ├── new admin question
                                   └── OpenAI GPT-3.5-turbo ──► data-driven answer
```

---

## ✅ STEP 1 — Get an OpenAI API Key

1. Go to **https://platform.openai.com/api-keys**
2. Sign in or create a free account
3. Click **+ Create new secret key**
4. Copy the key (starts with `sk-...`)

> 💡 **Free tier** gives you $5 of credits — more than enough for development and demo.
> GPT-3.5-turbo is cheap: ~$0.002 per 1K tokens (~750 words).

---

## ✅ STEP 2 — Add Your API Key

Open `src/main/resources/application.properties`:

```properties
openai.api-key=sk-REPLACE_WITH_YOUR_KEY
```

**Never commit this key to GitHub.** In a real project, use environment variables:
```properties
openai.api-key=${OPENAI_API_KEY}
```

---

## ✅ STEP 3 — Open in IntelliJ

**File → Open** → `08-chat-service` → wait for Maven.

---

## ✅ STEP 4 — Run the Service

```cmd
cd 08-chat-service
mvn spring-boot:run
```

**Wait for:** `Started ChatServiceApplication in X seconds`

**Verify:** `http://localhost:8761` → `CHAT-SERVICE` appears.

---

## ✅ STEP 5 — Run Tests

```cmd
mvn test
```

8 unit tests — no OpenAI calls, no Eureka needed.

---

## ✅ STEP 6 — Test With Postman

> 📌 All requests via **port 8080 (gateway)**.
> Get your token: `POST http://localhost:8080/api/users/auth/login`

---

### 🔬 TEST 1 — Simple User Chat (first message, no history)
```
Method:  POST
URL:     http://localhost:8080/api/chat/user
Headers: Authorization: Bearer <token>
         Content-Type: application/json
Body:
```
```json
{
    "message": "Hi! What car wash services do you offer?",
    "history": []
}
```

**Expected (200 OK):**
```json
{
    "reply": "Hi Vaishnavi! CarSpa offers three wash services: Basic (exterior wash, ~₹249–₹299), Premium (interior + exterior, ~₹449–₹549), and Full Detail (complete service with wax, ~₹799–₹1199). You can book any of these at your nearest CarSpa centre! 🚗",
    "role": "assistant",
    "tokensUsed": 120,
    "adminMode": false
}
```

---

### 🔬 TEST 2 — Multi-turn Conversation (with history)

After Test 1, continue the conversation by passing the previous exchange in history:

```json
{
    "message": "How do I cancel a booking?",
    "history": [
        {
            "role": "user",
            "content": "Hi! What car wash services do you offer?"
        },
        {
            "role": "assistant",
            "content": "Hi Vaishnavi! CarSpa offers three wash services..."
        }
    ]
}
```

**Expected:** CarBot answers about cancellation and remembers the context from the previous message.

---

### 🔬 TEST 3 — Off-topic Question (should be redirected)
```json
{
    "message": "What's the weather in Pune today?",
    "history": []
}
```

**Expected:** CarBot politely declines and redirects to CarSpa topics.

---

### 🔬 TEST 4 — Service Pricing Question
```json
{
    "message": "Is GST included in the prices shown?",
    "history": []
}
```

**Expected:** CarBot explains that 18% GST is added on top of the base service price, and the total is shown at checkout.

---

### 🔬 TEST 5 — Admin Chat: Business Intelligence
```
Method:  POST
URL:     http://localhost:8080/api/chat/admin
Headers: Authorization: Bearer <admin-token>
         Content-Type: application/json
Body:
```
```json
{
    "message": "What is our current booking performance?",
    "history": []
}
```

**Expected:** The AI responds with real numbers from your DB:
```json
{
    "reply": "Based on the current data, CarSpa has 5 total bookings: 2 confirmed, 1 in progress, 1 done, and 1 cancelled. The cancellation rate is 20%, which is acceptable for the early stage...",
    "role": "assistant",
    "tokensUsed": 180,
    "adminMode": true
}
```

---

### 🔬 TEST 6 — Admin Revenue Analysis
```json
{
    "message": "Which service type generates the most revenue?",
    "history": []
}
```

**Expected:** AI analyses `revenueByServiceType` from the live data and gives a real answer.

---

### 🔬 TEST 7 — Admin Actionable Insight
```json
{
    "message": "We have many cancelled bookings. What should we do?",
    "history": []
}
```

**Expected:** AI gives business recommendations based on the cancellation numbers in the live data.

---

### 🔬 TEST 8 — Non-Admin Calling /admin Endpoint (should fail)
```
Method:  POST
URL:     http://localhost:8080/api/chat/admin
Headers: Authorization: Bearer <regular-user-token>
```

**Expected (403 Forbidden)** — non-admin users blocked at the controller level.

---

### 🔬 TEST 9 — Empty Message Validation
```json
{
    "message": "",
    "history": []
}
```

**Expected (400 Bad Request):**
```json
{
    "title": "Validation Failed",
    "errors": {
        "message": "Message cannot be empty"
    }
}
```

---

### 🔬 TEST 10 — Very Long Message (over 1000 chars)

Send a message string longer than 1000 characters.

**Expected (400 Bad Request):**
```json
{
    "title": "Validation Failed",
    "errors": {
        "message": "Message too long (max 1000 chars)"
    }
}
```

---

## 📌 Service Summary

| Item                    | Value                                            |
|-------------------------|--------------------------------------------------|
| Port                    | `8086`                                           |
| Database                | **None** — stateless                             |
| User chat               | `POST /api/chat/user`                            |
| Admin chat              | `POST /api/chat/admin` (ROLE_ADMIN only)         |
| AI model                | GPT-3.5-turbo (configurable in properties)       |
| Conversation history    | Last 6 pairs (12 messages) — sent by frontend    |
| Admin live data         | booking-service + payment-service via Feign      |
| Fallback (service down) | Empty map — admin chat still works, just no data |
| Token limit             | 600 per response (configurable)                  |
| Swagger                 | `http://localhost:8086/swagger-ui.html`          |

---

## 💡 Conversation History — How Frontend Manages It

```javascript
// Frontend (React) manages history in state
const [history, setHistory] = useState([]);

async function sendMessage(userMessage) {
    const response = await api.post('/api/chat/user', {
        message: userMessage,
        history: history.slice(-12)  // last 6 pairs
    });

    // append both user message and assistant reply to history
    setHistory(prev => [
        ...prev,
        { role: 'user',      content: userMessage },
        { role: 'assistant', content: response.data.reply }
    ]);
}
```

---

## ❌ Common Errors & Fixes

**`401 Unauthorized` from OpenAI**
→ Wrong or expired API key. Re-generate at platform.openai.com/api-keys.

**`429 Too Many Requests` from OpenAI**
→ Free tier rate limit hit. Wait 60 seconds or upgrade your plan.
→ In application.properties, reduce `openai.max-tokens=300` to use fewer tokens.

**`503 Service Unavailable` on /api/chat/**`**
→ Chat service isn't registered in Eureka yet. Wait 15 seconds after startup.

**Admin chat shows "data unavailable"**
→ booking-service or payment-service isn't running.
→ The fallback kicks in — admin chat still works, just without live numbers.

**`ClassCastException` in WebClient**
→ Usually a Netty version conflict between Spring Boot and WebFlux.
→ Ensure you haven't added `spring-boot-starter-webflux` to the web application type.
→ The fix is already applied: `spring.main.web-application-type=servlet` in application.properties.

---

## 🎉 ALL 8 BACKEND SERVICES COMPLETE!

| # | Service             | Port  | Status |
|---|---------------------|-------|--------|
| 1 | Eureka              | 8761  | ✅     |
| 2 | User Service        | 8081  | ✅     |
| 3 | API Gateway         | 8080  | ✅     |
| 4 | Booking Service     | 8082  | ✅     |
| 5 | Car Service         | 8083  | ✅     |
| 6 | Payment Service     | 8084  | ✅     |
| 7 | Notification Service| 8085  | ✅     |
| 8 | Chat Service        | 8086  | ✅     |

Say **"next"** for the **React Frontend** — the final piece!
