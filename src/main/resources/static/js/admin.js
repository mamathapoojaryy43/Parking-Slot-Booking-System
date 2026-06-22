// Admin Control Center JS Logic

let currentUser = null;
let revenueChartInstance = null;
let predictionChartInstance = null;

document.addEventListener('DOMContentLoaded', () => {
    initAdmin();

    // Event listeners
    document.getElementById('pricing-config-form').addEventListener('submit', handlePricingSubmit);
    document.getElementById('add-floor-form').addEventListener('submit', handleAddFloorSubmit);
    document.getElementById('add-slot-form').addEventListener('submit', handleAddSlotSubmit);
});

async function initAdmin() {
    await verifyAdminLogin();
    if (!currentUser) return;

    await loadKPIs();
    await loadPricingConfig();
    await loadAdminFloors();
    await loadUsersList();
    await renderCharts();
}

async function verifyAdminLogin() {
    try {
        const res = await fetch('/api/auth/login-status');
        if (!res.ok) {
            window.location.href = '/login.html';
            return;
        }
        const data = await res.json();
        if (!data || !data.isAdmin) {
            alert("Access Denied: Admin role required.");
            window.location.href = '/dashboard.html';
            return;
        }
        currentUser = data;
        document.getElementById('admin-user-name').textContent = currentUser.fullName || currentUser.username;
    } catch (e) {
        console.error('Error verifying admin login:', e);
        window.location.href = '/login.html';
    }
}

async function loadKPIs() {
    try {
        const res = await fetch('/api/admin/stats');
        if (res.ok) {
            const stats = await res.json();
            document.getElementById('kpi-revenue').textContent = formatCurrency(stats.totalRevenue);
            document.getElementById('kpi-occupancy').textContent = stats.averageOccupancy + '%';
            document.getElementById('kpi-bookings').textContent = stats.todayBookings;
            document.getElementById('kpi-co2').textContent = stats.co2SavedTotal + 'g';
        }
    } catch (e) {
        console.error('Error loading KPIs:', e);
    }
}

async function loadPricingConfig() {
    try {
        const res = await fetch('/api/admin/pricing');
        if (res.ok) {
            const config = await res.json();
            document.getElementById('pricing-enabled').checked = config.enabled;
            document.getElementById('peak-multiplier').value = config.peakMultiplier;
            document.getElementById('medium-multiplier').value = config.mediumMultiplier;
            document.getElementById('discount-multiplier').value = config.discountMultiplier;
        }
    } catch (e) {
        console.error(e);
    }
}

async function handlePricingSubmit(e) {
    e.preventDefault();
    const enabled = document.getElementById('pricing-enabled').checked;
    const peakMultiplier = document.getElementById('peak-multiplier').value;
    const mediumMultiplier = document.getElementById('medium-multiplier').value;
    const discountMultiplier = document.getElementById('discount-multiplier').value;

    try {
        const res = await fetch('/api/admin/pricing', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ enabled, peakMultiplier, mediumMultiplier, discountMultiplier })
        });
        if (res.ok) {
            alert("Dynamic Pricing Engine configurations updated successfully!");
            await loadPricingConfig();
            await loadKPIs();
        } else {
            const data = await res.json();
            alert(data.error || "Failed to update pricing engine configs.");
        }
    } catch (err) {
        console.error(err);
    }
}

async function loadAdminFloors() {
    try {
        const res = await fetch('/api/slots/floors');
        if (res.ok) {
            const list = await res.json();
            const select = document.getElementById('new-slot-floor');
            select.innerHTML = '';
            list.forEach(f => {
                select.insertAdjacentHTML('beforeend', `<option value="${f.id}">${f.floorName} (Cap: ${f.capacity})</option>`);
            });
        }
    } catch (e) {
        console.error(e);
    }
}

async function handleAddFloorSubmit(e) {
    e.preventDefault();
    const floorNumber = document.getElementById('new-floor-num').value;
    const floorName = document.getElementById('new-floor-name').value;
    const capacity = document.getElementById('new-floor-cap').value;

    try {
        const res = await fetch('/api/admin/floors', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ floorNumber, floorName, capacity })
        });
        if (res.ok) {
            alert("Parking Floor created successfully.");
            document.getElementById('add-floor-form').reset();
            await loadAdminFloors();
        } else {
            const data = await res.json();
            alert(data.error);
        }
    } catch (err) {
        console.error(err);
    }
}

async function handleAddSlotSubmit(e) {
    e.preventDefault();
    const slotNumber = document.getElementById('new-slot-num').value;
    const type = document.getElementById('new-slot-type').value;
    const floorId = document.getElementById('new-slot-floor').value;

    try {
        const res = await fetch('/api/admin/slots', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ slotNumber, type, floorId })
        });
        if (res.ok) {
            alert("Parking Slot created successfully.");
            document.getElementById('add-slot-form').reset();
            await loadKPIs();
        } else {
            const data = await res.json();
            alert(data.error);
        }
    } catch (err) {
        console.error(err);
    }
}

async function loadUsersList() {
    try {
        const res = await fetch('/api/admin/users');
        if (res.ok) {
            const list = await res.json();
            const tbody = document.getElementById('admin-users-rows');
            tbody.innerHTML = '';

            list.forEach(u => {
                const rolesStr = u.roles.map(r => r.name.replace('ROLE_', '')).join(', ');
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td><strong>${u.id}</strong></td>
                    <td>${u.username}</td>
                    <td>${u.email}</td>
                    <td>${u.fullName || 'N/A'}</td>
                    <td>${u.phone || 'N/A'}</td>
                    <td><span class="badge ${rolesStr.includes('ADMIN') ? 'bg-danger' : 'bg-primary'}">${rolesStr}</span></td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (e) {
        console.error(e);
    }
}

async function renderCharts() {
    try {
        // 1. Load historical reports for Revenue Chart
        const reportsRes = await fetch('/api/admin/reports');
        if (reportsRes.ok) {
            const reports = await reportsRes.json();
            
            // Sort reports chronologically
            reports.sort((a,b) => new Date(a.date) - new Date(b.date));

            const labels = reports.map(r => r.date);
            const revenues = reports.map(r => r.totalRevenue);
            const bookings = reports.map(r => r.totalBookings);

            const ctx1 = document.getElementById('revenueChart').getContext('2d');
            if (revenueChartInstance) revenueChartInstance.destroy();

            revenueChartInstance = new Chart(ctx1, {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: 'Revenue ($)',
                            data: revenues,
                            borderColor: '#3b82f6',
                            backgroundColor: 'rgba(59, 130, 246, 0.1)',
                            borderWidth: 3,
                            fill: true,
                            yAxisID: 'y'
                        },
                        {
                            label: 'Bookings Count',
                            data: bookings,
                            borderColor: '#10b981',
                            backgroundColor: 'rgba(16, 185, 129, 0.1)',
                            borderWidth: 2,
                            type: 'bar',
                            yAxisID: 'y1'
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        y: {
                            type: 'linear',
                            display: true,
                            position: 'left',
                            title: { display: true, text: 'Revenue ($)' }
                        },
                        y1: {
                            type: 'linear',
                            display: true,
                            position: 'right',
                            grid: { drawOnChartArea: false }, // Only keep grid lines for one axis
                            title: { display: true, text: 'Bookings' }
                        }
                    }
                }
            });
        }

        // 2. Load Peak Hour Predictions Chart
        const predRes = await fetch('/api/admin/prediction');
        if (predRes.ok) {
            const predictions = await predRes.json();
            
            const hours = Object.keys(predictions).map(h => h + ':00');
            const occupancies = Object.values(predictions);

            const ctx2 = document.getElementById('predictionChart').getContext('2d');
            if (predictionChartInstance) predictionChartInstance.destroy();

            predictionChartInstance = new Chart(ctx2, {
                type: 'bar',
                data: {
                    labels: hours,
                    datasets: [{
                        label: 'Predicted Occupancy (%)',
                        data: occupancies,
                        backgroundColor: occupancies.map(occ => {
                            if (occ >= 80) return 'rgba(239, 68, 68, 0.75)'; // Red for high
                            if (occ >= 60) return 'rgba(245, 158, 11, 0.75)'; // Yellow for medium
                            return 'rgba(16, 185, 129, 0.75)'; // Green for low
                        }),
                        borderColor: occupancies.map(occ => {
                            if (occ >= 80) return '#ef4444';
                            if (occ >= 60) return '#f59e0b';
                            return '#10b981';
                        }),
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    scales: {
                        y: {
                            min: 0,
                            max: 100,
                            title: { display: true, text: 'Occupancy Rate (%)' }
                        }
                    }
                }
            });
        }
    } catch (e) {
        console.error('Error rendering admin charts:', e);
    }
}
