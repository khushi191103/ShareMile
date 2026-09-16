let currentUser = null;
let jwtToken = localStorage.getItem('sharemile_token') || '';
let map = null;
let markersLayer = null;
let currentSearchResults = [];
let stompClient = null;
let hourlyChartInstance = null;
let routesChartInstance = null;

// Pune Metro Central Coordinates
const DEFAULT_CENTER = [18.5204, 73.8567];

document.addEventListener('DOMContentLoaded', () => {
    initMap();
    initDefaultDates();
    
    // Auto login as Khushi Singh (Passenger) if no session exists
    if (!jwtToken) {
        quickLogin('khushi_passenger', 'pass123');
    } else {
        fetchCurrentUser();
    }

    executeSearch();
});

function initMap() {
    map = L.map('leaflet-map').setView(DEFAULT_CENTER, 12);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors | ShareMile'
    }).addTo(map);

    markersLayer = L.layerGroup().addTo(map);
}

function initDefaultDates() {
    const now = new Date();
    now.setHours(now.getHours() + 2);
    now.setMinutes(0);
    const isoString = now.toISOString().slice(0, 16);
    
    const searchTime = document.getElementById('search-time');
    if (searchTime) searchTime.value = isoString;

    const pubTime = document.getElementById('pub-time');
    if (pubTime) pubTime.value = isoString;
}

// 🔐 Authentication & Session
async function quickLogin(username, password) {
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) throw new Error("Login failed");
        const data = await response.json();

        jwtToken = data.token;
        currentUser = data;
        localStorage.setItem('sharemile_token', jwtToken);

        updateUserUI();
        connectWebSocket();
        closeModal('modal-auth');

        // Refresh current active tab
        const activeTab = document.querySelector('.nav-btn.active')?.id;
        if (activeTab === 'tab-btn-bookings') showTab('bookings');
        else if (activeTab === 'tab-btn-driver-requests') showTab('driver-requests');
        else if (activeTab === 'tab-btn-analytics') showTab('analytics');
        else executeSearch();

    } catch (err) {
        alert("Authentication failed: " + err.message);
    }
}

async function fetchCurrentUser() {
    try {
        const res = await fetch('/api/auth/me', {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        if (res.ok) {
            currentUser = await res.json();
            updateUserUI();
            connectWebSocket();
        } else {
            quickLogin('khushi_passenger', 'pass123');
        }
    } catch {
        quickLogin('khushi_passenger', 'pass123');
    }
}

function updateUserUI() {
    if (!currentUser) return;
    document.getElementById('user-name-text').innerText = `${currentUser.fullName} (${currentUser.role.replace('ROLE_', '')})`;
    document.getElementById('user-avatar-circle').innerText = currentUser.fullName.charAt(0);
}

// 🔔 Real-Time WebSockets (STOMP)
function connectWebSocket() {
    if (!currentUser) return;
    if (stompClient && stompClient.connected) return;

    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Suppress verbose console logs

    stompClient.connect({}, () => {
        stompClient.subscribe(`/topic/notifications/${currentUser.id}`, (msg) => {
            const notif = JSON.parse(msg.body);
            showInAppNotification(notif);
        });
    });
}

function showInAppNotification(notif) {
    const badge = document.getElementById('notif-badge');
    badge.style.display = 'inline-block';
    badge.innerText = Number(badge.innerText || 0) + 1;

    const list = document.getElementById('notification-list');
    const item = document.createElement('div');
    item.className = 'notif-item';
    item.innerHTML = `
        <strong>${notif.title}</strong>
        <p style="margin: 3px 0;">${notif.message}</p>
        <span class="notif-time">Just now</span>
    `;
    list.prepend(item);
}

function toggleNotifications() {
    const dd = document.getElementById('notification-dropdown');
    dd.style.display = dd.style.display === 'block' ? 'none' : 'block';
}

function clearNotifications() {
    document.getElementById('notification-list').innerHTML = '<p style="font-size: 12px; color: var(--dark-muted); text-align: center; padding: 10px;">No new alerts</p>';
    document.getElementById('notif-badge').style.display = 'none';
    document.getElementById('notif-badge').innerText = '0';
}

// 🔍 Search Rides & Map Plotting
async function executeSearch() {
    markersLayer.clearLayers();

    const searchPayload = {
        pickupLat: 18.5913, // Hinjawadi
        pickupLng: 73.7389,
        dropLat: 18.5314,   // Shivajinagar
        dropLng: 73.8446,
        radiusKm: Number(document.getElementById('search-radius').value),
        seatsNeeded: 1,
        desiredTime: document.getElementById('search-time').value ? document.getElementById('search-time').value + ":00" : null
    };

    try {
        const res = await fetch('/api/rides/search', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify(searchPayload)
        });

        currentSearchResults = await res.json();
        renderSearchResults(currentSearchResults);
    } catch (err) {
        console.error("Search failed:", err);
    }
}

function renderSearchResults(results) {
    const list = document.getElementById('rides-list');
    document.getElementById('results-count').innerText = results.length;
    list.innerHTML = '';

    if (results.length === 0) {
        list.innerHTML = `
            <div style="background: white; padding: 30px; text-align: center; border-radius: 12px; border: 1px solid var(--border);">
                <p style="font-size: 16px; font-weight: 600; color: var(--dark-muted);">No matching carpools found within this radius.</p>
                <p style="font-size: 13px; color: var(--dark-muted); margin-top: 6px;">Try expanding the radius slider or changing your search criteria.</p>
            </div>
        `;
        return;
    }

    const bounds = [];

    results.forEach((res, index) => {
        const r = res.ride;
        const card = document.createElement('div');
        card.className = 'ride-card';
        card.innerHTML = `
            <div class="card-top">
                <div class="driver-profile">
                    <div class="driver-avatar">${r.driver.fullName.charAt(0)}</div>
                    <div class="driver-info">
                        <h4>${r.driver.fullName} <span class="verified-badge" title="Verified Driver">✓</span></h4>
                        <div class="driver-sub">${r.driver.vehicleModel || 'Verified Vehicle'} • ⭐ ${r.driver.averageRating} (${r.driver.totalRatings} trips)</div>
                    </div>
                </div>
                <div class="match-badge" title="Haversine Multi-Factor Match Score">
                    🎯 ${res.matchScore}% Match
                </div>
            </div>

            <div class="route-flow">
                <div class="route-point origin">
                    <span class="route-dot dot-origin"></span>
                    <span>${r.originTitle}</span>
                </div>
                <div class="route-point dest">
                    <span class="route-dot dot-dest"></span>
                    <span>${r.destTitle}</span>
                </div>
            </div>

            <div style="display: flex; gap: 8px; font-size: 12px; color: var(--dark-muted); margin-bottom: 10px;">
                <span>🕒 Departure: ${new Date(r.departureTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                <span>•</span>
                <span>📏 ${r.estimatedDistanceKm} km</span>
                <span>•</span>
                <span class="carbon-badge">🌱 ~${res.estimatedCarbonSavingsKg} kg CO₂ offset</span>
            </div>

            <div class="card-metrics">
                <div>
                    <span class="price-tag">₹${r.pricePerSeat}</span>
                    <span style="font-size: 12px; color: var(--dark-muted);"> / seat</span>
                    <span class="seats-pill" style="margin-left: 10px;">${r.availableSeats} seat(s) left</span>
                </div>
                <button class="btn-primary" onclick="openBookingModal(${r.id})">Book Seat</button>
            </div>
        `;
        list.appendChild(card);

        // Map Pins & Polyline
        const originMarker = L.marker([r.originLat, r.originLng]).addTo(markersLayer)
            .bindPopup(`<b>Origin</b>: ${r.originTitle}<br>Driver: ${r.driver.fullName}`);
        const destMarker = L.marker([r.destLat, r.destLng]).addTo(markersLayer)
            .bindPopup(`<b>Destination</b>: ${r.destTitle}<br>Fare: ₹${r.pricePerSeat}`);

        const polyline = L.polyline([[r.originLat, r.originLng], [r.destLat, r.destLng]], {
            color: index === 0 ? '#2563eb' : '#64748b',
            weight: index === 0 ? 4 : 2,
            opacity: 0.8
        }).addTo(markersLayer);

        bounds.push([r.originLat, r.originLng], [r.destLat, r.destLng]);
    });

    if (bounds.length > 0) {
        map.fitBounds(bounds, { padding: [40, 40] });
    }
}

// 🧳 Seat Booking Modal & Atomic Booking Execution
function openBookingModal(rideId) {
    const resultObj = currentSearchResults.find(r => r.ride.id === rideId);
    if (!resultObj) return;

    const r = resultObj.ride;
    const content = document.getElementById('booking-modal-content');
    content.innerHTML = `
        <div style="margin-bottom: 16px;">
            <h4 style="font-size: 16px;">${r.originTitle} → ${r.destTitle}</h4>
            <p style="font-size: 13px; color: var(--dark-muted);">Driver: <strong>${r.driver.fullName}</strong> (${r.driver.vehicleModel || 'Standard Vehicle'})</p>
        </div>

        <div style="background: #f8fafc; padding: 14px; border-radius: 8px; margin-bottom: 16px; font-size: 13px;">
            <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
                <span>Base Price Per Seat:</span>
                <strong>₹${r.pricePerSeat}</strong>
            </div>
            <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
                <span>Available Seats (Atomic Locked):</span>
                <strong>${r.availableSeats}</strong>
            </div>
            <div style="display: flex; justify-content: space-between; color: #059669;">
                <span>Estimated CO₂ Offset:</span>
                <strong>~${resultObj.estimatedCarbonSavingsKg} kg</strong>
            </div>
        </div>

        <div class="form-group" style="margin-bottom: 16px;">
            <label>Select Seats to Reserve</label>
            <input type="number" id="book-seats-count" min="1" max="${r.availableSeats}" value="1" oninput="updateBookingTotal(${r.pricePerSeat})">
        </div>

        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; font-size: 16px;">
            <span>Total Payable Amount:</span>
            <span id="booking-total-display" style="font-size: 22px; font-weight: 800; color: var(--primary);">₹${r.pricePerSeat}</span>
        </div>

        <button class="btn-primary" style="width: 100%; padding: 12px; font-size: 15px;" onclick="confirmBookingAtomic(${r.id})">
            Confirm Seat Reservation
        </button>
    `;

    document.getElementById('modal-booking').style.display = 'flex';
}

function updateBookingTotal(pricePerSeat) {
    const count = Number(document.getElementById('book-seats-count').value) || 1;
    document.getElementById('booking-total-display').innerText = `₹${count * pricePerSeat}`;
}

async function confirmBookingAtomic(rideId) {
    const seats = Number(document.getElementById('book-seats-count').value) || 1;

    try {
        const res = await fetch('/api/bookings', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify({
                rideId: rideId,
                seatsBooked: seats
            })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || "Booking failed");
        }

        const booking = await res.json();
        closeModal('modal-booking');
        alert(`🎉 Booking #${booking.id} reserved successfully! Driver notified via WebSocket.`);
        executeSearch();
    } catch (err) {
        alert("Booking Error: " + err.message);
    }
}

// ➕ Offer Ride
function openPublishModal() {
    document.getElementById('modal-publish').style.display = 'flex';
}

async function handlePublishRide(e) {
    e.preventDefault();

    const payload = {
        originTitle: document.getElementById('pub-origin-title').value,
        originLat: 18.6517, // Pune / Nigdi
        originLng: 73.7716,
        destTitle: document.getElementById('pub-dest-title').value,
        destLat: 18.5284,   // Pune Station
        destLng: 73.8739,
        departureTime: document.getElementById('pub-time').value + ":00",
        totalSeats: Number(document.getElementById('pub-seats').value),
        pricePerSeat: Number(document.getElementById('pub-price').value)
    };

    try {
        const res = await fetch('/api/rides', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify(payload)
        });

        if (!res.ok) throw new Error("Could not publish ride");

        closeModal('modal-publish');
        alert("Ride published successfully!");
        executeSearch();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// 📑 Tabs Switcher
function showTab(tabName) {
    ['rides', 'bookings', 'driver-requests', 'analytics'].forEach(t => {
        const el = document.getElementById(`tab-${t}`);
        const btn = document.getElementById(`tab-btn-${t}`);
        if (el) el.style.display = 'none';
        if (btn) btn.classList.remove('active');
    });

    const target = document.getElementById(`tab-${tabName}`);
    const targetBtn = document.getElementById(`tab-btn-${tabName}`);
    if (target) target.style.display = 'grid';
    if (targetBtn) targetBtn.classList.add('active');

    const searchSection = document.getElementById('search-bar-section');
    if (searchSection) {
        searchSection.style.display = (tabName === 'rides') ? 'block' : 'none';
    }

    if (tabName === 'bookings') loadMyBookings();
    else if (tabName === 'driver-requests') loadDriverRequests();
    else if (tabName === 'analytics') loadAnalytics();
    else if (tabName === 'rides' && map) setTimeout(() => map.invalidateSize(), 200);
}

// 📑 Load Passenger Bookings
async function loadMyBookings() {
    const list = document.getElementById('my-bookings-list');
    list.innerHTML = '<p>Loading reservations...</p>';

    try {
        const res = await fetch('/api/bookings/my-bookings', {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        const bookings = await res.json();

        if (bookings.length === 0) {
            list.innerHTML = '<p style="color: var(--dark-muted);">You have no active or previous bookings.</p>';
            return;
        }

        list.innerHTML = bookings.map(b => `
            <div style="background: #f8fafc; padding: 18px; border-radius: 12px; border: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <span style="font-size: 11px; font-weight: 800; padding: 3px 8px; border-radius: 4px; background: ${b.status === 'ACCEPTED' ? '#dcfce7' : b.status === 'PENDING' ? '#fef3c7' : '#fee2e2'}; color: ${b.status === 'ACCEPTED' ? '#166534' : b.status === 'PENDING' ? '#92400e' : '#991b1b'};">
                        ${b.status}
                    </span>
                    <h4 style="margin: 8px 0 4px;">${b.ride.originTitle} → ${b.ride.destTitle}</h4>
                    <p style="font-size: 13px; color: var(--dark-muted);">Driver: <strong>${b.ride.driver.fullName}</strong> • ${b.seatsBooked} seat(s) • Fare: <strong>₹${b.totalFare}</strong></p>
                    <span class="carbon-badge" style="margin-top: 6px;">🌱 ${b.carbonOffsetKg} kg CO₂ offset</span>
                </div>
                <div style="display: flex; gap: 8px;">
                    ${b.status === 'ACCEPTED' ? `<button class="btn-outline" onclick="openRatingModal(${b.id}, ${b.ride.driver.id})">⭐ Rate Driver</button>` : ''}
                    ${b.status !== 'CANCELLED' && b.status !== 'REJECTED' ? `<button class="btn-outline" style="color: var(--danger); border-color: var(--danger);" onclick="cancelBooking(${b.id})">Cancel</button>` : ''}
                </div>
            </div>
        `).join('');
    } catch (err) {
        list.innerHTML = '<p>Could not load bookings.</p>';
    }
}

// 🚖 Load Driver Requests
async function loadDriverRequests() {
    const list = document.getElementById('driver-requests-list');
    list.innerHTML = '<p>Loading incoming passenger requests...</p>';

    try {
        const res = await fetch('/api/bookings/driver-requests', {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        const requests = await res.json();

        if (requests.length === 0) {
            list.innerHTML = '<p style="color: var(--dark-muted);">No booking requests received yet.</p>';
            return;
        }

        list.innerHTML = requests.map(b => `
            <div style="background: #f8fafc; padding: 18px; border-radius: 12px; border: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <span style="font-size: 11px; font-weight: 800; padding: 3px 8px; border-radius: 4px; background: ${b.status === 'ACCEPTED' ? '#dcfce7' : b.status === 'PENDING' ? '#fef3c7' : '#fee2e2'}; color: ${b.status === 'ACCEPTED' ? '#166534' : b.status === 'PENDING' ? '#92400e' : '#991b1b'};">
                        ${b.status}
                    </span>
                    <h4 style="margin: 8px 0 4px;">${b.passenger.fullName} requested ${b.seatsBooked} seat(s)</h4>
                    <p style="font-size: 13px; color: var(--dark-muted);">${b.ride.originTitle} → ${b.ride.destTitle} • Total Fare: <strong>₹${b.totalFare}</strong></p>
                </div>
                <div style="display: flex; gap: 8px;">
                    ${b.status === 'PENDING' ? `
                        <button class="btn-primary" style="background: var(--success);" onclick="respondBooking(${b.id}, true)">Accept</button>
                        <button class="btn-outline" style="color: var(--danger); border-color: var(--danger);" onclick="respondBooking(${b.id}, false)">Decline</button>
                    ` : ''}
                </div>
            </div>
        `).join('');
    } catch (err) {
        list.innerHTML = '<p>Could not load requests.</p>';
    }
}

async function respondBooking(bookingId, accept) {
    try {
        const res = await fetch(`/api/bookings/${bookingId}/respond`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify({ accept: accept })
        });
        if (!res.ok) throw new Error("Could not update booking");
        loadDriverRequests();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function cancelBooking(bookingId) {
    if (!confirm("Are you sure you want to cancel this booking?")) return;
    try {
        const res = await fetch(`/api/bookings/${bookingId}/cancel`, {
            method: 'PUT',
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        if (!res.ok) throw new Error("Could not cancel booking");
        loadMyBookings();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// ⭐ Ratings
function openRatingModal(bookingId, driverId) {
    document.getElementById('rating-booking-id').value = bookingId;
    document.getElementById('rating-reviewee-id').value = driverId;
    document.getElementById('modal-rating').style.display = 'flex';
}

async function handleRatingSubmit(e) {
    e.preventDefault();
    const payload = {
        bookingId: Number(document.getElementById('rating-booking-id').value),
        revieweeId: Number(document.getElementById('rating-reviewee-id').value),
        score: Number(document.getElementById('rating-score').value),
        comment: document.getElementById('rating-comment').value
    };

    try {
        const res = await fetch('/api/ratings', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error("Rating submission failed");
        closeModal('modal-rating');
        alert("Thank you! Your rating and review have been recorded.");
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// 📊 Load Admin Analytics
async function loadAnalytics() {
    try {
        const res = await fetch('/api/admin/analytics', {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        const data = await res.json();

        document.getElementById('metric-rides').innerText = data.totalRidesPublished;
        document.getElementById('metric-bookings').innerText = data.acceptedBookings;
        document.getElementById('metric-occupancy').innerText = `${data.overallOccupancyRatePercent}% Seat Occupancy`;
        document.getElementById('metric-carbon').innerText = `${data.totalCarbonOffsetKg} kg`;
        document.getElementById('metric-fare').innerText = `₹${data.totalFareVolume.toLocaleString('en-IN')}`;

        renderHourlyChart(data.hourlyDemandBreakdown);
        renderPopularRoutesChart(data.popularRoutes);
    } catch (err) {
        console.error("Could not load analytics:", err);
    }
}

function renderHourlyChart(hourlyData) {
    const ctx = document.getElementById('hourlyDemandChart');
    if (!ctx) return;
    if (hourlyChartInstance) hourlyChartInstance.destroy();

    const labels = Object.keys(hourlyData || {});
    const values = Object.values(hourlyData || {});

    hourlyChartInstance = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels.length > 0 ? labels : ['08:00 - 10:00', '10:00 - 12:00', '16:00 - 18:00', '18:00 - 20:00'],
            datasets: [{
                label: 'Carpool Trips Scheduled',
                data: values.length > 0 ? values : [12, 8, 15, 20],
                backgroundColor: '#3b82f6',
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } }
        }
    });
}

function renderPopularRoutesChart(routesData) {
    const ctx = document.getElementById('popularRoutesChart');
    if (!ctx) return;
    if (routesChartInstance) routesChartInstance.destroy();

    const labels = Object.keys(routesData || {});
    const values = Object.values(routesData || {});

    routesChartInstance = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: labels.length > 0 ? labels : ['Hinjawadi → Shivajinagar', 'Wakad → Magarpatta', 'Nigdi → Station'],
            datasets: [{
                data: values.length > 0 ? values : [45, 30, 25],
                backgroundColor: ['#2563eb', '#10b981', '#f59e0b', '#8b5cf6']
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false
        }
    });
}

// 🪟 Modals Helper
function closeModal(id) {
    document.getElementById(id).style.display = 'none';
}
function openAuthModal() {
    document.getElementById('modal-auth').style.display = 'flex';
}
