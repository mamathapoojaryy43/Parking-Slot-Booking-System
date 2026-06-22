// User Dashboard JS Logic

let currentUser = null;
let registeredVehicles = [];
let floors = [];
let selectedFloorId = null;
let selectedSlotId = null;
let selectedSlotDetails = null;
let activeBooking = null;

// Bootstrap modals
let qrModal = null;
let paymentModal = null;

document.addEventListener('DOMContentLoaded', () => {
    qrModal = new bootstrap.Modal(document.getElementById('qrModal'));
    paymentModal = new bootstrap.Modal(document.getElementById('paymentModal'));

    // Initialize Page
    initDashboard();

    // Event listeners
    document.getElementById('btn-get-recommendation').addEventListener('click', handleGetRecommendation);
    document.getElementById('btn-reserve-rec').addEventListener('click', handleReserveRecommendation);
    document.getElementById('btn-reserve-selected').addEventListener('click', handleReserveSelected);
    document.getElementById('vehicle-reg-form').addEventListener('submit', handleVehicleRegistration);
    
    document.getElementById('btn-simulate-entry').addEventListener('click', handleSimulateEntry);
    document.getElementById('btn-simulate-exit').addEventListener('click', handleShowPaymentModal);
    document.getElementById('btn-show-qr').addEventListener('click', handleShowQRModal);
    
    document.getElementById('checkout-payment-form').addEventListener('submit', handleProcessPayment);
});

async function initDashboard() {
    await verifyUserLogin();
    if (!currentUser) return;

    await loadLiveStats();
    await loadUserVehicles();
    await loadFloors();
    await loadNotifications();
    await loadBookingsHistory();
    await checkActiveBookings();
}

async function verifyUserLogin() {
    try {
        const res = await fetch('/api/auth/login-status');
        if (!res.ok) {
            window.location.href = '/login.html';
            return;
        }
        const data = await res.json();
        if (!data) {
            window.location.href = '/login.html';
            return;
        }
        currentUser = data;
        document.getElementById('dashboard-user-name').textContent = currentUser.fullName || currentUser.username;
    } catch (e) {
        console.error('Error verifying login:', e);
        window.location.href = '/login.html';
    }
}

async function loadLiveStats() {
    try {
        const res = await fetch('/api/slots/stats');
        if (res.ok) {
            const stats = await res.json();
            document.getElementById('stat-slots-available').textContent = stats.available;
            document.getElementById('stat-slots-occupied').textContent = stats.occupied;
            document.getElementById('stat-slots-reserved').textContent = stats.reserved;
        }
    } catch (e) {
        console.error('Error loading live stats:', e);
    }
}

async function loadUserVehicles() {
    try {
        const res = await fetch('/api/vehicles');
        if (res.ok) {
            registeredVehicles = await res.json();
            
            // Populate lists and selects
            const listContainer = document.getElementById('user-vehicles-list');
            const aiSelect = document.getElementById('ai-vehicle-select');
            const selSelect = document.getElementById('sel-vehicle-select');

            listContainer.innerHTML = '';
            aiSelect.innerHTML = '<option value="">-- Choose registered vehicle --</option>';
            selSelect.innerHTML = '';

            if (registeredVehicles.length === 0) {
                listContainer.innerHTML = '<div class="text-secondary small p-2">No registered vehicles yet. Add one below!</div>';
                return;
            }

            registeredVehicles.forEach(v => {
                // List item
                const item = document.createElement('div');
                item.className = 'list-group-item d-flex justify-content-between align-items-center bg-transparent border-0 px-0 py-2';
                
                let typeIcon = 'fa-car';
                if (v.type === 'EV') typeIcon = 'fa-bolt text-info';
                else if (v.type === 'TWO_WHEELER') typeIcon = 'fa-motorcycle';

                item.innerHTML = `
                    <div class="d-flex align-items-center">
                        <i class="fa-solid ${typeIcon} fs-5 me-3 text-secondary"></i>
                        <div>
                            <strong class="text-primary">${v.licensePlate}</strong>
                            <div class="small text-secondary">${v.model || 'Unknown Model'}</div>
                        </div>
                    </div>
                    <button class="btn btn-sm btn-outline-danger border-0" onclick="deleteVehicle(${v.id})"><i class="fas fa-trash"></i></button>
                `;
                listContainer.appendChild(item);

                // Option item
                const optStr = `<option value="${v.id}">${v.licensePlate} (${v.type})</option>`;
                aiSelect.insertAdjacentHTML('beforeend', optStr);
                selSelect.insertAdjacentHTML('beforeend', optStr);
            });
        }
    } catch (e) {
        console.error('Error loading vehicles:', e);
    }
}

async function loadFloors() {
    try {
        const res = await fetch('/api/slots/floors');
        if (res.ok) {
            floors = await res.json();
            const tabContainer = document.getElementById('floor-tab-pills');
            tabContainer.innerHTML = '';

            if (floors.length === 0) return;

            floors.forEach((f, index) => {
                const isActive = index === 0;
                if (isActive) selectedFloorId = f.id;

                const li = document.createElement('li');
                li.className = 'nav-item';
                li.innerHTML = `
                    <button class="nav-link fw-bold px-4 py-2 ${isActive ? 'active' : ''}" 
                            data-floor-id="${f.id}" 
                            onclick="selectFloor(${f.id}, this)">
                        ${f.floorName}
                    </button>
                `;
                tabContainer.appendChild(li);
            });

            // Load slots for the first floor
            if (selectedFloorId) {
                loadFloorSlots(selectedFloorId);
            }
        }
    } catch (e) {
        console.error('Error loading floors:', e);
    }
}

function selectFloor(floorId, button) {
    // Toggle active class on buttons
    document.querySelectorAll('#floor-tab-pills button').forEach(btn => btn.classList.remove('active'));
    button.classList.add('active');

    selectedFloorId = floorId;
    loadFloorSlots(floorId);
    
    // Hide reservation action panel when switching floors
    document.getElementById('selected-slot-action-panel').style.display = 'none';
}

async function loadFloorSlots(floorId) {
    try {
        const res = await fetch(`/api/slots/floors/${floorId}/slots`);
        if (res.ok) {
            const slots = await res.json();
            const grid = document.getElementById('slots-map-grid');
            grid.innerHTML = '';

            slots.forEach(slot => {
                const btn = document.createElement('div');
                
                // Color codes
                let statusClass = 'available';
                let iconStr = '<i class="fa-regular fa-square-check"></i>';
                
                if (slot.status === 'OCCUPIED') {
                    statusClass = 'occupied';
                    iconStr = '<i class="fa-solid fa-car-side"></i>';
                } else if (slot.status === 'RESERVED') {
                    statusClass = 'reserved';
                    iconStr = '<i class="fa-solid fa-clock"></i>';
                } else {
                    // Available, check type for styling
                    if (slot.type === 'EV') {
                        statusClass = 'ev';
                        iconStr = '<i class="fa-solid fa-bolt"></i>';
                    } else if (slot.type === 'DISABLED') {
                        statusClass = 'disabled-slot';
                        iconStr = '<i class="fa-solid fa-wheelchair"></i>';
                    }
                }

                btn.className = `parking-slot ${statusClass}`;
                btn.id = `slot-${slot.id}`;
                btn.innerHTML = `
                    ${iconStr}
                    ${slot.slotNumber}
                    <span>${slot.type.replace('_', ' ')}</span>
                `;

                if (slot.status === 'AVAILABLE') {
                    btn.addEventListener('click', () => selectSlot(slot));
                }

                grid.appendChild(btn);
            });
        }
    } catch (e) {
        console.error('Error loading slots:', e);
    }
}

function selectSlot(slot) {
    selectedSlotId = slot.id;
    selectedSlotDetails = slot;

    // Remove selected border from all slots and add to selected
    document.querySelectorAll('.parking-slot').forEach(el => el.classList.remove('selected'));
    document.getElementById(`slot-${slot.id}`).classList.add('selected');

    // Show action panel
    document.getElementById('sel-slot-number').textContent = slot.slotNumber;
    document.getElementById('sel-slot-type').textContent = slot.type.replace('_', ' ');
    
    let rate = "₹10.00/hr";
    if (slot.type === 'FOUR_WHEELER') rate = "₹20.00/hr";
    else if (slot.type === 'EV') rate = "₹25.00/hr";
    else if (slot.type === 'DISABLED') rate = "₹15.00/hr";
    
    document.getElementById('sel-slot-rate').textContent = rate;
    document.getElementById('selected-slot-action-panel').style.display = 'block';
}

async function loadNotifications() {
    try {
        const res = await fetch(`/api/notifications`);
        // Note: notifications API not declared or is mapped to get user logs. Let's create user notification mock logic or query if controller exist.
        // Wait, does notifications controller exist? We didn't define one!
        // But we have NotificationRepository and BookingService creates them.
        // Let's create the notification endpoint! Oh wait, let's write it in AuthController or add it to UserController/BookingController.
        // Actually, we can retrieve notifications via API. Let's make sure there is a notification controller endpoint or mock it.
        // Wait! We can fetch them. Let's make sure the backend endpoint exists. Oh, we didn't write a NotificationController!
        // Let's check: did we define NotificationController in task.md? No, we didn't!
        // But we can check if we should add it. Wait, we can fetch notifications in user login payload, or add a quick endpoint in UserController or AuthController, or let the user fetch them from a mapping.
        // Wait, let's fetch notifications from `/api/auth/notifications` or let's create a custom endpoint in `BookingController` or a new `NotificationController.java`.
        // Let's create a simple mapping in `AuthController` or define a dedicated controller. We'll add `/api/notifications` in AuthController or BookingController, or create a quick `NotificationController` file!
        // Let's look: creating a new `NotificationController.java` is incredibly clean and standard. We will write it next. For now in JS, we fetch from `/api/notifications`.
        const response = await fetch('/api/notifications');
        if (response.ok) {
            const list = await response.json();
            const container = document.getElementById('notifications-list');
            container.innerHTML = '';
            if (list.length === 0) {
                container.innerHTML = '<div class="text-secondary small p-3">No new notifications.</div>';
                return;
            }
            list.forEach(n => {
                const div = document.createElement('div');
                div.className = 'p-3 border-bottom small';
                div.innerHTML = `
                    <div class="d-flex justify-content-between mb-1">
                        <span class="fw-bold">${n.readStatus ? '' : '<span class="badge-unread"></span>'}${n.message}</span>
                    </div>
                    <div class="text-muted small">${formatDate(n.createdAt)}</div>
                `;
                container.appendChild(div);
            });
        }
    } catch (e) {
        console.error('Error loading notifications:', e);
    }
}

async function loadBookingsHistory() {
    try {
        const res = await fetch('/api/bookings/history');
        if (res.ok) {
            const list = await res.json();
            const tbody = document.getElementById('bookings-history-rows');
            tbody.innerHTML = '';

            // Update Total CO2 saved based on history
            let totalCo2 = 0;

            if (list.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center text-secondary small py-4">No bookings found.</td></tr>';
                return;
            }

            list.forEach(b => {
                const duration = b.endTime ? calculateDuration(b.startTime, b.endTime) : 'Active now';
                const paid = b.totalAmount ? formatCurrency(b.totalAmount) : 'Pending exit';
                
                // Status badge
                let statusBadge = `<span class="badge bg-success">${b.status}</span>`;
                if (b.status === 'RESERVED') statusBadge = `<span class="badge bg-warning text-dark">${b.status}</span>`;
                else if (b.status === 'ACTIVE') statusBadge = `<span class="badge bg-info">${b.status}</span>`;
                else if (b.status === 'CANCELLED') statusBadge = `<span class="badge bg-danger">${b.status}</span>`;

                // Calculate total CO2 saved in UI
                if (b.status === 'COMPLETED') {
                    let co2 = 900;
                    if (b.slot.type === 'EV') {
                        // EV offset: standard additional 1500g per hour
                        const durationHrs = Math.max(1, Math.ceil(DurationHours(b.startTime, b.endTime)));
                        co2 += durationHrs * 1500;
                    }
                    totalCo2 += co2;
                }

                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td><strong>${b.bookingNumber}</strong></td>
                    <td>${b.slot.slotNumber} <span class="badge bg-secondary ms-1">${b.slot.floor.floorName}</span></td>
                    <td>${b.vehicle.licensePlate}</td>
                    <td>${duration}</td>
                    <td>${paid}</td>
                    <td>${statusBadge}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="showBookingQR('${b.bookingNumber}', '${b.slot.slotNumber}', '${b.vehicle.licensePlate}', '${b.qrCodeData}')"><i class="fas fa-qrcode"></i></button>
                    </td>
                `;
                tbody.appendChild(tr);
            });

            document.getElementById('stat-co2-saved').textContent = totalCo2 + 'g';
        }
    } catch (e) {
        console.error('Error loading history:', e);
    }
}

function calculateDuration(start, end) {
    const minutes = Math.floor(Math.abs(new Date(end) - new Date(start)) / 1000 / 60);
    if (minutes < 60) return minutes + ' mins';
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hours}h ${mins}m`;
}

function DurationHours(start, end) {
    return Math.abs(new Date(end) - new Date(start)) / 1000 / 60 / 60;
}

async function checkActiveBookings() {
    try {
        const res = await fetch('/api/bookings/history');
        if (res.ok) {
            const list = await res.json();
            
            // Find any booking that is RESERVED or ACTIVE
            activeBooking = list.find(b => b.status === 'RESERVED' || b.status === 'ACTIVE');

            const banner = document.getElementById('active-booking-banner');
            
            if (activeBooking) {
                document.getElementById('banner-booking-number').textContent = activeBooking.bookingNumber;
                document.getElementById('banner-slot-number').textContent = activeBooking.slot.slotNumber;
                document.getElementById('banner-vehicle-plate').textContent = activeBooking.vehicle.licensePlate;
                
                const statusBadge = document.getElementById('banner-status');
                statusBadge.textContent = activeBooking.status;
                statusBadge.className = activeBooking.status === 'RESERVED' ? 'badge bg-warning text-dark' : 'badge bg-success';

                // Display appropriate simulated gate button
                const btnEntry = document.getElementById('btn-simulate-entry');
                const btnExit = document.getElementById('btn-simulate-exit');

                if (activeBooking.status === 'RESERVED') {
                    btnEntry.style.display = 'inline-block';
                    btnExit.style.display = 'none';
                } else {
                    btnEntry.style.display = 'none';
                    btnExit.style.display = 'inline-block';
                }

                banner.style.display = 'block';
            } else {
                banner.style.display = 'none';
            }
        }
    } catch (e) {
        console.error('Error checking active bookings:', e);
    }
}

async function handleVehicleRegistration(e) {
    e.preventDefault();
    const licensePlate = document.getElementById('reg-plate').value;
    const type = document.getElementById('reg-type').value;
    const model = document.getElementById('reg-model').value;

    try {
        const res = await fetch('/api/vehicles', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ licensePlate, type, model })
        });
        if (res.ok) {
            document.getElementById('vehicle-reg-form').reset();
            await loadUserVehicles();
        } else {
            const data = await res.json();
            alert(data.error || "Failed to add vehicle.");
        }
    } catch (err) {
        console.error(err);
    }
}

async function deleteVehicle(id) {
    if (!confirm("Are you sure you want to unregister this vehicle?")) return;
    try {
        const res = await fetch(`/api/vehicles/${id}`, { method: 'DELETE' });
        if (res.ok) {
            await loadUserVehicles();
        }
    } catch (err) {
        console.error(err);
    }
}

async function handleGetRecommendation() {
    const vehicleId = document.getElementById('ai-vehicle-select').value;
    const requiresDisabled = document.getElementById('ai-disabled-switch').checked;

    if (!vehicleId) {
        alert("Please select a registered vehicle to match your parking profile!");
        return;
    }

    try {
        const res = await fetch(`/api/slots/recommend?vehicleId=${vehicleId}&requiresDisabled=${requiresDisabled}`);
        const resultDiv = document.getElementById('recommendation-result');

        if (res.ok) {
            const slot = await res.json();
            
            let rate = "₹10.00/hr";
            if (slot.type === 'FOUR_WHEELER') rate = "₹20.00/hr";
            else if (slot.type === 'EV') rate = "₹25.00/hr";
            else if (slot.type === 'DISABLED') rate = "₹15.00/hr";

            document.getElementById('rec-slot-number').textContent = slot.slotNumber;
            document.getElementById('rec-floor-name').textContent = slot.floor.floorName;
            document.getElementById('rec-slot-type').textContent = slot.type.replace('_', ' ');
            document.getElementById('rec-rate').textContent = rate;

            // Store recommended slot ID
            resultDiv.setAttribute('data-rec-slot-id', slot.id);
            resultDiv.style.display = 'block';
        } else {
            const data = await res.json();
            alert(data.error || "No suitable slots available.");
            resultDiv.style.display = 'none';
        }
    } catch (err) {
        console.error(err);
    }
}

async function handleReserveRecommendation() {
    const resultDiv = document.getElementById('recommendation-result');
    const slotId = resultDiv.getAttribute('data-rec-slot-id');
    const vehicleId = document.getElementById('ai-vehicle-select').value;

    if (!slotId || !vehicleId) return;

    await makeReservation(slotId, vehicleId);
    resultDiv.style.display = 'none';
}

async function handleReserveSelected() {
    const vehicleId = document.getElementById('sel-vehicle-select').value;
    if (!selectedSlotId) return;
    if (!vehicleId) {
        alert("Please select a registered vehicle to allocate the selected spot!");
        return;
    }

    await makeReservation(selectedSlotId, vehicleId);
    document.getElementById('selected-slot-action-panel').style.display = 'none';
}

async function makeReservation(slotId, vehicleId) {
    try {
        // Set reservation start time to now
        const startTime = new Date().toISOString().substring(0, 19);
        const res = await fetch('/api/bookings/reserve', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ slotId, vehicleId, startTime })
        });

        if (res.ok) {
            const booking = await res.json();
            alert(`Slot successfully reserved! Your booking number is: ${booking.bookingNumber}`);
            
            // Reload layout state
            await loadLiveStats();
            await loadFloorSlots(selectedFloorId);
            await loadBookingsHistory();
            await checkActiveBookings();
            await loadNotifications();
        } else {
            const data = await res.json();
            alert(data.error || "Booking failed.");
        }
    } catch (err) {
        console.error(err);
    }
}

async function handleSimulateEntry() {
    if (!activeBooking) return;
    try {
        const res = await fetch('/api/bookings/entry', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ bookingNumber: activeBooking.bookingNumber })
        });
        if (res.ok) {
            alert("Barrier check-in simulation successful! Gate opened, timer started.");
            await loadLiveStats();
            await loadFloorSlots(selectedFloorId);
            await loadBookingsHistory();
            await checkActiveBookings();
            await loadNotifications();
        } else {
            const data = await res.json();
            alert(data.error);
        }
    } catch (err) {
        console.error(err);
    }
}

function handleShowPaymentModal() {
    if (!activeBooking) return;
    
    // Calculate dynamic cost estimates before showing the checkout modal
    const start = new Date(activeBooking.startTime);
    const now = new Date();
    const minutes = Math.floor(Math.abs(now - start) / 1000 / 60);
    const hours = Math.max(1.0, Math.ceil(minutes / 60.0));
    
    let hourlyRate = 20.0;
    if (activeBooking.slot.type === 'TWO_WHEELER') hourlyRate = 10.0;
    else if (activeBooking.slot.type === 'EV') hourlyRate = 25.0;
    else if (activeBooking.slot.type === 'DISABLED') hourlyRate = 15.0;

    let total = hours * hourlyRate;
    
    // Add charger costs if EV
    if (activeBooking.slot.type === 'EV') {
        total += minutes * 0.15; // Charging rate from initializer
    }

    document.getElementById('checkout-amount').textContent = formatCurrency(total);
    document.getElementById('checkout-booking-number').textContent = activeBooking.bookingNumber;
    
    paymentModal.show();
}

async function handleProcessPayment(e) {
    e.preventDefault();
    if (!activeBooking) return;

    const paymentMethod = document.querySelector('input[name="pay-method"]:checked').value;

    try {
        // First simulate checkout checkout
        const res = await fetch('/api/bookings/exit', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ bookingNumber: activeBooking.bookingNumber, paymentMethod })
        });

        if (res.ok) {
            paymentModal.hide();
            alert("Payment processed and gate barrier checkout completed successfully! Thank you for parking with AeroPark.");
            
            await loadLiveStats();
            await loadFloorSlots(selectedFloorId);
            await loadBookingsHistory();
            await checkActiveBookings();
            await loadNotifications();
        } else {
            const data = await res.json();
            alert(data.error || "Exit check-out failed.");
        }
    } catch (err) {
        console.error(err);
    }
}

function showBookingQR(bookingNumber, slotNumber, licensePlate, qrCodeData) {
    document.getElementById('modal-booking-id').textContent = bookingNumber;
    document.getElementById('modal-slot-number').textContent = slotNumber;
    document.getElementById('modal-vehicle-plate').textContent = licensePlate;
    document.getElementById('modal-qr-code-text').textContent = qrCodeData || bookingNumber;
    qrModal.show();
}

function handleShowQRModal() {
    if (!activeBooking) return;
    showBookingQR(
        activeBooking.bookingNumber, 
        activeBooking.slot.slotNumber, 
        activeBooking.vehicle.licensePlate, 
        activeBooking.qrCodeData
    );
}
