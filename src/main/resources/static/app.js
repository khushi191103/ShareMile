let currentUser = null;
let jwtToken = localStorage.getItem('sharemile_token') || '';
let map = null;
let markersLayer = null;
let driverTrackingLayer = null;
let currentSearchResults = [];
let stompClient = null;
let hourlyChartInstance = null;
let routesChartInstance = null;
let loginMode = 'user'; // 'user' | 'admin'
let registerUserType = 'PASSENGER'; // 'PASSENGER' | 'DRIVER'

// Passenger live GPS coordinates (defaults to Pune Hinjawadi / Central)
let currentPassengerLat = 18.5913;
let currentPassengerLng = 73.7389;
let currentDropLat = 18.5314;
let currentDropLng = 73.8446;

// Pune Central Coordinates
const DEFAULT_CENTER = [18.5500, 73.8000];

// ═══════════════════════════════════════════════
// INIT
// ═══════════════════════════════════════════════
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    if (jwtToken) {
        restoreSession();
    }
});

// ═══════════════════════════════════════════════
// DARK / LIGHT THEME TOGGLE
// ═══════════════════════════════════════════════
function initTheme() {
    const savedTheme = localStorage.getItem('sharemile_theme') || 'dark';
    if (savedTheme === 'dark') {
        document.body.classList.add('dark-theme');
        updateThemeBtn(true);
    } else {
        document.body.classList.remove('dark-theme');
        updateThemeBtn(false);
    }
}

function toggleTheme() {
    const isDark = document.body.classList.toggle('dark-theme');
    localStorage.setItem('sharemile_theme', isDark ? 'dark' : 'light');
    updateThemeBtn(isDark);
}

function updateThemeBtn(isDark) {
    const btn = document.getElementById('theme-toggle-btn');
    if (btn) {
        btn.innerText = isDark ? '☀️' : '🌙';
        btn.title = isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode';
    }
}

// ═══════════════════════════════════════════════
// TOAST NOTIFICATIONS (SMS & EMAIL DISPATCH ALERTS)
// ═══════════════════════════════════════════════
function showDispatchToast(type, title, message) {
    const container = document.getElementById('dispatch-toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = 'dispatch-toast';
    const icon = (type === 'sms') ? '📱' : (type === 'email') ? '✉️' : '🔔';

    toast.innerHTML = `
        <div class="toast-icon">${icon}</div>
        <div class="toast-content">
            <h5 style="color: ${type === 'sms' ? '#38bdf8' : type === 'email' ? '#34d399' : 'var(--primary)'};">${title}</h5>
            <p>${message}</p>
        </div>
    `;

    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(15px)';
        setTimeout(() => toast.remove(), 400);
    }, 6500);
}

// ═══════════════════════════════════════════════
// AUTH & REGISTRATION LOGIC
// ═══════════════════════════════════════════════
function switchLoginMode(mode) {
    loginMode = mode;
    document.getElementById('toggle-user').classList.toggle('active', mode === 'user');
    document.getElementById('toggle-admin').classList.toggle('active', mode === 'admin');

    const adminInfo = document.getElementById('admin-info');
    const userRegSection = document.getElementById('user-register-section');
    const submitBtn = document.getElementById('login-submit-btn');

    if (mode === 'admin') {
        adminInfo.style.display = 'flex';
        userRegSection.style.display = 'none';
        submitBtn.classList.add('admin-mode');
    } else {
        adminInfo.style.display = 'none';
        userRegSection.style.display = 'block';
        submitBtn.classList.remove('admin-mode');
    }
    document.getElementById('login-error').textContent = '';
}

function showRegisterPanel() {
    document.getElementById('login-panel').style.display = 'none';
    document.getElementById('register-panel').style.display = 'block';
    document.getElementById('register-error').textContent = '';
    setRegisterType('PASSENGER');
}

function showLoginPanel() {
    document.getElementById('register-panel').style.display = 'none';
    document.getElementById('login-panel').style.display = 'block';
    document.getElementById('login-error').textContent = '';
}

function setRegisterType(type) {
    registerUserType = type;
    document.getElementById('reg-type-passenger').classList.toggle('active', type === 'PASSENGER');
    document.getElementById('reg-type-driver').classList.toggle('active', type === 'DRIVER');

    const driverFields = document.getElementById('driver-specific-fields');
    if (type === 'DRIVER') {
        driverFields.style.display = 'block';
    } else {
        driverFields.style.display = 'none';
    }
}

async function handleRegister() {
    const fullName = document.getElementById('reg-fullname').value.trim();
    const username = document.getElementById('reg-username').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const phone = document.getElementById('reg-phone').value.trim();
    const gender = document.getElementById('reg-gender').value;
    const password = document.getElementById('reg-password').value;
    const license = document.getElementById('reg-license').value.trim();
    const vehicleModel = document.getElementById('reg-vehicle-model').value.trim();
    const vehicleNumber = document.getElementById('reg-vehicle-number').value.trim();
    const vehicleColor = document.getElementById('reg-vehicle-color').value.trim();
    const errorEl = document.getElementById('register-error');
    const btn = document.getElementById('register-submit-btn');

    errorEl.textContent = '';

    if (!fullName || !username || !email || !password) {
        errorEl.textContent = 'Please fill in all mandatory fields.';
        return;
    }

    if (registerUserType === 'DRIVER') {
        if (!license) {
            errorEl.textContent = '⚠️ Driving License Number is required for Driver registration.';
            return;
        }
        if (!vehicleModel) {
            errorEl.textContent = '⚠️ Car Name & Model is required for Driver registration.';
            return;
        }
        if (!vehicleNumber) {
            errorEl.textContent = '⚠️ Car Registration Number is required for Driver registration.';
            return;
        }
        if (!vehicleColor) {
            errorEl.textContent = '⚠️ Car Color is required for Driver registration.';
            return;
        }
    }

    btn.innerHTML = '<span class="btn-spinner"></span> Registering...';
    btn.disabled = true;

    const payload = {
        fullName,
        username,
        email,
        phone,
        gender,
        password,
        userType: registerUserType,
        driverLicenseNumber: registerUserType === 'DRIVER' ? license : null,
        vehicleModel: registerUserType === 'DRIVER' ? vehicleModel : null,
        vehicleNumber: registerUserType === 'DRIVER' ? vehicleNumber : null,
        vehicleColor: registerUserType === 'DRIVER' ? vehicleColor : null
    };

    try {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Registration failed');
        }

        const data = await res.json();
        showDispatchToast('sms', '📱 SMS Welcome Alert', `Welcome to ShareMile, ${fullName}! Your account has been verified.`);
        showDispatchToast('email', '✉️ Email Confirmation', `Registration details sent to ${email}`);

        onLoginSuccess(data);
    } catch (err) {
        errorEl.textContent = err.message;
        btn.innerHTML = 'Complete Registration';
        btn.disabled = false;
    }
}

async function handleLogin() {
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    const errorEl = document.getElementById('login-error');
    const btn = document.getElementById('login-submit-btn');

    errorEl.textContent = '';

    if (!username || !password) {
        errorEl.textContent = 'Please enter both username and password.';
        return;
    }

    btn.innerHTML = '<span class="btn-spinner"></span> Signing in...';
    btn.disabled = true;

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            let msg = 'Invalid username or password.';
            try {
                const err = await response.json();
                msg = err.message || msg;
            } catch {}
            throw new Error(msg);
        }

        const data = await response.json();

        if (loginMode === 'admin' && data.role !== 'ROLE_ADMIN') {
            throw new Error('Access denied. Administrator privileges required.');
        }

        onLoginSuccess(data);
    } catch (err) {
        errorEl.textContent = err.message;
        btn.innerHTML = 'Sign In to ShareMile';
        btn.disabled = false;
    }
}

function onLoginSuccess(data) {
    currentUser = data;
    jwtToken = data.token;
    localStorage.setItem('sharemile_token', jwtToken);
    localStorage.setItem('sharemile_user', JSON.stringify(data));

    document.getElementById('login-screen').style.display = 'none';
    document.getElementById('main-app').style.display = 'block';

    updateUserUI();
    connectWebSocket();
    // Show search landing page, NOT results page on login
    showSearchLanding();
    // Wire up location autocomplete
    setTimeout(initLocationAutocomplete, 200);

    if (currentUser.role === 'ROLE_ADMIN') {
        const adminBtn = document.getElementById('tab-btn-analytics');
        if (adminBtn) adminBtn.style.display = 'inline-block';
    }
}

function restoreSession() {
    const storedUser = localStorage.getItem('sharemile_user');
    if (storedUser) {
        try {
            currentUser = JSON.parse(storedUser);
            document.getElementById('login-screen').style.display = 'none';
            document.getElementById('main-app').style.display = 'block';
            updateUserUI();
            connectWebSocket();
            showSearchLanding();
            setTimeout(initLocationAutocomplete, 200);

            if (currentUser.role === 'ROLE_ADMIN') {
                const adminBtn = document.getElementById('tab-btn-analytics');
                if (adminBtn) adminBtn.style.display = 'inline-block';
            }
        } catch {
            handleLogout();
        }
    }
}

function handleLogout() {
    localStorage.removeItem('sharemile_token');
    localStorage.removeItem('sharemile_user');
    currentUser = null;
    jwtToken = '';
    if (stompClient) stompClient.disconnect();
    location.reload();
}

function updateUserUI() {
    if (!currentUser) return;
    const isDriver = (currentUser.role === 'ROLE_DRIVER' || (currentUser.driverLicenseNumber && currentUser.driverLicenseNumber.trim().length > 0));
    const roleBadge = currentUser.role === 'ROLE_ADMIN'
        ? '<span style="font-size:10.5px; background:rgba(239,68,68,0.2); color:#f87171; padding:2px 7px; border-radius:10px; font-weight:700; margin-left:5px;">🛡️ Admin</span>'
        : isDriver
            ? '<span style="font-size:10.5px; background:rgba(16,185,129,0.2); color:#10b981; padding:2px 7px; border-radius:10px; font-weight:700; margin-left:5px;">🚖 Driver</span>'
            : '<span style="font-size:10.5px; background:rgba(59,130,246,0.2); color:#60a5fa; padding:2px 7px; border-radius:10px; font-weight:700; margin-left:5px;">👤 Passenger</span>';

    document.getElementById('user-name-text').innerHTML = `${currentUser.fullName} ${roleBadge}`;
    document.getElementById('user-avatar-circle').innerText = currentUser.fullName.charAt(0).toUpperCase();
}

// ═══════════════════════════════════════════════
// MAP & GEOLOCATION LOGIC
// ═══════════════════════════════════════════════
function initLeafletMap() {
    if (map) return;
    map = L.map('leaflet-map').setView(DEFAULT_CENTER, 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '© OpenStreetMap contributors'
    }).addTo(map);

    markersLayer = L.layerGroup().addTo(map);
    driverTrackingLayer = L.layerGroup().addTo(map);
}

function detectCurrentLocation() {
    if (!navigator.geolocation) {
        alert("Geolocation is not supported by your browser.");
        return;
    }

    const pickupInput = document.getElementById('search-pickup');
    pickupInput.value = "📍 Acquiring GPS position...";

    navigator.geolocation.getCurrentPosition(
        (pos) => {
            currentPassengerLat = pos.coords.latitude;
            currentPassengerLng = pos.coords.longitude;

            pickupInput.value = `📍 Live GPS Location (${currentPassengerLat.toFixed(3)}, ${currentPassengerLng.toFixed(3)})`;

            if (map && markersLayer) {
                // Drop prominent user pulse marker
                const userIcon = L.divIcon({
                    className: 'user-gps-pulse-icon',
                    html: `<div style="background: #2563eb; width: 18px; height: 18px; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 14px rgba(37,99,235,0.8);"><div class="eta-pulse" style="width:12px;height:12px;margin:-1px 0 0 -1px;"></div></div>`,
                    iconSize: [24, 24],
                    iconAnchor: [12, 12]
                });

                L.marker([currentPassengerLat, currentPassengerLng], { icon: userIcon })
                    .addTo(markersLayer)
                    .bindPopup(`<b>📍 You Are Here</b><br>Searching carpools near your location`)
                    .openPopup();

                map.setView([currentPassengerLat, currentPassengerLng], 14);
            }

            showDispatchToast('info', '📍 Live GPS Detected', 'Location locked! Finding carpools nearby.');
            executeSearch();
        },
        (err) => {
            console.warn("Geolocation fallback:", err.message);
            // Default to Hinjawadi Phase 1
            currentPassengerLat = 18.5913;
            currentPassengerLng = 73.7389;
            pickupInput.value = "Hinjawadi Phase 1, Pune (GPS Default)";
            executeSearch();
        },
        { enableHighAccuracy: true, timeout: 8000, maximumAge: 60000 }
    );
}

// ═══════════════════════════════════════════════
// WEBSOCKET & NOTIFICATIONS
// ═══════════════════════════════════════════════
function connectWebSocket() {
    if (!currentUser) return;
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Suppress verbose stomp logs

    stompClient.connect({}, () => {
        stompClient.subscribe(`/topic/notifications/${currentUser.id}`, (frame) => {
            const notif = JSON.parse(frame.body);
            showInAppNotification(notif);

            // Pop toast alert
            if (notif.type === 'BOOKING_REQUEST') {
                showDispatchToast('sms', '📥 Incoming Ride Request', notif.message);
                if (document.getElementById('tab-driver-requests').style.display !== 'none') {
                    loadDriverIncomingRequests();
                }
            } else if (notif.type === 'BOOKING_ACCEPTED') {
                showDispatchToast('sms', '🎉 Ride Confirmed!', notif.message);
                if (document.getElementById('tab-bookings').style.display !== 'none') {
                    loadMyBookings();
                }
            } else if (notif.type === 'ADMIN_COMPLAINT_ALERT' || notif.type === 'COMPLAINT_ALERT') {
                showDispatchToast('danger', '🚨 Safety Incident Reported', notif.message);
                const adminTab = document.getElementById('tab-analytics');
                if (adminTab && adminTab.style.display !== 'none') {
                    loadAdminPanelData();
                }
            } else if (notif.type === 'ACCOUNT_SUSPENDED') {
                showDispatchToast('danger', '⛔ Account Notice', notif.message);
                alert("⚠️ NOTICE: " + notif.message);
            } else if (notif.type === 'ACCOUNT_RESTORED' || notif.type === 'COMPLAINT_RESOLVED') {
                showDispatchToast('info', '🛡️ Compliance Update', notif.message);
            }
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
        <p style="margin: 3px 0; color: var(--dark);">${notif.message}</p>
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

// ═══════════════════════════════════════════════
// GEOCODING & LOCATION AUTOCOMPLETE
// ═══════════════════════════════════════════════

// Geocode a place name to lat/lng using Nominatim (OpenStreetMap)
async function geocodeLocation(query) {
    if (!query || query.trim().length < 2) return null;
    try {
        // Bias results towards India
        const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query + ', India')}&format=json&limit=1&accept-language=en`;
        const res = await fetch(url, { headers: { 'Accept-Language': 'en' } });
        const data = await res.json();
        if (data && data.length > 0) {
            return { lat: parseFloat(data[0].lat), lng: parseFloat(data[0].lon), displayName: data[0].display_name };
        }
    } catch (err) {
        console.warn('Geocoding failed:', err);
    }
    return null;
}

// Fetch autocomplete suggestions from Nominatim
async function fetchLocationSuggestions(query) {
    if (!query || query.trim().length < 3) return [];
    try {
        const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query + ' India')}&format=json&limit=5&accept-language=en&addressdetails=1`;
        const res = await fetch(url, { headers: { 'Accept-Language': 'en' } });
        const data = await res.json();
        return data.map(item => ({
            display: item.display_name.split(',').slice(0, 3).join(', '),
            full: item.display_name,
            lat: parseFloat(item.lat),
            lng: parseFloat(item.lon)
        }));
    } catch { return []; }
}

// Debounce timer store
const _autocompleteTimers = {};

function initLocationAutocomplete() {
    // Map: inputId -> { onSelect: fn(lat, lng, display) }
    const fields = [
        {
            inputId: 'search-pickup',
            dropdownId: 'suggest-pickup',
            onSelect: (lat, lng, display) => {
                currentPassengerLat = lat;
                currentPassengerLng = lng;
                document.getElementById('search-pickup').value = display;
            }
        },
        {
            inputId: 'search-drop',
            dropdownId: 'suggest-drop',
            onSelect: (lat, lng, display) => {
                currentDropLat = lat;
                currentDropLng = lng;
                document.getElementById('search-drop').value = display;
            }
        },
        {
            inputId: 'pub-origin-title',
            dropdownId: 'suggest-pub-origin',
            onSelect: (lat, lng, display) => {
                window._pubOriginLat = lat;
                window._pubOriginLng = lng;
                document.getElementById('pub-origin-title').value = display;
            }
        },
        {
            inputId: 'pub-dest-title',
            dropdownId: 'suggest-pub-dest',
            onSelect: (lat, lng, display) => {
                window._pubDestLat = lat;
                window._pubDestLng = lng;
                document.getElementById('pub-dest-title').value = display;
            }
        }
    ];

    fields.forEach(({ inputId, dropdownId, onSelect }) => {
        const input = document.getElementById(inputId);
        if (!input) return;

        // Create suggestions dropdown if not exists
        if (!document.getElementById(dropdownId)) {
            const dd = document.createElement('div');
            dd.id = dropdownId;
            dd.className = 'location-suggest-dropdown';
            input.parentNode.style.position = 'relative';
            input.parentNode.appendChild(dd);
        }

        input.addEventListener('input', () => {
            clearTimeout(_autocompleteTimers[inputId]);
            const q = input.value.trim();
            if (q.length < 3) {
                hideSuggestions(dropdownId);
                return;
            }
            _autocompleteTimers[inputId] = setTimeout(async () => {
                const suggestions = await fetchLocationSuggestions(q);
                renderSuggestions(dropdownId, suggestions, onSelect, inputId);
            }, 350);
        });

        input.addEventListener('blur', () => {
            setTimeout(() => hideSuggestions(dropdownId), 200);
        });
    });
}

function renderSuggestions(dropdownId, suggestions, onSelect, inputId) {
    const dd = document.getElementById(dropdownId);
    if (!dd) return;
    if (suggestions.length === 0) { hideSuggestions(dropdownId); return; }

    dd.innerHTML = suggestions.map((s, i) => `
        <div class="suggest-item" data-idx="${i}" title="${s.full}">
            <span class="suggest-pin">📍</span>
            <span>${s.display}</span>
        </div>
    `).join('');

    dd.querySelectorAll('.suggest-item').forEach((el, i) => {
        el.addEventListener('mousedown', () => {
            onSelect(suggestions[i].lat, suggestions[i].lng, suggestions[i].display);
            hideSuggestions(dropdownId);
        });
    });

    dd.style.display = 'block';
}

function hideSuggestions(dropdownId) {
    const dd = document.getElementById(dropdownId);
    if (dd) dd.style.display = 'none';
}

// ═══════════════════════════════════════════════
// SEARCH LANDING & RESULTS NAVIGATION
// ═══════════════════════════════════════════════
function showSearchLanding() {
    // Hide results page, show search landing
    document.getElementById('tab-search-results').style.display = 'none';
    document.getElementById('tab-rides').style.display = 'block';
    document.getElementById('hero-banner-section').style.display = 'block';
    // Hide other tabs
    ['bookings', 'driver-requests', 'analytics'].forEach(t => {
        const el = document.getElementById(`tab-${t}`);
        if (el) el.style.display = 'none';
    });
    // Update nav active state
    ['rides', 'bookings', 'driver-requests', 'analytics'].forEach(t => {
        const btn = document.getElementById(`tab-btn-${t}`);
        if (btn) btn.classList.remove('active');
    });
    document.getElementById('tab-btn-rides')?.classList.add('active');
    // Re-init autocomplete since DOM is now visible
    setTimeout(initLocationAutocomplete, 100);
}

function backToSearch() {
    document.getElementById('tab-search-results').style.display = 'none';
    document.getElementById('tab-rides').style.display = 'block';
    document.getElementById('hero-banner-section').style.display = 'block';
    setTimeout(initLocationAutocomplete, 100);
}

// ═══════════════════════════════════════════════
// SEARCH RIDES & CO-PASSENGER TRANSPARENCY
// ═══════════════════════════════════════════════
async function executeSearch() {
    // Ensure map is initialized before showing results
    initLeafletMap();

    if (markersLayer) markersLayer.clearLayers();
    if (driverTrackingLayer) driverTrackingLayer.clearLayers();

    const pickup = document.getElementById('search-pickup').value.trim();
    const drop = document.getElementById('search-drop').value.trim();
    const genderFilter = document.getElementById('search-gender-filter').value;

    // Show a loading indicator on the button
    const btn = document.getElementById('btn-find-rides');
    if (btn) { btn.innerHTML = '<span class="btn-spinner"></span> Locating...'; btn.disabled = true; }

    // === GEOCODE PICKUP ===
    if (pickup && pickup.length > 2) {
        const geo = await geocodeLocation(pickup);
        if (geo) {
            currentPassengerLat = geo.lat;
            currentPassengerLng = geo.lng;
        }
    }

    // === GEOCODE DROP ===
    if (drop && drop.length > 2) {
        const geo = await geocodeLocation(drop);
        if (geo) {
            currentDropLat = geo.lat;
            currentDropLng = geo.lng;
        }
    }

    if (btn) { btn.innerHTML = '🔍 Find Rides'; btn.disabled = false; }

    // Update results route label
    const routeLabel = document.getElementById('results-route-label');
    if (routeLabel && pickup && drop) {
        routeLabel.textContent = `· ${pickup} → ${drop}`;
    }

    const searchPayload = {
        pickupLat: currentPassengerLat,
        pickupLng: currentPassengerLng,
        dropLat: currentDropLat,
        dropLng: currentDropLng,
        radiusKm: Number(document.getElementById('search-radius').value),
        seatsNeeded: 1,
        genderPreference: genderFilter,
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

        // Navigate to results page
        document.getElementById('tab-rides').style.display = 'none';
        document.getElementById('hero-banner-section').style.display = 'none';
        document.getElementById('tab-search-results').style.display = 'block';
        // Invalidate map after becoming visible
        setTimeout(() => { if (map) map.invalidateSize(); }, 150);
    } catch (err) {
        console.error("Search failed:", err);
        if (btn) { btn.innerHTML = '🔍 Find Rides'; btn.disabled = false; }
    }
}

function renderSearchResults(results) {
    const list = document.getElementById('rides-list');
    // Update both count displays
    const count = results.length;
    const rc = document.getElementById('results-count');
    const rc2 = document.getElementById('results-count-2');
    if (rc) rc.innerText = count;
    if (rc2) rc2.innerText = count;
    list.innerHTML = '';

    if (results.length === 0) {
        list.innerHTML = `
            <div style="background: var(--card-bg); padding: 34px; text-align: center; border-radius: 14px; border: 1px solid var(--border);">
                <p style="font-size: 16px; font-weight: 700;">No matching carpools found.</p>
                <p style="font-size: 13.5px; color: var(--dark-muted); margin-top: 6px;">Try expanding your radius slider or selecting "All Rides" filter.</p>
            </div>
        `;
        return;
    }

    const bounds = [];

    results.forEach((res, index) => {
        const r = res.ride;
        const card = document.createElement('div');
        card.className = 'ride-card';

        // Gender Policy Badge
        const genderPref = res.genderPreference || 'ANY';
        let genderBadgeHtml = '';
        if (genderPref === 'FEMALE_ONLY') {
            genderBadgeHtml = `<span class="badge-gender gender-girls">🌸 Girls Only Ride</span>`;
        } else if (genderPref === 'MALE_ONLY') {
            genderBadgeHtml = `<span class="badge-gender gender-boys">👔 Boys Only Ride</span>`;
        } else {
            genderBadgeHtml = `<span class="badge-gender gender-coed">👥 All Commuters Welcome</span>`;
        }

        // Co-Passengers on board
        const confirmed = res.confirmedPassengers || [];
        let passengersHtml = '';
        if (confirmed.length === 0) {
            passengersHtml = `<span class="passenger-empty">🟢 No co-passengers booked yet — be the first to reserve!</span>`;
        } else {
            passengersHtml = confirmed.map(p => `
                <span class="passenger-chip" title="Co-traveler detail: ${p.passengerNamesDetail || p.passengerName}">
                    👤 ${p.passengerName} (${p.gender === 'FEMALE' ? 'Female 🌸' : p.gender === 'MALE' ? 'Male 👔' : 'Other'}) • ${p.seatsBooked} seat(s)
                </span>
            `).join('');
        }

        const ratingDisplay = (r.driver.totalRatings > 0)
            ? `⭐ ${r.driver.averageRating} (${r.driver.totalRatings} trips)`
            : `<span class="new-driver-badge">🔰 New Driver (0 trips)</span>`;

        card.innerHTML = `
            <div class="card-top">
                <div class="driver-profile">
                    <div class="driver-avatar">${r.driver.fullName.charAt(0)}</div>
                    <div class="driver-info">
                        <h4>${r.driver.fullName} <span style="color:var(--success);font-size:12px;">✓ Verified Driver</span></h4>
                        <div class="driver-sub">${r.driver.vehicleModel || 'Standard Car'} ${r.driver.vehicleColor ? '(' + r.driver.vehicleColor + ')' : ''} ${r.driver.vehicleNumber ? '• ' + r.driver.vehicleNumber : ''} • ${ratingDisplay}</div>
                    </div>
                </div>
                <div style="display:flex; flex-direction:column; align-items:flex-end; gap:6px;">
                    <div class="match-badge">🎯 ${res.matchScore}% Match</div>
                    ${genderBadgeHtml}
                </div>
            </div>

            <!-- Driver Live Location & ETA to Pickup -->
            <div class="driver-eta-banner">
                <div>
                    <span class="eta-pulse"></span>
                    <strong>Driver Live Location:</strong> ~${res.driverDistanceKm} km away
                </div>
                <div style="font-weight: 700; color: var(--primary);">
                    ⏱️ Reaching you in ~${res.driverEtaMinutes} mins
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

            <!-- Co-Passenger Transparency Section -->
            <div class="co-passengers-box">
                <div class="co-passengers-title">
                    <span>👥 Passengers Currently Booked (${r.totalSeats - r.availableSeats}/${r.totalSeats} seats)</span>
                    <span style="font-weight:400; text-transform:none; color:var(--dark-muted);">Commuter Comfort Verification</span>
                </div>
                <div class="passenger-chip-list">
                    ${passengersHtml}
                </div>
            </div>

            <div style="display: flex; gap: 8px; font-size: 12px; color: var(--dark-muted); margin-bottom: 10px; flex-wrap: wrap;">
                <span>🕒 Departure: ${new Date(r.departureTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                <span>•</span>
                <span>📏 Distance: ${r.estimatedDistanceKm} km</span>
                <span>•</span>
                <span class="carbon-badge">🌱 ~${res.estimatedCarbonSavingsKg} kg CO₂ offset</span>
            </div>

            <div class="card-metrics">
                <div>
                    <span class="price-tag">₹${r.pricePerSeat}</span>
                    <span style="font-size: 12px; color: var(--dark-muted);"> / seat</span>
                    <span class="seats-pill" style="margin-left: 8px;">${r.availableSeats} seat(s) remaining</span>
                </div>
                <div style="display:flex; gap: 8px;">
                    <button class="btn-outline" style="font-size: 13px; padding: 7px 12px;" onclick="trackDriverOnMap(${r.id}, ${res.driverCurrentLat}, ${res.driverCurrentLng}, ${r.originLat}, ${r.originLng}, ${r.destLat}, ${r.destLng}, '${r.driver.fullName}', ${res.driverEtaMinutes})">
                        📍 Track on Map
                    </button>
                    <button class="btn-primary" style="font-size: 13px; padding: 7px 16px;" onclick="openBookingModal(${r.id})">
                        Book Seat
                    </button>
                </div>
            </div>
        `;
        list.appendChild(card);

        // Map Origin & Destination
        L.marker([r.originLat, r.originLng]).addTo(markersLayer)
            .bindPopup(`<b>Ride Origin</b>: ${r.originTitle}<br>Driver: ${r.driver.fullName}`);
        L.marker([r.destLat, r.destLng]).addTo(markersLayer)
            .bindPopup(`<b>Destination</b>: ${r.destTitle}<br>Fare: ₹${r.pricePerSeat}`);

        L.polyline([[r.originLat, r.originLng], [r.destLat, r.destLng]], {
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

// ═══════════════════════════════════════════════
// LIVE DRIVER TRACKING ON MAP
// ═══════════════════════════════════════════════
function trackDriverOnMap(rideId, driverLat, driverLng, originLat, originLng, destLat, destLng, driverName, etaMinutes) {
    if (!map || !driverTrackingLayer) return;
    driverTrackingLayer.clearLayers();

    const carIcon = L.divIcon({
        className: 'driver-live-car-icon',
        html: `<div style="background:#0ea5e9; color:white; width:34px; height:34px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:18px; box-shadow: 0 0 16px rgba(14,165,233,0.9); border:2px solid white;"><span class="eta-pulse" style="position:absolute; width:34px; height:34px; margin:0; border-radius:50%;"></span>🚗</div>`,
        iconSize: [36, 36],
        iconAnchor: [18, 18]
    });

    const driverMarker = L.marker([driverLat, driverLng], { icon: carIcon }).addTo(driverTrackingLayer)
        .bindPopup(`<b>🚖 Driver ${driverName} Live Location</b><br>Estimated Time of Arrival: <strong>~${etaMinutes} mins</strong> to pickup!`)
        .openPopup();

    // Highlight route line
    L.polyline([[driverLat, driverLng], [currentPassengerLat, currentPassengerLng], [destLat, destLng]], {
        color: '#0ea5e9',
        weight: 5,
        dashArray: '8, 8',
        opacity: 0.95
    }).addTo(driverTrackingLayer);

    map.setView([driverLat, driverLng], 14);
    showDispatchToast('info', '🛰️ Driver Live Tracking', `Driver ${driverName} is ~${etaMinutes} minutes away from your pickup point.`);
}

// ═══════════════════════════════════════════════
// BOOKING REQUEST & CO-PASSENGER SPECIFICATION
// ═══════════════════════════════════════════════
function openBookingModal(rideId) {
    const resultObj = currentSearchResults.find(r => r.ride.id === rideId);
    if (!resultObj) return;

    const r = resultObj.ride;
    const content = document.getElementById('booking-modal-content');
    const defaultPassengerName = currentUser ? currentUser.fullName : '';

    content.innerHTML = `
        <div style="margin-bottom: 14px;">
            <h4 style="font-size: 16px;">${r.originTitle} → ${r.destTitle}</h4>
            <p style="font-size: 13px; color: var(--dark-muted);">Driver: <strong>${r.driver.fullName}</strong> • 🚗 <strong>${r.driver.vehicleModel || 'Standard Vehicle'}</strong> ${r.driver.vehicleColor ? '(' + r.driver.vehicleColor + ')' : ''} ${r.driver.vehicleNumber ? '[' + r.driver.vehicleNumber + ']' : ''}</p>
        </div>

        <div style="background: var(--light-bg); padding: 14px; border-radius: 10px; margin-bottom: 16px; font-size: 13px; border: 1px solid var(--border-subtle);">
            <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
                <span>Fare per Seat:</span>
                <strong>₹${r.pricePerSeat}</strong>
            </div>
            <div style="display: flex; justify-content: space-between; margin-bottom: 6px;">
                <span>Available Seats:</span>
                <strong>${r.availableSeats}</strong>
            </div>
            <div style="display: flex; justify-content: space-between; color: var(--success);">
                <span>Estimated CO₂ Offset:</span>
                <strong>~${resultObj.estimatedCarbonSavingsKg} kg</strong>
            </div>
            <div style="display: flex; justify-content: space-between; margin-top: 6px; padding-top: 6px; border-top: 1px dashed var(--border);">
                <span>Driver ETA:</span>
                <strong style="color:var(--primary);">~${resultObj.driverEtaMinutes} mins to pickup</strong>
            </div>
        </div>

        <div class="form-group" style="margin-bottom: 14px;">
            <label>Select Number of Seats</label>
            <input type="number" id="book-seats-count" min="1" max="${r.availableSeats}" value="1" oninput="updateBookingTotal(${r.pricePerSeat})">
        </div>

        <div class="form-group" style="margin-bottom: 14px;">
            <label>Name(s) of Passenger(s) Traveling <span style="font-size:10px; color:var(--dark-muted);">(shown to driver &amp; co-commuters)</span></label>
            <input type="text" id="book-passenger-names" value="${defaultPassengerName}" placeholder="e.g. Khushi Singh + 1 friend">
        </div>

        <div class="form-group" style="margin-bottom: 16px;">
            <label>Special Notes / Luggage Info (Optional)</label>
            <input type="text" id="book-note" placeholder="e.g. 1 small laptop bag, comfortable with music">
        </div>

        <div style="background: rgba(245,158,11,0.1); border: 1px solid rgba(245,158,11,0.3); border-radius: 9px; padding: 10px 12px; margin-bottom: 18px; font-size: 12px; color: var(--warning);">
            ℹ️ <strong>Driver Approval Required:</strong> Your booking will be submitted with status <strong>PENDING</strong>. The driver will review your request, and the ride is confirmed only once accepted.
        </div>

        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; font-size: 15px;">
            <span>Total Payable Amount:</span>
            <span id="booking-total-display" style="font-size: 22px; font-weight: 800; color: var(--primary);">₹${r.pricePerSeat}</span>
        </div>

        <button class="btn-primary" style="width: 100%; padding: 13px; font-size: 14.5px;" onclick="confirmBookingAtomic(${r.id})">
            Submit Booking Request to Driver
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
    const passengerNames = document.getElementById('book-passenger-names').value.trim();
    const note = document.getElementById('book-note').value.trim();

    try {
        const res = await fetch('/api/bookings', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify({
                rideId: rideId,
                seatsBooked: seats,
                passengerNames: passengerNames,
                note: note,
                pickupLat: currentPassengerLat,
                pickupLng: currentPassengerLng,
                dropLat: currentDropLat,
                dropLng: currentDropLng
            })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || "Booking failed");
        }

        const booking = await res.json();
        closeModal('modal-booking');

        // Instant Multi-Channel Alerts
        showDispatchToast('sms', '📱 SMS Dispatched to Driver', `Booking request for ${seats} seat(s) forwarded to driver.`);
        showDispatchToast('email', '✉️ Email Sent to Passenger', `Your booking #${booking.id} request is PENDING driver approval.`);

        alert(`✅ Booking Request #${booking.id} Submitted!\n\nStatus: ⏳ PENDING DRIVER APPROVAL.\nThe driver has been notified via WebSocket STOMP, Email and SMS to review and confirm your ride.`);
        executeSearch();
    } catch (err) {
        alert("Booking Error: " + err.message);
    }
}

// ═══════════════════════════════════════════════
function openPublishModal() {
    if (!currentUser) return;

    // Strict guard: passengers cannot publish rides without DL and Car verification
    const isDriver = (currentUser.role === 'ROLE_DRIVER' || currentUser.role === 'ROLE_ADMIN') &&
                     currentUser.driverLicenseNumber && currentUser.driverLicenseNumber.trim().length > 0 &&
                     currentUser.vehicleModel && currentUser.vehicleModel.trim().length > 0;

    if (!isDriver) {
        openDriverVerificationModal();
        return;
    }

    const carInfoEl = document.getElementById('pub-driver-car-info');
    if (carInfoEl) {
        carInfoEl.style.display = 'block';
        carInfoEl.innerHTML = `🚗 <strong>Registered Car:</strong> ${currentUser.vehicleModel} (${currentUser.vehicleColor || 'Color N/A'}) • <strong>${currentUser.vehicleNumber || 'Reg Pending'}</strong> • DL: <strong>${currentUser.driverLicenseNumber}</strong> <span style="color:var(--success); font-weight:700; margin-left:6px;">✓ Verified</span>`;
    }

    document.getElementById('modal-publish').style.display = 'flex';
}

function openDriverVerificationModal() {
    if (currentUser) {
        if (currentUser.driverLicenseNumber) document.getElementById('verify-license').value = currentUser.driverLicenseNumber;
        if (currentUser.vehicleModel) document.getElementById('verify-vehicle-model').value = currentUser.vehicleModel;
        if (currentUser.vehicleNumber) document.getElementById('verify-vehicle-number').value = currentUser.vehicleNumber;
        if (currentUser.vehicleColor) document.getElementById('verify-vehicle-color').value = currentUser.vehicleColor;
    }
    document.getElementById('modal-driver-verification').style.display = 'flex';
}

async function handleDriverVerification(e) {
    e.preventDefault();
    const license = document.getElementById('verify-license').value.trim();
    const vehicleModel = document.getElementById('verify-vehicle-model').value.trim();
    const vehicleNumber = document.getElementById('verify-vehicle-number').value.trim();
    const vehicleColor = document.getElementById('verify-vehicle-color').value.trim();
    const btn = document.getElementById('btn-verify-driver-submit');

    if (!license || !vehicleModel || !vehicleNumber || !vehicleColor) {
        alert('Please fill in Driving License Number, Car Name, Car Number, and Car Color.');
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span class="btn-spinner"></span> Verifying & Activating...';

    try {
        const res = await fetch('/api/auth/verify-driver', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify({
                driverLicenseNumber: license,
                vehicleModel,
                vehicleNumber,
                vehicleColor
            })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Driver verification failed');
        }

        const updatedUser = await res.json();
        currentUser = updatedUser;
        if (updatedUser.token) {
            jwtToken = updatedUser.token;
            localStorage.setItem('sharemile_token', jwtToken);
        }
        localStorage.setItem('sharemile_user', JSON.stringify(currentUser));

        closeModal('modal-driver-verification');
        updateUserUI();

        showDispatchToast('sms', '🛡️ Driver Verified', `Congratulations ${currentUser.fullName}! Your Driving License (${license}) and vehicle have been verified.`);
        alert('🎉 Driver & Vehicle Verification Successful! Your profile is now authorized to offer carpool rides.');

        openPublishModal();
    } catch (err) {
        alert('Verification Failed: ' + err.message);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '✓ Verify & Activate Driver Profile';
    }
}

async function handlePublishRide(e) {
    e.preventDefault();

    const isDriver = (currentUser.role === 'ROLE_DRIVER' || currentUser.role === 'ROLE_ADMIN') &&
                     currentUser.driverLicenseNumber && currentUser.driverLicenseNumber.trim().length > 0;
    if (!isDriver) {
        closeModal('modal-publish');
        openDriverVerificationModal();
        return;
    }

    const originTitle = document.getElementById('pub-origin-title').value.trim();
    const destTitle = document.getElementById('pub-dest-title').value.trim();
    const submitBtn = e.target.querySelector('button[type="submit"]');
    const originalBtnText = submitBtn ? submitBtn.innerHTML : 'Publish Ride Now';
    if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="btn-spinner"></span> Geocoding & Publishing...';
    }

    // Geocode if not already selected from autocomplete dropdown
    let originLat = window._pubOriginLat;
    let originLng = window._pubOriginLng;
    if (!originLat && originTitle.length > 2) {
        const geo = await geocodeLocation(originTitle);
        if (geo) {
            originLat = geo.lat;
            originLng = geo.lng;
        } else {
            originLat = 18.6517; // Nigdi default fallback
            originLng = 73.7716;
        }
    } else if (!originLat) {
        originLat = 18.6517;
        originLng = 73.7716;
    }

    let destLat = window._pubDestLat;
    let destLng = window._pubDestLng;
    if (!destLat && destTitle.length > 2) {
        const geo = await geocodeLocation(destTitle);
        if (geo) {
            destLat = geo.lat;
            destLng = geo.lng;
        } else {
            destLat = 18.5284; // Pune Station fallback
            destLng = 73.8739;
        }
    } else if (!destLat) {
        destLat = 18.5284;
        destLng = 73.8739;
    }

    const payload = {
        originTitle,
        originLat,
        originLng,
        destTitle,
        destLat,
        destLng,
        departureTime: document.getElementById('pub-time').value + ":00",
        totalSeats: Number(document.getElementById('pub-seats').value),
        pricePerSeat: Number(document.getElementById('pub-price').value),
        genderPreference: document.getElementById('pub-gender-pref').value
    };

    // Reset coords for next use
    window._pubOriginLat = null;
    window._pubOriginLng = null;
    window._pubDestLat = null;
    window._pubDestLng = null;

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
        showDispatchToast('email', '✉️ Ride Published Online', `Your ride has been listed with ${payload.genderPreference} preference!`);
        alert("🎉 Ride published successfully! Commuters along your route can now request bookings.");
        executeSearch();
        if (document.getElementById('tab-driver-requests').style.display !== 'none') {
            loadDriverIncomingRequests();
        }
    } catch (err) {
        alert("Error: " + err.message);
    } finally {
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalBtnText;
        }
    }
}

// ═══════════════════════════════════════════════
// TABS NAVIGATION
// ═══════════════════════════════════════════════
function showTab(tabName) {
    // Hide results page and all main tabs
    document.getElementById('tab-search-results').style.display = 'none';
    ['rides', 'bookings', 'driver-requests', 'analytics'].forEach(t => {
        const el = document.getElementById(`tab-${t}`);
        const btn = document.getElementById(`tab-btn-${t}`);
        if (el) el.style.display = 'none';
        if (btn) btn.classList.remove('active');
    });

    if (tabName === 'rides') {
        // Go back to search landing page
        showSearchLanding();
        return;
    }

    const target = document.getElementById(`tab-${tabName}`);
    const targetBtn = document.getElementById(`tab-btn-${tabName}`);
    if (target) target.style.display = 'grid';
    if (targetBtn) targetBtn.classList.add('active');

    // Hide hero banner on non-rides tabs
    const heroBanner = document.getElementById('hero-banner-section');
    if (heroBanner) heroBanner.style.display = 'none';

    if (tabName === 'bookings') loadMyBookings();
    else if (tabName === 'driver-requests') loadDriverIncomingRequests();
    else if (tabName === 'analytics') loadAdminPanelData();
}

// ═══════════════════════════════════════════════
// PASSENGER: MY BOOKINGS
// ═══════════════════════════════════════════════
async function loadMyBookings() {
    const list = document.getElementById('my-bookings-list');
    list.innerHTML = '<p>Loading your seat reservations...</p>';

    try {
        const res = await fetch('/api/bookings/my-bookings', {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        const bookings = await res.json();

        if (bookings.length === 0) {
            list.innerHTML = '<p style="color: var(--dark-muted);">You have no active or previous bookings.</p>';
            return;
        }

        list.innerHTML = bookings.map(b => {
            const isConfirmed = b.status === 'ACCEPTED';
            const isPending = b.status === 'PENDING';
            const isRejected = b.status === 'REJECTED';
            const statusClass = isConfirmed ? 'status-accepted' : isPending ? 'status-pending' : isRejected ? 'status-rejected' : 'status-cancelled';
            const statusLabel = isConfirmed ? '✅ Confirmed by Driver' : isPending ? '⏳ Awaiting Driver Approval' : isRejected ? '❌ Declined by Driver' : '⚪ Cancelled';

            return `
                <div style="background: var(--light-bg); padding: 20px; border-radius: 12px; border: 1px solid var(--border); display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 14px;">
                    <div>
                        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
                            <span class="booking-status-pill ${statusClass}">${statusLabel}</span>
                            <span style="font-size: 11.5px; color: var(--dark-muted);">Booking #${b.id}</span>
                        </div>
                        <h4 style="font-size: 16px; margin: 4px 0;">${b.ride.originTitle} → ${b.ride.destTitle}</h4>
                        <p style="font-size: 13.5px; color: var(--dark-muted);">Driver: <strong>${b.ride.driver.fullName}</strong> (${b.ride.driver.vehicleModel || 'Standard Car'}) • ${b.seatsBooked} seat(s) • Total Fare: <strong>₹${b.totalFare}</strong></p>
                        ${b.passengerNames ? `<p style="font-size: 12.5px; color: var(--dark); margin-top: 4px;">👥 <strong>Traveling:</strong> ${b.passengerNames}</p>` : ''}
                        ${b.note ? `<p style="font-size: 12px; color: var(--dark-muted); margin-top: 2px;">📝 <em>${b.note}</em></p>` : ''}
                        <div style="margin-top: 8px;">
                            <span class="carbon-badge">🌱 ${b.carbonOffsetKg} kg CO₂ saved</span>
                        </div>
                    </div>
                    <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
                        ${isConfirmed ? `<button class="btn-outline" onclick="openRatingModal(${b.id}, ${b.ride.driver.id})">⭐ Rate Driver</button>` : ''}
                        <button class="btn-outline" style="color: var(--danger); border-color: rgba(239, 68, 68, 0.45); font-size: 12.5px; padding: 6px 12px;" onclick="openComplaintModal(${b.ride.driver.id}, '${escapeAttr(b.ride.driver.fullName)}', '${escapeAttr(b.ride.originTitle)} → ${escapeAttr(b.ride.destTitle)}')">
                            ⚠️ Report Issue
                        </button>
                        ${b.status !== 'CANCELLED' && b.status !== 'REJECTED' ? `<button class="btn-outline" style="color: var(--danger); border-color: var(--danger);" onclick="cancelBooking(${b.id})">Cancel</button>` : ''}
                    </div>
                </div>
            `;
        }).join('');
    } catch (err) {
        list.innerHTML = '<p>Could not load bookings.</p>';
    }
}

// ═══════════════════════════════════════════════
// DRIVER PORTAL: INCOMING REQUESTS & ACCEPT / REJECT
// ═══════════════════════════════════════════════
async function loadDriverIncomingRequests() {
    const pendingList = document.getElementById('driver-pending-requests-list');
    const ridesList = document.getElementById('driver-rides-list');

    pendingList.innerHTML = '<p>Loading incoming passenger requests...</p>';
    ridesList.innerHTML = '<p>Loading your published rides...</p>';

    try {
        // 1. Fetch incoming requests
        const resRequests = await fetch('/api/bookings/driver-requests', {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        const requests = await resRequests.json();

        const pending = requests.filter(r => r.status === 'PENDING');
        const processed = requests.filter(r => r.status !== 'PENDING');

        if (pending.length === 0) {
            pendingList.innerHTML = '<div style="background:var(--light-bg); padding:16px; border-radius:10px; border:1px solid var(--border-subtle);"><p style="color:var(--dark-muted); font-size:13px;">✅ You have no pending booking requests right now. Any incoming requests from commuters will appear here for your approval.</p></div>';
        } else {
            pendingList.innerHTML = pending.map(b => `
                <div class="booking-request-card" style="border-left: 4px solid var(--warning);">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 10px;">
                        <div>
                            <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 6px;">
                                <span class="booking-status-pill status-pending">Action Required</span>
                                <h4 style="font-size: 15.5px; margin: 0;">${b.passenger.fullName}</h4>
                                <span class="badge-gender ${b.passenger.gender === 'FEMALE' ? 'gender-girls' : 'gender-boys'}">
                                    ${b.passenger.gender === 'FEMALE' ? 'Female 🌸' : 'Male 👔'}
                                </span>
                            </div>
                            <p style="font-size: 13.5px; margin: 4px 0;">
                                Route: <strong>${b.ride.originTitle} → ${b.ride.destTitle}</strong>
                            </p>
                            <p style="font-size: 13px; color: var(--dark-muted);">
                                Requested Seats: <strong>${b.seatsBooked}</strong> • Total Fare: <strong style="color:var(--primary);">₹${b.totalFare}</strong>
                            </p>
                            ${b.passengerNames ? `<p style="font-size: 12.5px; margin-top: 4px;">👥 <strong>Traveling:</strong> ${b.passengerNames}</p>` : ''}
                            ${b.note ? `<p style="font-size: 12px; color: var(--dark-muted);">📝 Note: "${b.note}"</p>` : ''}
                        </div>
                        <div style="display: flex; gap: 8px; margin-top: 4px;">
                            <button class="btn-primary" style="background: var(--success); padding: 8px 16px;" onclick="respondBooking(${b.id}, true)">
                                ✅ Accept Request
                            </button>
                            <button class="btn-outline" style="color: var(--danger); border-color: var(--danger); padding: 8px 14px;" onclick="respondBooking(${b.id}, false)">
                                ❌ Decline
                            </button>
                        </div>
                    </div>
                </div>
            `).join('');
        }

        // 2. Fetch driver's published rides
        const resRides = await fetch('/api/rides/my-published', {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        const publishedRides = await resRides.json();

        if (publishedRides.length === 0) {
            ridesList.innerHTML = '<p style="color: var(--dark-muted); font-size:13px;">You have not published any rides yet. Click "Offer a New Ride" to start carpooling.</p>';
        } else {
            ridesList.innerHTML = publishedRides.map(r => `
                <div style="background: var(--light-bg); padding: 18px; border-radius: 12px; border: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
                    <div>
                        <div style="display:flex; align-items:center; gap:8px;">
                            <span class="seats-pill">${r.availableSeats}/${r.totalSeats} seats left</span>
                            <span class="badge-gender ${r.genderPreference === 'FEMALE_ONLY' ? 'gender-girls' : r.genderPreference === 'MALE_ONLY' ? 'gender-boys' : 'gender-coed'}">
                                ${r.genderPreference === 'FEMALE_ONLY' ? '🌸 Girls Only' : r.genderPreference === 'MALE_ONLY' ? '👔 Boys Only' : '👥 All Commuters'}
                            </span>
                        </div>
                        <h4 style="margin: 8px 0 4px;">${r.originTitle} → ${r.destTitle}</h4>
                        <p style="font-size: 13px; color: var(--dark-muted);">
                            Departure: ${new Date(r.departureTime).toLocaleString()} • Price: <strong>₹${r.pricePerSeat}/seat</strong> • Status: <strong>${r.status}</strong>
                        </p>
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <button class="btn-outline" style="font-size:12.5px; padding:6px 12px;" onclick="simulateDriverMovement(${r.id}, ${r.originLat}, ${r.originLng}, ${r.destLat}, ${r.destLng})">
                            📡 Broadcast Live GPS
                        </button>
                    </div>
                </div>
            `).join('');
        }
    } catch (err) {
        pendingList.innerHTML = '<p>Could not load requests.</p>';
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

        if (accept) {
            showDispatchToast('sms', '📱 SMS Dispatched to Passenger', 'Passenger has been notified that you CONFIRMED their ride!');
            showDispatchToast('email', '✉️ Confirmation Email Dispatched', 'Booking confirmation details sent to passenger email.');
            alert("🎉 Booking confirmed successfully! Passenger seat reservation is now locked.");
        } else {
            showDispatchToast('sms', '📱 SMS Dispatched', 'Passenger notified of declined request.');
        }

        loadDriverIncomingRequests();
        executeSearch();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function simulateDriverMovement(rideId, originLat, originLng, destLat, destLng) {
    // Simulate updating driver's live GPS coordinates towards passenger / destination
    const newLat = originLat + (destLat - originLat) * 0.3;
    const newLng = originLng + (destLng - originLng) * 0.3;

    try {
        const res = await fetch(`/api/rides/${rideId}/location`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify({ lat: newLat, lng: newLng })
        });
        if (!res.ok) throw new Error("Location update failed");

        showDispatchToast('info', '📡 Driver GPS Broadcasted', `Live coordinates updated on map! Commuters will see updated ~5 mins ETA.`);
        alert("🛰️ Live GPS broadcasted successfully!\nPassenger tracking map now reflects your vehicle moving on the Pune commute corridor.");
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
        showDispatchToast('sms', '📱 SMS Alert', 'Booking cancellation dispatched to driver.');
        loadMyBookings();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// ═══════════════════════════════════════════════
// RATINGS
// ═══════════════════════════════════════════════
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
        if (!res.ok) throw new Error("Failed to submit rating");
        closeModal('modal-rating');
        alert("Thank you! Your feedback has been recorded.");
    } catch (err) {
        alert("Error: " + err.message);
    }
}

// ═══════════════════════════════════════════════
// ANALYTICS
// ═══════════════════════════════════════════════
async function loadAnalytics() {
    try {
        const res = await fetch('/api/admin/analytics', {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        if (!res.ok) return;
        const data = await res.json();

        document.getElementById('metric-rides').innerText = data.totalRides;
        document.getElementById('metric-bookings').innerText = data.acceptedBookings;
        document.getElementById('metric-occupancy').innerText = `${data.occupancyRate}% Seat Occupancy`;
        document.getElementById('metric-carbon').innerText = `${data.totalCarbonSavedKg.toFixed(1)} kg`;
        document.getElementById('metric-fare').innerText = `₹${data.totalFareVolume.toFixed(0)}`;

        renderAnalyticsCharts(data);
    } catch (err) {
        console.error("Could not load analytics:", err);
    }
}

function renderAnalyticsCharts(data) {
    const hourlyCtx = document.getElementById('hourlyDemandChart');
    if (hourlyCtx) {
        if (hourlyChartInstance) hourlyChartInstance.destroy();
        hourlyChartInstance = new Chart(hourlyCtx, {
            type: 'bar',
            data: {
                labels: Object.keys(data.hourlyDemand).map(h => `${h}:00`),
                datasets: [{
                    label: 'Commuter Ride Demand',
                    data: Object.values(data.hourlyDemand),
                    backgroundColor: 'rgba(59, 130, 246, 0.75)',
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: { beginAtZero: true, ticks: { stepSize: 1 } }
                }
            }
        });
    }

    const routeCtx = document.getElementById('popularRoutesChart');
    if (routeCtx) {
        if (routesChartInstance) routesChartInstance.destroy();
        routesChartInstance = new Chart(routeCtx, {
            type: 'doughnut',
            data: {
                labels: Object.keys(data.popularRoutes),
                datasets: [{
                    data: Object.values(data.popularRoutes),
                    backgroundColor: ['#3b82f6', '#10b981', '#f59e0b', '#8b5cf6', '#ec4899']
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false
            }
        });
    }
}

function closeModal(id) {
    document.getElementById(id).style.display = 'none';
}

function escapeAttr(str) {
    if (!str) return '';
    return String(str).replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

// ═══════════════════════════════════════════════
// PASSENGER: REPORT DRIVER / SAFETY INCIDENT
// ═══════════════════════════════════════════════
function openComplaintModal(driverId, driverName, routeTitle) {
    const container = document.getElementById('complaint-modal-content');
    if (!container) return;

    container.innerHTML = `
        <div style="margin-bottom: 14px;">
            <p style="font-size: 13.5px; margin-bottom: 4px;">Reporting Driver: <strong style="color:var(--dark);">${driverName}</strong></p>
            <p style="font-size: 12px; color: var(--dark-muted);">Route: ${routeTitle}</p>
        </div>

        <form onsubmit="handleComplaintSubmit(event, ${driverId}, '${escapeAttr(driverName)}')">
            <div class="form-group" style="margin-bottom: 14px;">
                <label>Incident / Grievance Category</label>
                <select id="complaint-reason" required style="padding: 10px; font-size: 13.5px;">
                    <option value="Reckless or Dangerous Driving">⚠️ Reckless / High-Speed Dangerous Driving</option>
                    <option value="Inappropriate or Unsafe Conduct">🚫 Inappropriate or Harassing Behavior</option>
                    <option value="Demanding Extra Cash / Overcharging">💰 Fare Extortion / Demanding Extra Cash</option>
                    <option value="Vehicle / Driver Mismatch">🚗 Vehicle Did Not Match Registration / Wrong Driver</option>
                    <option value="Severe Route Deviation / Abandonment">📍 Severe Route Deviation / Abandoned Route</option>
                    <option value="Smoking or Substance Use">🚭 Smoking / Substance Use Inside Vehicle</option>
                    <option value="Other Safety Violation">⚠️ Other Compliance / Safety Issue</option>
                </select>
            </div>

            <div class="form-group" style="margin-bottom: 16px;">
                <label>Detailed Incident Description</label>
                <textarea id="complaint-description" rows="4" required placeholder="Please provide specific details about what occurred, time, and location..." style="padding: 10px; border: 1px solid var(--border); border-radius: 8px; background: var(--card-bg); color: var(--dark); font-size: 13px;"></textarea>
            </div>

            <div style="background: rgba(239,68,68,0.08); border: 1px solid rgba(239,68,68,0.25); border-radius: 8px; padding: 10px 12px; margin-bottom: 16px; font-size: 12px; color: var(--danger);">
                🛡️ <strong>Safety Alert:</strong> This grievance will be transmitted instantly to the ShareMile Administrative Incident Team. If verified, the driver will face immediate blacklisting.
            </div>

            <button type="submit" class="btn-primary" style="background: var(--danger); width: 100%; padding: 12px; font-size: 14px; font-weight: 700;" id="complaint-submit-btn">
                🚨 Dispatch Incident Report to Administration
            </button>
        </form>
    `;

    document.getElementById('modal-complaint').style.display = 'flex';
}

async function handleComplaintSubmit(e, driverId, driverName) {
    e.preventDefault();
    const reason = document.getElementById('complaint-reason').value;
    const description = document.getElementById('complaint-description').value.trim();
    const btn = document.getElementById('complaint-submit-btn');

    if (!description) {
        alert("Please enter a description of the incident.");
        return;
    }

    btn.disabled = true;
    btn.innerHTML = '<span class="btn-spinner"></span> Filing Incident...';

    try {
        const res = await fetch('/api/admin/complaints', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify({
                reportedUserId: driverId,
                reason: reason,
                description: description
            })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || "Failed to submit grievance");
        }

        const ticket = await res.json();
        closeModal('modal-complaint');

        showDispatchToast('info', '🛡️ Safety Grievance Dispatched', `Ticket #${ticket.id} filed regarding driver ${driverName}. Admins have been alerted in real time.`);
        alert(`✅ Safety Complaint #${ticket.id} Submitted Successfully!\n\nThe Platform Administrator has been alerted immediately via push alerts & WebSocket. Appropriate disciplinary measures including driver blacklisting will be reviewed.`);
    } catch (err) {
        alert("Submission Failed: " + err.message);
        btn.disabled = false;
        btn.innerHTML = '🚨 Dispatch Incident Report to Administration';
    }
}

// ═══════════════════════════════════════════════
// 🛡️ ADMIN GOVERNANCE & CONTROL CENTER
// ═══════════════════════════════════════════════
let adminAllUsers = [];
let adminAllComplaints = [];

function switchAdminSubTab(subTab) {
    ['complaints', 'users', 'analytics'].forEach(t => {
        const pane = document.getElementById(`admin-subtab-${t}`);
        const btn = document.getElementById(`admin-nav-${t}`);
        if (pane) pane.style.display = (t === subTab) ? 'block' : 'none';
        if (btn) btn.classList.toggle('active', t === subTab);
    });

    if (subTab === 'analytics') {
        loadAnalytics();
    }
}

async function loadAdminPanelData() {
    try {
        // 1. Fetch Analytics Overview
        const resAnalytics = await fetch('/api/admin/analytics', {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        if (resAnalytics.ok) {
            const analytics = await resAnalytics.json();
            document.getElementById('metric-rides').innerText = analytics.totalRides || 0;
            document.getElementById('metric-carbon').innerText = `${(analytics.totalCarbonSavedKg || 0).toFixed(1)} kg`;
            renderAnalyticsCharts(analytics);
        }

        // 2. Fetch All Complaints
        const resComplaints = await fetch('/api/admin/complaints', {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        if (resComplaints.ok) {
            adminAllComplaints = await resComplaints.json();
            renderAdminComplaints(adminAllComplaints);
        }

        // 3. Fetch All Users
        const resUsers = await fetch('/api/admin/users', {
            headers: { 'Authorization': `Bearer ${jwtToken}` }
        });
        if (resUsers.ok) {
            adminAllUsers = await resUsers.json();
            renderAdminUsers(adminAllUsers);
        }
    } catch (err) {
        console.error("Failed to load admin panel data:", err);
    }
}

function renderAdminComplaints(complaints) {
    const tbody = document.getElementById('admin-complaints-table-body');
    const badgeCount = document.getElementById('admin-complaints-badge');
    const metricCount = document.getElementById('metric-complaints');
    const pendingCount = document.getElementById('metric-pending-complaints');

    const pendingTickets = complaints.filter(c => c.status === 'PENDING');
    if (badgeCount) badgeCount.innerText = pendingTickets.length;
    if (metricCount) metricCount.innerText = complaints.length;
    if (pendingCount) pendingCount.innerText = `${pendingTickets.length} Pending Review`;

    if (!tbody) return;

    if (complaints.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--dark-muted); padding: 30px;">✅ No passenger safety complaints on record. All drivers operating in good standing.</td></tr>`;
        return;
    }

    tbody.innerHTML = complaints.map(c => {
        const isPending = c.status === 'PENDING';
        const isBlacklisted = c.status === 'DRIVER_BLACKLISTED';
        const isResolved = c.status === 'RESOLVED';

        const statusBadge = isPending
            ? `<span class="booking-status-pill status-pending"><span class="eta-pulse" style="display:inline-block;width:6px;height:6px;margin:0 4px 1px 0;"></span>PENDING</span>`
            : isBlacklisted
            ? `<span class="badge-blacklisted">⛔ DRIVER BANNED</span>`
            : `<span class="booking-status-pill status-accepted">RESOLVED</span>`;

        const driver = c.reportedUser || {};
        const reporter = c.reporter || {};
        const dateFormatted = new Date(c.createdAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' });

        return `
            <tr style="${isPending ? 'background: rgba(245,158,11,0.04);' : ''}">
                <td style="font-weight: 700; color: var(--primary);">#${c.id}</td>
                <td style="white-space: nowrap; font-size: 12px; color: var(--dark-muted);">${dateFormatted}</td>
                <td>
                    <strong>${escapeAttr(reporter.fullName || 'Passenger')}</strong><br>
                    <span style="font-size: 11px; color: var(--dark-muted);">${escapeAttr(reporter.email || '')}</span>
                </td>
                <td>
                    <strong>${escapeAttr(driver.fullName || 'Driver')}</strong><br>
                    <span style="font-size: 11.5px; color: var(--dark-muted);">${escapeAttr(driver.vehicleModel || 'Standard Vehicle')}</span>
                    ${driver.isBlacklisted ? '<br><span class="badge-blacklisted" style="margin-top:3px;">⛔ Blacklisted</span>' : ''}
                </td>
                <td>
                    <span style="font-weight: 600; color: var(--danger); font-size: 12.5px;">${escapeAttr(c.reason)}</span>
                </td>
                <td style="max-width: 250px; font-size: 12.5px; line-height: 1.4;">
                    "${escapeAttr(c.description)}"
                </td>
                <td>${statusBadge}</td>
                <td>
                    <div style="display: flex; gap: 6px; flex-wrap: wrap;">
                        ${!driver.isBlacklisted ? `
                            <button class="btn-admin-danger" onclick="adminBlacklistDriverFromComplaint(${c.id}, '${escapeAttr(driver.fullName)}')">
                                🚫 Blacklist Driver
                            </button>
                        ` : ''}
                        ${isPending ? `
                            <button class="btn-admin-resolve" onclick="adminResolveComplaint(${c.id})">
                                ✅ Resolve
                            </button>
                            <button class="btn-outline" style="font-size: 11px; padding: 4px 8px;" onclick="adminDismissComplaint(${c.id})">
                                Dismiss
                            </button>
                        ` : ''}
                        ${!isPending && driver.isBlacklisted ? `
                            <span style="font-size: 11.5px; color: var(--success); font-weight: 600;">✓ Disciplinary Action Enforced</span>
                        ` : ''}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function renderAdminUsers(users) {
    const tbody = document.getElementById('admin-users-table-body');
    const metricUsers = document.getElementById('metric-users');
    const metricDrivers = document.getElementById('metric-drivers-count');

    const totalDrivers = users.filter(u => u.role === 'ROLE_DRIVER' || (u.driverLicenseNumber && u.driverLicenseNumber.trim().length > 0));
    const blacklistedUsers = users.filter(u => u.isBlacklisted || u.blacklisted);

    if (metricUsers) metricUsers.innerText = users.length;
    if (metricDrivers) metricDrivers.innerText = `${totalDrivers.length} Drivers • ${blacklistedUsers.length} Blacklisted`;

    if (!tbody) return;

    filterAdminUsersTable();
}

function filterAdminUsersTable() {
    const tbody = document.getElementById('admin-users-table-body');
    if (!tbody) return;

    const query = (document.getElementById('admin-user-search')?.value || '').toLowerCase().trim();
    const roleFilter = document.getElementById('admin-user-role-filter')?.value || 'ALL';

    const filtered = adminAllUsers.filter(u => {
        const isDriver = u.role === 'ROLE_DRIVER' || (u.driverLicenseNumber && u.driverLicenseNumber.trim().length > 0);
        const isBlacklisted = u.isBlacklisted || u.blacklisted;

        if (roleFilter === 'ROLE_DRIVER' && !isDriver) return false;
        if (roleFilter === 'ROLE_PASSENGER' && isDriver) return false;
        if (roleFilter === 'BLACKLISTED' && !isBlacklisted) return false;

        if (query) {
            const matchName = (u.fullName || '').toLowerCase().includes(query);
            const matchEmail = (u.email || '').toLowerCase().includes(query);
            const matchVehicle = (u.vehicleModel || '').toLowerCase().includes(query);
            const matchLicense = (u.driverLicenseNumber || '').toLowerCase().includes(query);
            return matchName || matchEmail || matchVehicle || matchLicense;
        }
        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--dark-muted); padding: 24px;">No users match your filter criteria.</td></tr>`;
        return;
    }

    tbody.innerHTML = filtered.map(u => {
        const isDriver = u.role === 'ROLE_DRIVER' || (u.driverLicenseNumber && u.driverLicenseNumber.trim().length > 0);
        const isBlacklisted = Boolean(u.isBlacklisted || u.blacklisted);
        const isVerified = Boolean(u.verified || u.isVerified);

        const ratingLabel = (u.totalRatings > 0)
            ? `⭐ ${u.averageRating} (${u.totalRatings} trips)`
            : `<span class="new-driver-badge">🔰 New User (0 trips)</span>`;

        return `
            <tr style="${isBlacklisted ? 'background: rgba(239,68,68,0.04);' : ''}">
                <td>
                    <div style="display:flex; align-items:center; gap:8px;">
                        <span style="width:28px; height:28px; border-radius:50%; background:${isDriver ? '#0ea5e9' : '#2563eb'}; color:white; display:flex; align-items:center; justify-content:center; font-weight:bold; font-size:12px;">
                            ${(u.fullName || '?').charAt(0).toUpperCase()}
                        </span>
                        <div>
                            <strong>${escapeAttr(u.fullName)}</strong><br>
                            <span style="font-size: 11px; color: var(--dark-muted);">@${escapeAttr(u.username)}</span>
                        </div>
                    </div>
                </td>
                <td>
                    <span class="seats-pill">${isDriver ? '🚖 Driver Partner' : '👤 Passenger'}</span><br>
                    <span style="font-size: 11px; color: var(--dark-muted);">${u.gender || 'Not specified'}</span>
                </td>
                <td>
                    <span style="font-size: 12.5px;">${escapeAttr(u.email)}</span><br>
                    <span style="font-size: 11.5px; color: var(--dark-muted);">📞 ${escapeAttr(u.phone || 'N/A')}</span>
                </td>
                <td>
                    ${isDriver ? `
                        <strong>${escapeAttr(u.vehicleModel || 'Standard Vehicle')}</strong><br>
                        <span style="font-size: 11.5px; color: var(--dark-muted);">No: ${escapeAttr(u.vehicleNumber || 'Pending')} • DL: ${escapeAttr(u.driverLicenseNumber || 'N/A')}</span>
                    ` : '<span style="color: var(--dark-muted); font-size: 12px;">Commuter Profile</span>'}
                </td>
                <td>${ratingLabel}</td>
                <td>
                    <span class="${isVerified ? 'badge-verified' : 'badge-unverified'}">
                        ${isVerified ? '✓ Verified' : 'Pending'}
                    </span>
                    <button class="btn-outline" style="font-size: 10.5px; padding: 2px 6px; margin-left: 4px;" onclick="adminToggleVerifyUser(${u.id}, ${!isVerified})">
                        ${isVerified ? 'Unverify' : 'Verify'}
                    </button>
                </td>
                <td>
                    ${isBlacklisted 
                        ? `<span class="badge-blacklisted">⛔ BLACKLISTED</span>` 
                        : `<span class="badge-active">🟢 Active</span>`}
                </td>
                <td>
                    ${isBlacklisted ? `
                        <button class="btn-admin-restore" onclick="adminToggleBlacklistUser(${u.id}, false, '${escapeAttr(u.fullName)}')">
                            🔄 Restore &amp; Unban
                        </button>
                    ` : `
                        <button class="btn-admin-danger" onclick="adminToggleBlacklistUser(${u.id}, true, '${escapeAttr(u.fullName)}')">
                            🚫 Blacklist User
                        </button>
                    `}
                </td>
            </tr>
        `;
    }).join('');
}

async function adminToggleBlacklistUser(userId, blacklist, userName) {
    const actionText = blacklist ? 'BLACKLIST and SUSPEND' : 'RESTORE and UNBAN';
    if (!confirm(`Are you sure you want to ${actionText} "${userName}"?`)) return;

    try {
        const res = await fetch(`/api/admin/users/${userId}/blacklist`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify({ blacklist: blacklist })
        });

        if (!res.ok) throw new Error("Could not update user blacklist status");

        showDispatchToast(blacklist ? 'danger' : 'info', '🛡️ Administrative Security Action', `${userName} has been ${blacklist ? 'blacklisted' : 'restored'}.`);
        loadAdminPanelData();
    } catch (err) {
        alert("Operation Failed: " + err.message);
    }
}

async function adminToggleVerifyUser(userId, verify) {
    try {
        const res = await fetch(`/api/admin/users/${userId}/verify`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify({ verified: verify })
        });
        if (!res.ok) throw new Error("Could not update verification");
        loadAdminPanelData();
    } catch (err) {
        alert("Verification update failed: " + err.message);
    }
}

async function adminBlacklistDriverFromComplaint(complaintId, driverName) {
    if (!confirm(`🚨 IMMEDIATE SANCTION:\nAre you sure you want to BLACKLIST driver "${driverName}" based on passenger complaint #${complaintId}?\n\nThis will immediately cancel all active rides and suspend driver privileges platform-wide.`)) return;

    try {
        const res = await fetch(`/api/admin/complaints/${complaintId}/blacklist-driver`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwtToken}`
            }
        });
        if (!res.ok) throw new Error("Failed to blacklist driver");

        showDispatchToast('danger', '⛔ Driver Blacklisted', `Driver ${driverName} permanently banned from the platform.`);
        alert(`✅ Driver ${driverName} has been BLACKLISTED!\n\nAll open rides cancelled and disciplinary actions recorded on grievance ticket #${complaintId}.`);
        loadAdminPanelData();
    } catch (err) {
        alert("Action failed: " + err.message);
    }
}

async function adminResolveComplaint(complaintId) {
    try {
        const res = await fetch(`/api/admin/complaints/${complaintId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify({ status: 'RESOLVED' })
        });
        if (!res.ok) throw new Error("Could not resolve ticket");
        showDispatchToast('info', '✅ Complaint Resolved', `Ticket #${complaintId} marked as resolved.`);
        loadAdminPanelData();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function adminDismissComplaint(complaintId) {
    try {
        const res = await fetch(`/api/admin/complaints/${complaintId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${jwtToken}`
            },
            body: JSON.stringify({ status: 'DISMISSED' })
        });
        if (!res.ok) throw new Error("Could not dismiss ticket");
        loadAdminPanelData();
    } catch (err) {
        alert("Error: " + err.message);
    }
}
