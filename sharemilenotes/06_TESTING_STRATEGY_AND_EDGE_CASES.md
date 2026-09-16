# 06. Testing Strategy, Concurrency Simulation & Edge Cases

---

## 🧪 Testing Philosophy

In ShareMile, I followed the standard **Testing Pyramid**:
1. **Unit Tests (Fast & Isolated)**: Testing mathematical logic like Haversine distance, multi-factor match score calculation, and carbon offset equations without starting Spring contexts.
2. **Integration Tests (Spring Boot Test)**: Verifying complete service and repository flows against an in-memory database (`H2` with MySQL compatibility mode) to validate JPA entity persistence, transactions, and security.
3. **Concurrency Stress Tests (Multithreaded)**: Directly testing race conditions and lock behavior under simultaneous thread execution.

---

## ⚡ Concurrency Test Walkthrough: `BookingConcurrencyTest`

When an interviewer asks: *"How did you test for race conditions?"*, walk them through this exact test:

### The Test Setup
Located in [`BookingConcurrencyTest.java`](file:///C:/Users/LENOVO/IdeaProjects/ShareMile/src/test/java/com/sharemile/BookingConcurrencyTest.java):
1. Create a ride in the database with `available_seats = 1`.
2. Create 5 separate passenger accounts.
3. Use `ExecutorService` with 5 threads.
4. Use two `CountDownLatch` barriers:
   - `startSignal = new CountDownLatch(1)`: Holds all 5 worker threads at the starting line until all are spawned and ready.
   - `doneSignal = new CountDownLatch(5)`: Waits for all 5 threads to complete execution before asserting results.

```java
// Hold all threads until ready
CountDownLatch startSignal = new CountDownLatch(1);
CountDownLatch doneSignal = new CountDownLatch(numberOfThreads);

for (int i = 0; i < numberOfThreads; i++) {
    final String passengerUsername = passengerUsernames.get(i);
    executor.submit(() -> {
        try {
            startSignal.await(); // Align all threads at the exact same instant
            bookingService.bookSeatsAtomic(request, passengerUsername);
            successCount.incrementAndGet();
        } catch (Exception e) {
            failureCount.incrementAndGet();
        } finally {
            doneSignal.countDown();
        }
    });
}

// Release the starting gate simultaneously
startSignal.countDown();
doneSignal.await();
```

### The Invariant Assertions
```java
// 1. Exactly ONE reservation succeeded
assertEquals(1, successCount.get());

// 2. Exactly FOUR reservations failed cleanly with capacity exceptions
assertEquals(4, failureCount.get());

// 3. Database state is strictly 0, NEVER negative
Ride finalRide = rideRepository.findById(testRideId).orElseThrow();
assertEquals(0, finalRide.getAvailableSeats());

// 4. Exactly one booking row created
assertEquals(1, bookingRepository.findByRideId(testRideId).size());
```

---

## 🛡️ Edge Cases Handled in Code

| Edge Case | Problem if Ignored | How I Solved It |
|---|---|---|
| **Driver books own ride** | Driver generates fake bookings to exploit promotion points or inflate stats | Checked in `BookingService`: `if (ride.getDriver().getId().equals(passenger.getId())) throw IllegalArgumentException` |
| **Booking on Cancelled Ride** | Passenger reserves seats on a ride the driver already cancelled | Checked in `BookingService`: `if (!"SCHEDULED".equals(ride.getStatus())) throw IllegalStateException` |
| **Driver rejects booking** | Seats were deducted during request; rejection would permanently lose driver capacity | In `respondToBooking(accept = false)`: seats are automatically replenished: `ride.setAvailableSeats(seats + booked)` |
| **Passenger cancels trip** | Driver vehicle travels empty despite passengers needing rides | `cancelBooking()` restores seat capacity and dispatches a WebSocket notification to the driver |
| **Identical Coordinates** | Division by zero or NaN in trigonometric Haversine | Handled in `SpatialUtils`: zero delta latitude/longitude returns `0.0 km` gracefully |
| **Single occupant carbon offset** | Formula would calculate savings for driving alone | Formula incorporates `(Passengers - 1)`: if total passengers is 1, savings is explicitly `0.0 kg` |
| **Duplicate User Registration** | DB throws raw SQL duplicate key constraint error | Checked upfront in `AuthService` using `userRepository.existsByUsername()` and `existsByEmail()` with friendly HTTP 400 responses |
