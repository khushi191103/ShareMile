# 08. Future Roadmap & Scalability Strategy

---

## 🚀 High-Scale Target Architecture (1M+ Users)

```mermaid
flowchart TD
    Client["Clients (Web / iOS / Android)"]
    API_Gateway["Spring Cloud Gateway / NGINX (SSL, Rate Limiting, Auth)"]

    subgraph MicroservicesTier ["Microservices Cluster"]
        AuthSvc["Auth & Identity Service"]
        SearchSvc["Spatial Discovery Service (Redis Geo)"]
        BookingSvc["Atomic Booking Engine (MySQL Shards)"]
        NotifSvc["WebSocket Push Gateway"]
    end

    subgraph EventStream ["Asynchronous Event Backbone"]
        Kafka["Apache Kafka (Topics: ride-events, booking-events)"]
    end

    subgraph DataStorage ["Distributed Data Tier"]
        RedisCluster[("Redis Cluster (Geohash Spatial Cache)")]
        MySQLPrimary[("MySQL Primary (Writes)")]
        MySQLReplica[("MySQL Read Replicas (Reads)")]
    end

    Client --> API_Gateway
    API_Gateway --> AuthSvc
    API_Gateway --> SearchSvc
    API_Gateway --> BookingSvc

    SearchSvc <--> RedisCluster
    BookingSvc --> MySQLPrimary
    MySQLPrimary -.->|Replication| MySQLReplica
    SearchSvc --> MySQLReplica

    BookingSvc -->|Publish Event| Kafka
    Kafka -->|Consume Event| NotifSvc
    NotifSvc -->|STOMP Push| Client
```

---

## 📈 Key Scalability Pillars

### 1. Redis Geospatial Indexing (`GEOSEARCH` / `GEORADIUS`)
- Instead of computing the Haversine trigonometric formula in Java across in-memory lists of rides, active ride origins are stored in Redis using `GEOADD rides:active <lng> <lat> <ride_id>`.
- Spatial radius searches execute in **sub-millisecond time ($O(N + \log M)$)** directly in RAM.

### 2. OSRM (Open Source Routing Machine) Dynamic Detour Matching
- In Phase 1, ShareMile calculates great-circle distance between pickup and driver origin.
- In Phase 2, integrate **OSRM API** to project the driver's exact road polyline (waypoints). If a passenger's pickup is within 500 meters of any highway point along the driver's actual driving path, the ride is matched even if the origin is far away!

### 3. Asynchronous Decoupling via Apache Kafka
- When a passenger completes a booking, the HTTP thread does not wait for email, SMS, or push notifications.
- The booking engine publishes a `BookingCreatedEvent` to Kafka. Dedicated consumer workers handle driver SMS, push alerts, and audit analytics independently.

### 4. Real-World Payment Gateway & Escrow
- Integration with UPI (Unified Payments Interface) via Razorpay or Stripe.
- Fare is held in platform escrow when the booking is confirmed and released to the driver only after the ride state transitions to `COMPLETED`.
