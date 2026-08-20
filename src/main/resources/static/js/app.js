// =============================================
// EventHub - Core JavaScript (app.js)
// =============================================

const API_BASE_URL = window.origin + "/api";

// =============================================
// API Helper with Session Cookie Auth
// =============================================
async function fetchAPI(endpoint, options = {}) {
    const defaultOptions = {
        credentials: "include",
        headers: { "Content-Type": "application/json" },
    };
    const mergedOptions = { ...defaultOptions, ...options, headers: { ...defaultOptions.headers, ...options.headers } };
    const response = await fetch(API_BASE_URL + endpoint, mergedOptions);
    if (response.status === 401 || response.status === 403) {
        window.currentUser = null;
        if (!window.location.pathname.includes("login.html")) {
            showToast("Session expired. Please sign in again.", "warning");
            setTimeout(() => { window.location.href = "login.html"; }, 1500);
        }
        throw new Error("Unauthorized");
    }
    if (response.status === 204) return null;
    const ct = response.headers.get("content-type");
    let data = (ct && ct.includes("application/json")) ? await response.json() : await response.text();
    if (!response.ok) throw new Error((data && (data.message || data.error)) || "Request failed");
    return data;
}

// =============================================
// Toast Notifications
// =============================================
function showToast(message, type = "info") {
    let container = document.getElementById("toast-container");
    if (!container) {
        container = document.createElement("div");
        container.id = "toast-container";
        container.style.cssText = "position:fixed;top:20px;right:20px;z-index:9999;display:flex;flex-direction:column;gap:10px;";
        document.body.appendChild(container);
    }
    const toast = document.createElement("div");
    toast.className = "toast toast-" + type;
    const icons = { success: "✓", error: "✗", warning: "⚠", info: "ℹ" };
    toast.innerHTML = "<span class=\"toast-icon\">" + (icons[type] || icons.info) + "</span><span class=\"toast-message\">" + message + "</span><button class=\"toast-close\">&times;</button>";
    container.appendChild(toast);
    const timer = setTimeout(() => { toast.style.opacity = "0"; setTimeout(() => toast.remove(), 300); }, 4000);
    toast.querySelector(".toast-close").addEventListener("click", () => { clearTimeout(timer); toast.remove(); });
}

// =============================================
// Razorpay Checkout Helper
// =============================================
function initiateRazorpayCheckout(options) {
    return new Promise((resolve, reject) => {
        if (typeof Razorpay === "undefined") { reject(new Error("Razorpay SDK not loaded.")); return; }
        const rzp = new Razorpay({
            key: options.key,
            amount: options.amount,
            currency: options.currency || "INR",
            name: options.name || "EventHub",
            description: options.description || "Event Booking",
            order_id: options.orderId,
            handler: function(response) { resolve(response); },
            prefill: { name: options.prefillName || "", email: options.prefillEmail || "", contact: options.prefillContact || "" },
            notes: options.notes || {},
            theme: { color: "#6366f1" },
            modal: { ondismiss: function() { reject(new Error("Payment cancelled by user.")); } }
        });
        rzp.open();
    });
}

// =============================================
// Date & Currency Formatting
// =============================================
function formatDate(dateStr) {
    return new Date(dateStr).toLocaleDateString("en-US", { weekday: "short", year: "numeric", month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

function formatCurrency(amount) {
    return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(amount);
}

// =============================================
// Navigation
// =============================================
function updateNavigation() {
    const authNav = document.getElementById("auth-nav-links");
    if (!authNav) return;
    if (window.currentUser) {
        authNav.innerHTML = "<li><a href="index.html">Explore</a></li><li><a href="dashboard.html">Dashboard</a></li><li><button onclick="handleLogout()" class="btn btn-secondary">Sign Out</button></li>";
    } else {
        authNav.innerHTML = "<li><a href="index.html">Explore</a></li><li><a href="login.html" class="btn btn-primary">Sign In</a></li>";
    }
}

// =============================================
// Auth
// =============================================
async function handleLogout() {
    try {
        await fetchAPI("/api/auth/logout", { method: "POST" });
        window.currentUser = null;
        showToast("Logged out successfully", "success");
        setTimeout(() => { window.location.href = "index.html"; }, 1000);
    } catch (err) { showToast(err.message || "Failed to logout", "error"); }
}

async function checkAuthSession() {
    try {
        const user = await fetchAPI("/api/auth/me");
        if (user && !user.error) window.currentUser = user;
    } catch (err) { /* not logged in */ } finally { updateNavigation(); }
}

// =============================================
// Init
// =============================================
document.addEventListener("DOMContentLoaded", checkAuthSession);