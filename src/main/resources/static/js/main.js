// Global JS for Smart Parking Management System

document.addEventListener('DOMContentLoaded', () => {
    // Initialize Theme
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);
    updateThemeToggleButtonIcon(savedTheme);

    const themeToggleBtn = document.getElementById('theme-toggle-btn');
    if (themeToggleBtn) {
        themeToggleBtn.addEventListener('click', () => {
            const currentTheme = document.documentElement.getAttribute('data-theme');
            const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
            document.documentElement.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
            updateThemeToggleButtonIcon(newTheme);
        });
    }

    // Check Login status for navbars
    checkGlobalLoginStatus();
});

function updateThemeToggleButtonIcon(theme) {
    const icon = document.querySelector('#theme-toggle-btn i');
    if (icon) {
        if (theme === 'dark') {
            icon.className = 'fas fa-sun';
        } else {
            icon.className = 'fas fa-moon';
        }
    }
}

async function checkGlobalLoginStatus() {
    try {
        const res = await fetch('/api/auth/login-status');
        if (!res.ok) return;
        const user = await res.json();
        
        const userNav = document.getElementById('user-nav-links');
        const guestNav = document.getElementById('guest-nav-links');

        if (user) {
            if (guestNav) guestNav.style.setProperty('display', 'none', 'important');
            if (userNav) {
                userNav.style.setProperty('display', 'flex', 'important');
                const nameSpan = document.getElementById('nav-user-name');
                if (nameSpan) nameSpan.textContent = user.fullName || user.username;
            }
        } else {
            if (userNav) userNav.style.setProperty('display', 'none', 'important');
            if (guestNav) guestNav.style.setProperty('display', 'flex', 'important');
        }
    } catch (e) {
        console.error('Error verifying login status:', e);
    }
}

async function logoutUser() {
    try {
        const res = await fetch('/api/auth/logout', { method: 'POST' });
        window.location.href = '/index.html';
    } catch (e) {
        console.error('Error logging out:', e);
    }
}

// Helpers
function formatCurrency(amount) {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount);
}

function formatDate(dateString) {
    if (!dateString || dateString === 'N/A') return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        hour12: true
    });
}
