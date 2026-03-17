<div align="center">

<!-- BANNER -->
<img src="https://capsule-render.vercel.app/api?type=waving&color=gradient&customColorList=6,11,20&height=200&section=header&text=🚗%20CarSpa&fontSize=80&fontColor=fff&animation=twinkling&fontAlignY=35&desc=Premium%20On-Demand%20Car%20Wash%20Booking%20Platform&descAlignY=55&descSize=18" width="100%"/>

<!-- BADGES -->
<p>
  <img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/>
  <img src="https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black"/>
  <img src="https://img.shields.io/badge/Microservices-8%20Services-FF6B6B?style=for-the-badge&logo=kubernetes&logoColor=white"/>
  <img src="https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"/>
  <img src="https://img.shields.io/badge/Status-Active-00C851?style=for-the-badge"/>
</p>

<p>
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square"/>
  <img src="https://img.shields.io/github/last-commit/vaishnavipatil23800/carSpa?style=flat-square&color=indigo"/>
  <img src="https://img.shields.io/badge/PRs-Welcome-brightgreen?style=flat-square"/>
</p>

### ✨ A full-stack enterprise-grade car wash booking platform built with 8 Spring Boot microservices and React 18 — featuring real-time AI chat, JWT authentication, Razorpay payments, and a stunning dark glassmorphism UI.
</div>

---

## 📋 Table of Contents

- [✨ Features](#-features)
- [🏗️ System Architecture](#️-system-architecture)
- [🛠️ Tech Stack](#️-tech-stack)
- [📸 Screenshots](#-screenshots)
- [🚀 Getting Started](#-getting-started)
- [⚙️ Microservices Overview](#️-microservices-overview)
- [🔐 Authentication Flow](#-authentication-flow)
- [💳 Payment Integration](#-payment-integration)
- [📡 API Documentation](#-api-documentation)
- [📁 Project Structure](#-project-structure)
- [🧪 Testing](#-testing)
- [🗺️ Roadmap](#️-roadmap)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)

---

## ✨ Features

### 👤 User Features
| Feature | Description |
|---|---|
| 🔐 **JWT Authentication** | Secure register/login with role-based access (USER, ADMIN) |
| 🚗 **Car Management** | Add, update, delete personal vehicles with full details |
| 📅 **Smart Booking** | Book on-demand or schedule car wash services |
| 💳 **Razorpay Payments** | Seamless checkout with payment verification & invoice generation |
| 🤖 **AI Chat Assistant** | Real-time AI-powered support via OpenAI + WebSocket |
| 📲 **Email Notifications** | Booking confirmations and status updates via SMTP |
| 👤 **Profile Management** | Update personal info, view booking history |

### 🛠️ Admin Features
| Feature | Description |
|---|---|
| 📊 **Analytics Dashboard** | Revenue charts, booking stats, user growth (Recharts) |
| 🏢 **Wash Centre Management** | Create and manage service locations and packages |
| 👥 **User Management** | View all users, roles, and account details |
| 📋 **Booking Overview** | Monitor all bookings, update statuses in real-time |

### ⚡ Technical Highlights
- **Microservices Architecture** — 8 independently deployable Spring Boot services
- **API Gateway** — Centralized routing with JWT filter on port 8080
- **Service Discovery** — Netflix Eureka for dynamic service registration
- **Feign Clients** — Declarative HTTP communication between services
- **Dark Glassmorphism UI** — Premium aesthetic with Framer Motion animations
- **Skeleton Loaders** — Polished loading states across all views
- **Zustand State Management** — Lightweight global state for React
- **React Query v5** — Server-state caching and synchronization

---

## 🏗️ System Architecture

```
                          ┌─────────────────────────────────────┐
                          │         React 18 Frontend            │
                          │   (Dark Glassmorphism + Framer Motion)│
                          └──────────────┬──────────────────────┘
                                         │ HTTP (port 8080)
                          ┌──────────────▼──────────────────────┐
                          │          API Gateway                  │
                          │   JWT Filter + Rate Limiting          │
                          │         (port: 8080)                  │
                          └───┬────┬────┬────┬────┬────┬────┬───┘
                              │    │    │    │    │    │    │
               ┌──────────────┘    │    │    │    │    │    └──────────────┐
               │            ┌──────┘    │    │    └──────┐                 │
               ▼            ▼           ▼    ▼           ▼                 ▼
        ┌─────────┐  ┌─────────┐  ┌──────┐ ┌───────┐ ┌──────────┐ ┌──────────┐
        │  User   │  │   Car   │  │Booking│ │Payment│ │Notif.    │ │  Chat    │
        │Service  │  │Service  │  │Service│ │Service│ │Service   │ │Service   │
        │:8081    │  │:8082    │  │:8083  │ │:8084  │ │:8085     │ │:8086     │
        └────┬────┘  └────┬────┘  └───┬──┘ └───┬───┘ └────┬─────┘ └────┬─────┘
             │            │           │         │          │            │
             └────────────┴───────────┴────┬────┴──────────┘            │
                                           │                            │
                               ┌───────────▼──────────┐    ┌───────────▼────────┐
                               │    MySQL / H2 DB      │    │  OpenAI API +      │
                               │  (per-service schema) │    │  WebSocket (STOMP) │
                               └───────────────────────┘    └────────────────────┘

                    ┌──────────────────────────────────────┐
                    │         Eureka Server                  │
                    │   Service Registry & Discovery         │
                    │         (port: 8761)                   │
                    └──────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

### Backend
| Technology | Purpose |
|---|---|
| **Java 17** | Core language |
| **Spring Boot 3.x** | Microservice framework |
| **Spring Security + JWT** | Authentication & Authorization |
| **Spring Cloud Netflix Eureka** | Service Discovery |
| **Spring Cloud Gateway** | API Gateway with JWT filter |
| **Spring Data JPA** | ORM & database abstraction |
| **OpenFeign** | Inter-service HTTP communication |
| **Razorpay SDK** | Payment gateway integration |
| **JavaMail (SMTP)** | Email notifications |
| **WebSocket (STOMP)** | Real-time AI chat |
| **MySQL** | Production database |
| **Lombok** | Boilerplate reduction |
| **Maven** | Build & dependency management |

### Frontend
| Technology | Purpose |
|---|---|
| **React 18** | UI framework |
| **React Query v5** | Server state management & caching |
| **Zustand** | Global client state management |
| **Framer Motion** | Animations & micro-interactions |
| **React Hook Form + Yup** | Form handling & validation |
| **Recharts** | Admin analytics charts |
| **Axios** | HTTP client |
| **Lucide React** | Icon library |
| **React Hot Toast** | Toast notifications |
| **Bootstrap 5** | Base styling system |
| **date-fns** | Date utilities |

---
## 🚀 Getting Started

### Prerequisites

```bash
# Required
Java 17+
Node.js 18+
Maven 3.8+
MySQL 8+
Eclipse IDE (or IntelliJ)

# Optional
Postman (for API testing)
```

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/vaishnavipatil23800/carSpa.git
cd carSpa
```

### 2️⃣ Configure Environment Variables

For each service, update `application.properties`:

```properties
# User Service (src/main/resources/application.properties)
spring.datasource.url=jdbc:mysql://localhost:3306/carspa_users
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
jwt.secret=YOUR_JWT_SECRET_KEY

# Payment Service
razorpay.key.id=YOUR_RAZORPAY_KEY_ID
razorpay.key.secret=YOUR_RAZORPAY_KEY_SECRET

# Notification Service
spring.mail.username=YOUR_EMAIL
spring.mail.password=YOUR_APP_PASSWORD

# Chat Service
openai.api.key=YOUR_OPENAI_API_KEY
```

### 3️⃣ Start Backend Services (in order ⚠️)

```bash
# Step 1 — Eureka Server (Service Discovery)
cd eurekaService
mvn spring-boot:run
# ✅ Visit: http://localhost:8761

# Step 2 — User Service
cd ../userService
mvn spring-boot:run

# Step 3 — API Gateway
cd ../apiGateway
mvn spring-boot:run
# ✅ All APIs available at: http://localhost:8080

# Step 4 — Remaining Services (order flexible)
cd ../carService     && mvn spring-boot:run &
cd ../bookingService && mvn spring-boot:run &
cd ../paymentService && mvn spring-boot:run &
cd ../notificationService && mvn spring-boot:run &
cd ../chatService    && mvn spring-boot:run &
```

> 💡 **Tip:** In Eclipse, right-click each project → `Run As` → `Spring Boot App`

### 4️⃣ Start Frontend

```bash
cd carspa-frontend
npm install
npm start
# ✅ App running at: http://localhost:3000
```

---

## ⚙️ Microservices Overview

| Service | Port | Responsibility |
|---|---|---|
| **Eureka Server** | `8761` | Service registry and discovery |
| **API Gateway** | `8080` | Routing, JWT validation, load balancing |
| **User Service** | `8081` | Registration, login, JWT, role management |
| **Car Service** | `8082` | Vehicle CRUD, wash centre management |
| **Booking Service** | `8083` | Booking lifecycle, scheduling |
| **Payment Service** | `8084` | Razorpay integration, invoice generation |
| **Notification Service** | `8085` | Email alerts via JavaMail SMTP |
| **Chat Service** | `8086` | AI chat via OpenAI API + WebSocket |

---

## 🔐 Authentication Flow

```
Client                API Gateway              User Service
  │                        │                        │
  │── POST /auth/login ───►│                        │
  │                        │── forward request ────►│
  │                        │                        │ Validate credentials
  │                        │                        │ Generate JWT
  │                        │◄── JWT token ──────────│
  │◄── JWT token ──────────│                        │
  │                        │                        │
  │── GET /api/bookings ──►│                        │
  │   (Authorization: Bearer <JWT>)                 │
  │                        │ Validate JWT            │
  │                        │ Extract roles           │
  │                        │── route to service ────►│
```

**Roles:**
- `ROLE_USER` — Booking, payments, car management, AI chat
- `ROLE_ADMIN` — All user permissions + analytics, user management, centre management

---

## 💳 Payment Integration

CarSpa uses **Razorpay** for secure payment processing:

```
1. User initiates payment → POST /api/payments/create-order
2. Backend creates Razorpay order → returns order_id
3. Frontend opens Razorpay checkout modal
4. User completes payment
5. Frontend sends payment_id + signature → POST /api/payments/verify
6. Backend verifies HMAC signature
7. On success → update booking status + send email invoice
```

---

## 📡 API Documentation

> Full Swagger UI available at: **`http://localhost:8080/swagger-ui.html`**

### Auth Endpoints
```http
POST   /api/auth/register       # Register new user
POST   /api/auth/login          # Login, returns JWT
```

### User Endpoints
```http
GET    /api/users/profile       # Get current user profile
PUT    /api/users/profile       # Update profile
GET    /api/admin/users         # [ADMIN] Get all users
```

### Car Endpoints
```http
GET    /api/cars                # Get user's cars
POST   /api/cars                # Add new car
PUT    /api/cars/{id}           # Update car
DELETE /api/cars/{id}           # Delete car
```

### Booking Endpoints
```http
POST   /api/bookings            # Create booking
GET    /api/bookings            # Get user bookings
GET    /api/bookings/{id}       # Get booking details
PUT    /api/bookings/{id}/cancel # Cancel booking
GET    /api/admin/bookings      # [ADMIN] All bookings
```

### Payment Endpoints
```http
POST   /api/payments/create-order  # Create Razorpay order
POST   /api/payments/verify        # Verify payment signature
GET    /api/payments/invoice/{id}  # Download invoice
```

### Chat Endpoints
```http
GET    /api/chat/history           # Get chat history
WebSocket: /ws/chat               # Real-time AI chat
```

---

## 📁 Project Structure

```
carSpa/
├── 📦 eurekaService/               # Service discovery server
├── 📦 apiGateway/                  # JWT filter + routing (port 8080)
│   └── filter/AuthenticationFilter.java
├── 📦 userService/                 # Auth, JWT, user management
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   └── PasswordEncoderConfig.java   ← prevents circular dep
│   ├── controller/AuthController.java
│   └── service/UserService.java
├── 📦 carService/                  # Vehicle + wash centre management
├── 📦 bookingService/              # Booking lifecycle
├── 📦 paymentService/              # Razorpay + invoicing
├── 📦 notificationService/         # Email via SMTP
├── 📦 chatService/                 # OpenAI + WebSocket
│
└── 🌐 carspa-frontend/             # React 18 SPA
    ├── src/
    │   ├── api/                    # Axios instances + interceptors
    │   ├── components/             # Reusable UI components
    │   │   ├── Navbar.jsx
    │   │   ├── LoadingSpinner.jsx
    │   │   └── SkeletonLoader.jsx
    │   ├── pages/
    │   │   ├── Landing.jsx
    │   │   ├── Login.jsx / Register.jsx
    │   │   ├── Dashboard.jsx
    │   │   ├── Bookings.jsx
    │   │   ├── MyCars.jsx
    │   │   ├── Payment.jsx
    │   │   ├── Chat.jsx
    │   │   ├── Profile.jsx
    │   │   └── admin/
    │   │       ├── AdminDashboard.jsx
    │   │       ├── ManageUsers.jsx
    │   │       └── ManageBookings.jsx
    │   ├── store/                  # Zustand global state
    │   └── App.jsx
    └── package.json
```

---

## 🧪 Testing

### Postman Collection
Import `CarSpa.postman_collection.json` from the repo root to test all endpoints.

```bash
# Quick health check — all services via Gateway
curl http://localhost:8080/actuator/health

# Test auth flow
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123"}'
```

### Swagger UI
```
http://localhost:8080/swagger-ui.html
```
All endpoints documented with request/response schemas.

### Eureka Dashboard
```
http://localhost:8761
```
Verify all 8 services are registered and `UP`.

---

## 🗺️ Roadmap

- [x] 🏗️ 8-service microservices architecture
- [x] 🔐 JWT authentication with role-based access
- [x] 🚗 Car & wash centre management
- [x] 📅 Booking system with scheduling
- [x] 💳 Razorpay payment integration
- [x] 🤖 AI chat with OpenAI + WebSocket
- [x] 📲 Email notifications
- [x] 🎨 Dark glassmorphism React frontend
- [x] 📊 Admin analytics dashboard
- [ ] 🐳 Docker Compose for one-command startup
- [ ] ☸️ Kubernetes deployment manifests
- [ ] 📱 React Native mobile app
- [ ] 🌍 Multi-language support (i18n)
- [ ] 📍 GPS-based nearest wash centre
- [ ] ⭐ Reviews & ratings system

---

## 🏆 Key Engineering Decisions

| Decision | Rationale |
|---|---|
| **Microservices over Monolith** | Independent deployability, fault isolation, team scalability |
| **API Gateway JWT Validation** | Centralized auth — services trust the gateway, reducing duplication |
| **Eureka Service Discovery** | Dynamic scaling without hardcoded service URLs |
| **Feign Clients** | Declarative, readable inter-service calls vs. RestTemplate boilerplate |
| **Zustand over Redux** | 80% less boilerplate for the same capability at this scale |
| **React Query v5** | Eliminates manual loading/error state — built-in caching & refetch |
| **PasswordEncoderConfig isolation** | Resolves Spring circular dependency between SecurityConfig and UserService |

---

## 👩‍💻 About the Developer

**Vaishnavi Patil** — Java Full Stack Developer

> Passionate about building scalable, production-grade systems. CarSpa demonstrates end-to-end ownership of a complex distributed system — from database design to pixel-perfect UI.

[![GitHub](https://img.shields.io/badge/GitHub-vaishnavipatil23800-181717?style=for-the-badge&logo=github)](https://github.com/vaishnavipatil23800)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0A66C2?style=for-the-badge&logo=linkedin)](https://linkedin.com/in/YOUR_LINKEDIN)

---

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---

