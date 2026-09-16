# 04. Core Algorithms: Haversine, Match Scoring & Concurrency

---

## 📐 1. Haversine Distance Formula

### Mathematical Definition
The Haversine formula determines the great-circle distance between two points on a sphere given their longitudes and latitudes:

$$\text{Distance} = 2r \cdot \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta\varphi}{2}\right) + \cos(\varphi_1)\cos(\varphi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)}\right)$$

Where:
- $r = 6371.0 \text{ km}$ (Mean radius of the Earth)
- $\varphi_1, \varphi_2$ = Latitudes of Point 1 and Point 2 in radians
- $\Delta\varphi = \varphi_2 - \varphi_1$ (Difference in latitude)
- $\Delta\lambda = \lambda_2 - \lambda_1$ (Difference in longitude)

### Java Implementation in ShareMile
Located in [`SpatialUtils.java`](file:///C:/Users/LENOVO/IdeaProjects/ShareMile/src/main/java/com/sharemile/util/SpatialUtils.java):
```java
public static double haversineDistanceKm(double lat1, double lng1, double lat2, double lng2) {
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);

    double phi1 = Math.toRadians(lat1);
    double phi2 = Math.toRadians(lat2);

    double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
            + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLng / 2.0) * Math.sin(dLng / 2.0);

    double c = 2.0 * Math.asin(Math.min(1.0, Math.sqrt(a)));
    return Math.round((EARTH_RADIUS_KM * c) * 100.0) / 100.0;
}
```

### Why not simple Euclidean distance?
- **Interviewer Question**: *"Why can't you just use $\sqrt{(x_2-x_1)^2 + (y_2-y_1)^2}$?"*
- **Your Answer**:
  > *"Because the Earth is an oblate spheroid, not a flat 2D Cartesian plane. Degrees of longitude shrink dramatically as latitude approaches the poles: at the equator, $1^\circ$ longitude is $\approx 111.32 \text{ km}$, but at $60^\circ$ latitude, $1^\circ$ is only $\approx 55.8 \text{ km}$. Euclidean calculations produce catastrophic distortion. Haversine projects coordinates onto a spherical surface, ensuring sub-kilometer accuracy."*

---

## 🎯 2. Multi-Factor Match Score Algorithm

In the ShareMile synopsis, ride recommendations are dynamically scored ($0 \text{ to } 100\%$):

$$\text{Match Score} = (\text{Route Overlap \%} \times 0.5) + (\text{Time Proximity Score} \times 0.3) + (\text{Driver Rating Score} \times 0.2)$$

### Component Breakdown
1. **Route Overlap Component ($50\%$ Weight)**:
   - Measures how close the passenger's pickup and drop-off are to the driver's planned route.
   - Formula:
     $$\text{Overlap Score} = \max\left(0.0, 100 \times \left(1.0 - \frac{\text{PickupDist} + \text{DropDist}}{2 \times \text{SearchRadius}}\right)\right)$$
   - *Example*: If search radius is $10\text{ km}$, and passenger pickup is $1\text{ km}$ from driver origin, and dropoff is $1\text{ km}$ from destination, total detour is $2\text{ km}$. Overlap $= 100 \times (1 - 2/20) = 90\%$.

2. **Time Proximity Component ($30\%$ Weight)**:
   - Evaluates schedule compatibility:
     - $|\Delta t| \le 15 \text{ min} \implies 100\%$
     - $|\Delta t| \le 45 \text{ min} \implies 85\%$
     - $|\Delta t| \le 90 \text{ min} \implies 70\%$
     - $|\Delta t| \le 180 \text{ min} \implies 40\%$
     - Beyond 3 hours, score decays linearly.

3. **Driver Rating Component ($20\%$ Weight)**:
   - Normalized from star rating (1.0 to 5.0):
     $$\text{Rating Score} = \left(\frac{\text{averageRating}}{5.0}\right) \times 100$$
   - A 4.9-star driver gets $98\%$.

---

## ⚔️ 3. Concurrency Control: Preventing Double-Booking

### The Classic Race Condition
```text
Time    Thread A (Passenger 1)                Thread B (Passenger 2)
 |
 T1     SELECT available_seats (returns 1)    
 T2                                           SELECT available_seats (returns 1)
 T3     Validate: 1 >= 1 (OK)                 
 T4                                           Validate: 1 >= 1 (OK)
 T5     UPDATE available_seats = 0            
 T6                                           UPDATE available_seats = 0 (OVERBOOKING!)
 V      Both receive "Booking Confirmed"!
```

### The Solution: Pessimistic Row-Level Locking
In [`RideRepository.java`](file:///C:/Users/LENOVO/IdeaProjects/ShareMile/src/main/java/com/sharemile/repository/RideRepository.java):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT r FROM Ride r WHERE r.id = :id")
Optional<Ride> findByIdWithLock(@Param("id") Long id);
```

### How MySQL InnoDB Executes This:
```sql
SELECT * FROM rides WHERE id = 123 FOR UPDATE;
```

```text
Time    Thread A (Passenger 1)                   Thread B (Passenger 2)
 |
 T1     BEGIN TRANSACTION                        BEGIN TRANSACTION
 T2     SELECT ... FOR UPDATE (Lock Acquired!)   SELECT ... FOR UPDATE (BLOCKED / WAITING)
 T3     Validate: 1 >= 1 (OK)                    ...
 T4     UPDATE available_seats = 0               ...
 T5     INSERT INTO bookings ...                 ...
 T6     COMMIT TRANSACTION (Lock Released!)      ...
 T7                                              Lock Acquired! Reads available_seats = 0
 T8                                              Validate: 0 < 1 -> Throws IllegalStateException!
 V                                               ROLLBACK TRANSACTION
```

### Interview Comparison: Optimistic vs. Pessimistic Locking

| Feature | Optimistic Locking (`@Version`) | Pessimistic Locking (`PESSIMISTIC_WRITE`) |
|---|---|---|
| **Mechanism** | Checks version integer at commit time | Locks the row in DB immediately (`FOR UPDATE`) |
| **When Contention Occurs** | Throws `OptimisticLockException` requiring user retry | Serializes requests; strictly enforces atomic decrement |
| **Best Used For** | Low contention, read-heavy workloads | High contention on scarce inventory (carpool seats, ticket booking) |
| **Why I Chose It** | In peak morning rush hour, users don't want "Your transaction failed, please try again". They need immediate deterministic booking confirmation or instant capacity rejection. |

---

## 🌿 4. Environmental Carbon Footprint Offset

$$\text{CO}_2 \text{ Saved (kg)} = \text{Distance (km)} \times 0.12 \text{ kg/km} \times (\text{Total Passengers} - 1)$$

### Derivation:
- Average standard petrol/diesel car produces $\approx 120 \text{ g} = 0.12 \text{ kg of CO}_2$ per kilometer.
- When $N$ passengers travel together in 1 vehicle instead of $N$ separate individual vehicles/cabs, $(N - 1)$ vehicles are eliminated from the road.
- *Example*: A $25\text{ km}$ commute with 4 carpooling colleagues:
  $$\text{Savings} = 25 \times 0.12 \times (4 - 1) = 3.0 \times 3 = 9.00 \text{ kg of CO}_2 \text{ saved!}$$
