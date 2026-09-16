# 01. Project Overview & Elevator Pitch

---

## 🎙️ 30-Second Elevator Pitch (Memorize This!)

> *"Hi! I built **ShareMile**, a smart city carpooling and ride-sharing platform engineered with **Java 21, Spring Boot 3, and MySQL 8**. It connects daily city commuters travelling on overlapping routes to share empty vehicle seats, drastically cutting travel expenses and traffic emissions.*
> 
> *What makes ShareMile technically compelling is that I tackled two major distributed systems challenges: **spatial route discovery using custom Haversine algorithms with a multi-factor compatibility scoring engine**, and **guaranteeing strict ACID compliance using MySQL InnoDB pessimistic row-locking (`SELECT ... FOR UPDATE`)** to mathematically eliminate double-booking race conditions when multiple users contest the final seat.*
> 
> *It also features real-time WebSocket push notifications via STOMP, interactive Leaflet.js mapping, and automated administrative mobility analytics measuring urban CO₂ offsets."*

---

## ⏱️ 2-Minute In-Depth Architectural Pitch

If the interviewer asks: *"Take 2 minutes to walk me through the architecture and design decisions:"*

> *"When designing ShareMile, I structured it as a **modular enterprise application** prioritizing data integrity, low-latency spatial discovery, and responsive user feedback.*
> 
> *1. **The Core Engine**: Daily commuters specify pickup and destination coordinates. Rather than simple point-to-point equality, I implemented a **Haversine spherical distance algorithm** that queries driver routes passing within a configurable spatial radius. Rides are dynamically ranked using a 3-factor **Match Score** weighting spatial overlap (50%), temporal departure proximity (30%), and driver reputation (20%).*
> 
> *2. **Concurrency & Seat Consistency**: In ride-sharing systems, high contention on the last seat is a classic race condition. I designed the booking pipeline with **Spring Data JPA's `@Transactional` boundary set to `Isolation.READ_COMMITTED` combined with `LockModeType.PESSIMISTIC_WRITE`**. This acquires an immediate row-level exclusive lock on the `rides` table in MySQL InnoDB. If 5 passengers hit 'Book Seat' at the exact same millisecond, the database serializes access—exactly one transaction decrements the capacity, while the remaining 4 receive a clean business exception without dirty reads or overbooking.*
> 
> *3. **Real-Time Communication**: Once a booking request is persisted, a Spring WebSocket broker dispatches an asynchronous STOMP message to the driver's private subscription channel (`/topic/notifications/{driverId}`), providing instant in-app alerts without expensive HTTP polling.*
> 
> *4. **Environmental Intelligence**: I incorporated an environmental metric model: `Distance (km) × 0.12 kg/km × (Passengers - 1)`, calculating verifiable carbon offset numbers displayed across passenger receipts and the administrative dashboard.*
> 
> *5. **Verification & Quality**: I built a full JUnit 5 and Spring Boot Test suite, including a multithreaded concurrency test using `ExecutorService` and `CountDownLatch` that rigorously proves zero double-booking under concurrent load."*

---

## 🎯 Problem Statement & Motivation

### Why did I build this?
1. **Urban Traffic Congestion & Empty Vehicle Capacity**: In major tech hubs (e.g., Pune's Hinjawadi, Bengaluru's Whitefield), thousands of single-occupancy cars clog arteries daily while passengers struggle with surging cab fares.
2. **Failure of Informal Ride-Sharing**: WhatsApp groups or Telegram channels lack search indexing, have no automated route-overlap computation, lack seat verification, and offer zero transaction safety.
3. **Need for Transactional Integrity**: Booking platforms often suffer from overbooking bugs during peak rush hours when concurrency spikes. I wanted to design a production-grade system that treats seat allocation with banking-grade ACID transactions.

---

## 🌟 Key Highlights to Emphasize

| Highlight | Technical Implementation | Talking Point for Interview |
|---|---|---|
| **ACID Atomic Booking** | `@Lock(LockModeType.PESSIMISTIC_WRITE)` | "I ensured absolute consistency over eventual consistency because overbooking damages passenger trust immediately." |
| **Spatial Matching** | Haversine Formula (r = 6371 km) | "I computed great-circle distances directly without relying on expensive third-party paid APIs for the core filter." |
| **Match Scoring** | 3-factor composite formula (0-100%) | "I built an intelligent ranking mechanism so passengers see the most compatible rides first, not just raw chronological lists." |
| **Real-Time Push** | Spring WebSocket + STOMP | "I eliminated battery-draining client polling by pushing booking events asynchronously via WebSockets." |
| **Security** | Spring Security + Stateless JWT + BCrypt | "Full token-based authentication with role-based access control (`ROLE_PASSENGER`, `ROLE_DRIVER`, `ROLE_ADMIN`)." |
