# 🚗 ShareMile — Smart City-Based Ride Sharing & Carpool Management System

[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0%20(InnoDB)-blue.svg)](https://www.mysql.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/badge/Tests-10%20Passed%20(100%25)-success.svg)]()

**ShareMile** is a full-stack, smart city carpooling platform designed to connect urban commuters travelling along overlapping routes. Built with **Java 21, Spring Boot 3.3, MySQL 8.0, and Leaflet.js**, the system optimizes vehicle occupancy, reduces travel costs, and mitigates urban traffic congestion and carbon emissions.

---

## 🌟 Core Technical Highlights

- **📐 Spatial Route Discovery**: Computes great-circle distances using custom **Haversine trigonometric formulas** ($r = 6371.0 \text{ km}$) to locate carpools passing within configurable geographic search radii.
- **🎯 Dynamic 3-Factor Match Scoring**: Ranks suitable rides ($0 \text{--} 100\%$) based on:
  $$\text{Match Score} = (\text{Route Overlap \%} \times 0.5) + (\text{Time Proximity Score} \times 0.3) + (\text{Driver Rating Score} \times 0.2)$$
- **🔒 ACID Atomic Seat Allocation**: Employs Spring Boot `@Transactional` methods coupled with **MySQL InnoDB pessimistic row-locking (`SELECT ... FOR UPDATE`)** to guarantee zero overbooking during concurrent booking spikes.
- **🌿 Environmental Carbon Footprint Offset**: Calculates verified carbon emission offsets:
  $$\text{CO}_2 \text{ Saved (kg)} = \text{Distance (km)} \times 0.12 \text{ kg/km} \times (\text{Passengers} - 1)$$
- **⚡ Real-Time Push Notifications**: Delivers instantaneous booking alerts and ride status updates via **Spring WebSocket with STOMP messaging protocol**.
- **🗺️ Interactive Map Interface**: Seamless mapping with Leaflet.js and OpenStreetMap displaying custom origin/destination pins, route lines, and driver profiles.
- **📊 Administrative Analytics Dashboard**: Real-time SQL aggregations for hourly travel demand breakdown, route popularity, vehicle seat occupancy, and user moderation.

---

## 📁 Project Structure

```text
ShareMile/
├── sharemilenotes/                                # 📚 Comprehensive Recruiter & Interview Notes
│   ├── 01_PROJECT_OVERVIEW_AND_ELEVATOR_PITCH.md  # 30-sec pitch, 2-min pitch, business value
│   ├── 02_SYSTEM_ARCHITECTURE_AND_TECH_STACK.md   # Layered architecture, Mermaid diagrams, trade-offs
│   ├── 03_DATABASE_DESIGN_SCHEMA_AND_ACID.md      # ER diagram, schema, indexes, ACID breakdown
│   ├── 04_CORE_ALGORITHMS_HAVERSINE_MATCHING.md   # Haversine formula, match score, locking math
│   ├── 05_API_ENDPOINTS_AND_WEBSOCKET_WORKFLOW.md # REST catalog, sequence diagrams, STOMP flow
│   ├── 06_TESTING_STRATEGY_AND_EDGE_CASES.md      # Concurrency testing, CountDownLatch, edge cases
│   ├── 07_INTERVIEW_QUESTIONS_AND_TALKING_POINTS.md # 25+ First-person interviewer Q&A scripts
│   └── 08_FUTURE_ROADMAP_AND_SCALABILITY.md       # Redis geo-cache, Kafka, microservices roadmap
├── src/
│   ├── main/
│   │   ├── java/com/sharemile/
│   │   │   ├── config/DataInitializer.java        # Pre-populates test drivers, rides & routes
│   │   │   ├── controller/                        # Auth, Ride, Booking, Admin REST Controllers
│   │   │   ├── dto/                               # Clean request/response payload contracts
│   │   │   ├── model/                             # JPA Entities (User, Ride, Booking, Rules, Reviews)
│   │   │   ├── repository/                        # JPA Repositories (with Pessimistic Lock queries)
│   │   │   ├── security/                          # Spring Security 6, JWT Filter, Token Provider
│   │   │   ├── service/                           # Business logic (Spatial, Booking, Analytics)
│   │   │   ├── util/                              # SpatialUtils (Haversine), MatchScoreCalculator
│   │   │   ├── websocket/WebSocketConfig.java     # STOMP message broker configuration
│   │   │   └── ShareMileApplication.java          # Spring Boot main entrypoint
│   │   └── resources/
│   │       ├── application.properties             # MySQL 8.0 & Server configuration
│   │       └── static/                            # Full-featured Single-Page Web Application
│   │           ├── index.html                     # Responsive UI (Search, Leaflet Map, Portals)
│   │           ├── style.css                      # Modern CSS styling & glassmorphism components
│   │           └── app.js                         # Map logic, REST integration, STOMP client
│   └── test/
│       ├── java/com/sharemile/
│       │   ├── AuthServiceTest.java               # Registration, login, duplicate check
│       │   ├── BookingConcurrencyTest.java        # Multi-threaded race condition stress test
│       │   ├── MatchScoreCalculatorTest.java      # 3-factor weighting calculation tests
│       │   ├── RideServiceTest.java               # Ride publishing & Haversine search test
│       │   └── SpatialUtilsTest.java              # Haversine distance & carbon formula tests
│       └── resources/application.properties       # Fast in-memory H2 database for tests
├── pom.xml                                        # Maven dependencies & build configuration
└── README.md                                      # Documentation & getting started guide
```

---

## 🚀 Getting Started

### Prerequisites
- **JDK 21** (or Java 17+)
- **Maven 3.9+**
- **MySQL 8.0** (e.g., via XAMPP or standalone service)

---

### 1. Database Setup (MySQL)
1. Start your local MySQL service (e.g. via XAMPP Control Panel).
2. Create the database:
   ```sql
   CREATE DATABASE IF NOT EXISTS sharemile_db;
   ```
> The default configuration in `src/main/resources/application.properties` connects to `localhost:3306/sharemile_db` with user `root` and empty password.

---

### 2. Running the Application
Open a terminal in the project root directory and run:
```bash
mvn spring-boot:run
```

Once started, open your web browser and navigate to:
👉 **`http://localhost:8080`**

The full interactive dashboard with Leaflet map, search engine, passenger portal, driver portal, and admin analytics will load immediately!

---

### 3. Running Automated Tests & Concurrency Verification
To execute the automated test suite (including the multithreaded pessimistic locking race condition test):
```bash
mvn test
```
All **10 tests** will execute and pass against the isolated in-memory test database.

---

## 🔑 Pre-Configured Demo Credentials

The platform comes pre-seeded with realistic commuters and smart-city Pune corridor rides (Hinjawadi, Wakad, Shivajinagar, Nigdi, Magarpatta):

| Role | Username | Password | Full Name & Details |
|---|---|---|---|
| **Passenger** | `khushi_passenger` | `pass123` | Khushi Singh (⭐ 5.0 Rating) |
| **Driver** | `raj_driver` | `driver123` | Rajesh Sharma (Honda City, ⭐ 4.9 Rating) |
| **Driver** | `amit_driver` | `driver123` | Amit Deshmukh (Hyundai Verna, ⭐ 4.7 Rating) |
| **Admin** | `admin` | `admin123` | System Administrator |

---

## 📚 Recruiter & Interview Notes

For complete deep-dive documentation, architectural rationales, and first-person interviewer question scripts, refer to the **[`sharemilenotes/`](file:///sharemilenotes/)** directory:
- [01. Project Overview & Elevator Pitch](file:///sharemilenotes/01_PROJECT_OVERVIEW_AND_ELEVATOR_PITCH.md)
- [02. System Architecture & Tech Stack Rationale](file:///sharemilenotes/02_SYSTEM_ARCHITECTURE_AND_TECH_STACK.md)
- [03. Database Design, Schema & ACID Implementation](file:///sharemilenotes/03_DATABASE_DESIGN_SCHEMA_AND_ACID.md)
- [04. Core Algorithms: Haversine, Match Scoring & Concurrency](file:///sharemilenotes/04_CORE_ALGORITHMS_HAVERSINE_MATCHING_CONCURRENCY.md)
- [05. REST API Specifications & WebSocket Event Flow](file:///sharemilenotes/05_API_ENDPOINTS_AND_WEBSOCKET_WORKFLOW.md)
- [06. Testing Strategy, Concurrency Simulation & Edge Cases](file:///sharemilenotes/06_TESTING_STRATEGY_AND_EDGE_CASES.md)
- [07. 25+ Technical Interview Questions & Talking Points](file:///sharemilenotes/07_INTERVIEW_QUESTIONS_AND_TALKING_POINTS.md)
- [08. Future Roadmap & Scalability Strategy](file:///sharemilenotes/08_FUTURE_ROADMAP_AND_SCALABILITY.md)

---

## 👤 Author

- **Student Name**: Khushi Singh
- **Institution**: ATSS's Institute of Industrial & Computer Management & Research (IICMR), Nigdi
- **Academic Year**: 2026–2027 | MCA Semester – II Mini Project
- **GitHub**: [@khushi191103](https://github.com/khushi191103)
