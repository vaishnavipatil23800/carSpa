CarSpa is a full-stack car wash booking platform engineered with a microservices architecture — 8 independent Spring Boot services behind a single API Gateway, with a dark-themed React frontend featuring real-time AI chat, Razorpay payments, and role-based dashboards.
✦ Services
Eureka Server         8761  Service registry and discovery
API Gateway           8080  JWT validation, routing — the only public entry 
User Service          8081  Auth, JWT issuance, role management 
Car Service           8082  Vehicle and wash centre management 
Booking Service       8083  Booking lifecycle and scheduling 
Payment Service       8084  Razorpay integration and invoicing 
Notification Service  8085  Email alerts via SMTP 
Chat Service          8086  AI assistant via OpenAI + WebSocket


✦ Stack

Java 17 + Spring Boot 3 across all 8 services
Spring Security + JWT — stateless auth, role-based endpoint protection
Spring Cloud Gateway — centralized routing with a JWT filter
Netflix Eureka — service registration and discovery
OpenFeign — declarative HTTP between services
Spring Data JPA + MySQL — per-service isolated databases
Razorpay SDK — payment order creation and HMAC signature verification
JavaMail — transactional email via SMTP
WebSocket (STOMP) — real-time bidirectional chat channel


React 18 SPA with dark glassmorphism design system
React Query v5 — server-state caching, background refetch
Zustand — global client state without the Redux boilerplate
Framer Motion — page transitions, micro-animations, skeleton loaders
React Hook Form + Yup — schema-validated forms
Recharts — admin analytics (revenue, booking trends, user growth)
Axios with JWT interceptor for automatic token attachment



Start backend

1. Service discovery first
cd eurekaService && mvn spring-boot:run

2. Auth layer
cd userService && mvn spring-boot:run

3. Gateway (now Eureka + UserService are up)
cd apiGateway && mvn spring-boot:run

4. Remaining services (any order)
cd carService && mvn spring-boot:run
cd bookingService && mvn spring-boot:run
cd paymentService && mvn spring-boot:run
cd notificationService && mvn spring-boot:run
cd chatService && mvn spring-boot:run

Start frontend

cd carspa-frontend
npm install
npm start 


✦ Folder Structure

carSpa/
├── eurekaService/          # Service registry
├── apiGateway/             # JWT filter + routing
├── userService/            # Auth + user management
├── carService/             # Vehicles + wash centres
├── bookingService/         # Booking lifecycle
├── paymentService/         # Razorpay + invoices
├── notificationService/    # Email alerts
├── chatService/            # OpenAI + WebSocket
└── carspa-frontend/        # React 18 SPA
    └── src/
        ├── api/            # Axios + interceptors
        ├── components/     # Shared UI components
        ├── pages/          # Route-level pages
        └── store/          # Zustand state
