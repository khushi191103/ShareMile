# 07. Interview Questions & Talking Points (First-Person Scripts)

> [!TIP]
> Read these in the first person (**"I designed..."**, **"I chose..."**, **"When I ran concurrency tests..."**). These answers demonstrate architectural maturity, deep understanding of distributed systems, and real hands-on ownership.

---

### Q1: "Tell me about your project ShareMile."
**Your Model Answer**:
> *"ShareMile is a smart city carpooling and ride-sharing platform that I built using Java 21, Spring Boot 3, and MySQL 8. The core goal was to address the unorganized nature of daily office commutes where thousands of cars travel with empty seats along similar corridors.*
> 
> *I designed the system around two key engineering requirements: First, an intelligent **Haversine spatial discovery algorithm** with a 3-factor dynamic compatibility score that matches drivers and passengers within customizable geographic radius thresholds. Second, **strict ACID seat allocation using MySQL InnoDB pessimistic row-locking** to guarantee zero overbooking during concurrent booking spikes.*
> 
> *The platform also includes real-time WebSocket notifications via STOMP, recurring commute schedule generation, and an administrative dashboard visualizing mobility patterns and carbon emission offsets."*

---

### Q2: "What was the most challenging technical problem you solved?"
**Your Model Answer**:
> *"The hardest challenge was **concurrency control during seat reservation**. In carpooling, popular rush-hour routes often have 5 to 10 passengers trying to claim the last available seat on a ride at the same second.*
> 
> *Initially, in a standard read-then-write approach, threads suffered from a race condition where multiple transactions read `available_seats = 1` before either could decrement it, resulting in overbooking. I solved this by implementing **pessimistic row-level locking (`PESSIMISTIC_WRITE`)** on the `rides` table inside a `@Transactional(isolation = Isolation.READ_COMMITTED)` boundary.*
> 
> *This forces MySQL InnoDB to execute `SELECT ... FOR UPDATE`, obtaining an exclusive lock on that specific ride row. I then validated this by writing a multithreaded test using `ExecutorService` and `CountDownLatch` where 5 concurrent threads contested 1 seat, proving that exactly 1 succeeded and 4 received clean capacity exceptions, keeping the database in a consistent state."*

---

### Q3: "Why did you choose Pessimistic Locking over Optimistic Locking (`@Version`)?"
**Your Model Answer**:
> *"That was an intentional trade-off I evaluated carefully:*
> 
> *With **Optimistic Locking**, no database locks are held; Hibernate checks an entity version number on commit. If another transaction updated the row in the meantime, an `OptimisticLockException` is thrown. However, in a ride-sharing booking scenario where seats are scarce and contention is high, optimistic locking would cause 4 out of 5 users to fail with a cryptic retry prompt. The user experience during a morning commute would be terrible.*
> 
> *With **Pessimistic Locking**, the row is locked exclusively for only a few milliseconds while the seat count is checked and decremented. It guarantees deterministic serialization: the first request succeeds immediately, and the subsequent queued request immediately sees `availableSeats = 0` and receives a clear 'Sold Out' response without unnecessary retry loops."*

---

### Q4: "Can your pessimistic locking cause deadlocks? How did you prevent it?"
**Your Model Answer**:
> *"Deadlocks typically occur when two transactions try to acquire locks on multiple resources in opposite orders (e.g., Transaction 1 locks Ride A then Ride B, while Transaction 2 locks Ride B then Ride A).*
> 
> *In ShareMile, each seat booking transaction **only locks a single ride row at a time**, eliminating circular lock-wait graphs. Furthermore, the transaction scope is kept extremely lean: we only hold the lock while reading the integer count, decrementing it, and saving the booking record. No slow external API calls or WebSocket dispatches occur inside the locked block; notifications are sent asynchronously after the database commit."*

---

### Q5: "How does your route matching algorithm work?"
**Your Model Answer**:
> *"I implemented the **Haversine formula** to compute great-circle distance between coordinate pairs (origin-to-origin and destination-to-destination) on the spherical Earth surface ($r = 6371.0 \text{ km}$).*
> 
> *Once candidate rides within the passenger's search radius are filtered, I compute a composite **Match Score (0 to 100%)**:*
> - *50% weight on **Route Overlap**: Penalizes detour distance relative to the search radius.*
> - *30% weight on **Time Proximity**: Scores departure time variance (15 min difference yields 100%, tapering down as difference expands).*
> - *20% weight on **Driver Reputation**: Scaled from the driver's verified average star rating.*
> 
> *The search results are then sorted descending by Match Score so commuters always see the most practical and trusted options at the top."*

---

### Q6: "Why did you use Leaflet.js instead of the Google Maps API?"
**Your Model Answer**:
> *"Two reasons: **zero vendor lock-in** and **production cost control**. Google Maps charges per map load and per geocoding query, which can become prohibitively expensive for a student or startup project. Leaflet.js is lightweight (~38 KB), open-source, and integrates seamlessly with OpenStreetMap tiles. It allowed me to plot driver-passenger routes, custom markers, and polyline route paths directly using native browser Canvas and SVG."*

---

### Q7: "How did you secure your REST endpoints?"
**Your Model Answer**:
> *"I used **Spring Security 6** with a stateless **JWT (JSON Web Token)** filter chain.*
> 
> *1. When users authenticate at `/api/auth/login`, credentials are validated against BCrypt-hashed passwords.*
> *2. Upon success, a signed JWT is issued with an HMAC-SHA256 secret containing user claims and roles (`ROLE_PASSENGER`, `ROLE_DRIVER`, `ROLE_ADMIN`).*
> *3. On subsequent requests, a custom `JwtAuthenticationFilter` (extending `OncePerRequestFilter`) intercepts the request, validates the token signature and expiration, extracts user details, and populates the `SecurityContextHolder`.*
> *4. Sensitive endpoints use method-level security (e.g., `@PreAuthorize("hasRole('ADMIN')")`)."*

---

### Q8: "What was the hardest bug you encountered and how did you resolve it?"
**Your Model Answer**:
> *"When implementing the driver review and rating system, I noticed that if multiple passengers submitted ratings around the same time, the driver's `averageRating` would occasionally record inaccurate decimal numbers due to concurrent floating point recalculations.*
> 
> *I resolved this by structuring the review submission inside an isolated transaction and storing both `totalRatings` (int) and `averageRating` (double). Instead of calculating the average from scratch via a heavy aggregate SQL query across all historical rows on every read, I updated the rolling average using:*
> 
> $$\text{New Avg} = \frac{(\text{Current Avg} \times \text{Total Count}) + \text{New Score}}{\text{Total Count} + 1}$$
> 
> *I rounded the result to 1 decimal place using mathematical precision rounding, eliminating floating-point drift while maintaining high performance."*

---

### Q9: "If ShareMile scales to 1,000,000 daily active rides, what bottlenecks would you expect and how would you re-architect it?"
**Your Model Answer**:
> *"At 1 million daily rides, the primary bottlenecks would be: (1) high database write contention on popular corridor rows, (2) spatial calculation CPU overhead on every search query, and (3) persistent WebSocket connections on a single application server.*
> 
> *Here is how I would scale the architecture:*
> 1. * **Redis Geospatial Indexing (`GEOADD` / `GEORADIUS`)**: Instead of computing Haversine in Java across all scheduled rows, I would cache active ride coordinates in Redis. Redis uses Geohash-based sorted sets to query coordinates within a radius in $O(N + \log M)$ time in memory.*
> 2. * **Read/Write DB Splitting**: Direct writes (bookings, ride creation) to a MySQL Primary instance, with multiple read replicas for search queries.*
> 3. * **Message Broker (RabbitMQ / Apache Kafka)**: Decouple notifications and booking events from HTTP request threads into asynchronous Kafka topics.*
> 4. * **Microservices Decomposition**: Separate into Auth Service, Spatial Ride Discovery Service, Booking Engine, and Real-Time Gateway."*
