// EventHub - Core JavaScript (app.js)
var API_BASE_URL = window.origin + "/api";

async function fetchAPI(endpoint, options) {
    options = options || {};
    var hdrs = Object.assign({"Content-Type":"application/json"}, options.headers || {});
    var opts = Object.assign({credentials:"include"}, options, {headers:hdrs});
    var resp = await fetch(API_BASE_URL + endpoint, opts);
    if (resp.status === 401 || resp.status === 403) {
        window.currentUser = null;
        if (!window.location.pathname.includes("login.html")) {
            showToast("Session expired.", "warning");
            setTimeout(function(){ window.location.href = "login.html"; }, 1500);
        }
        throw new Error("Unauthorized");
    }
    if (resp.status === 204) return null;
    var ct = resp.headers.get("content-type");
    var data = (ct && ct.indexOf("json") !== -1) ? await resp.json() : await resp.text();
    if (!resp.ok) throw new Error((data && (data.message || data.error)) || "Request failed");
    return data;
}

function showToast(message, type) {
    type = type || "info";
    var container = document.getElementById("toast-container");
    if (!container) {
        container = document.createElement("div");
        container.id = "toast-container";
        container.style.cssText = "position:fixed;top:20px;right:20px;z-index:9999;display:flex;flex-direction:column;gap:10px;";
        document.body.appendChild(container);
    }
    var toast = document.createElement("div");
    toast.className = "toast toast-" + type;
    var icons = {success:"\u2713", error:"\u2717", warning:"\u26A0", info:"\u2139"};
    var iconSpan = document.createElement("span");
    iconSpan.className = "toast-icon";
    iconSpan.textContent = icons[type] || icons.info;
    var msgSpan = document.createElement("span");
    msgSpan.className = "toast-message";
    msgSpan.textContent = message;
    var closeBtn = document.createElement("button");
    closeBtn.className = "toast-close";
    closeBtn.textContent = "\u00D7";
    toast.appendChild(iconSpan);
    toast.appendChild(msgSpan);
    toast.appendChild(closeBtn);
    container.appendChild(toast);
    var timer = setTimeout(function(){ toast.style.opacity="0"; setTimeout(function(){toast.remove();},300); }, 4000);
    closeBtn.addEventListener("click", function(){ clearTimeout(timer); toast.remove(); });
}

function initiateRazorpayCheckout(options) {
    return new Promise(function(resolve, reject) {
        if (typeof Razorpay === "undefined") { reject(new Error("Razorpay SDK not loaded.")); return; }
        var rzp = new Razorpay({
            key: options.key,
            amount: options.amount,
            currency: options.currency || "INR",
            name: options.name || "EventHub",
            description: options.description || "Event Booking",
            order_id: options.orderId,
            handler: function(resp) { resolve(resp); },
            prefill: {name: options.prefillName||"", email: options.prefillEmail||"", contact: options.prefillContact||""},
            notes: options.notes || {},
            theme: {color: "#ff6b35"},
            modal: {ondismiss: function(){ reject(new Error("Payment cancelled.")); }}
        });
        rzp.open();
    });
}

function formatDate(dateStr) {
    if (!dateStr) return "";
    return new Date(dateStr).toLocaleDateString("en-US", {weekday:"short", year:"numeric", month:"short", day:"numeric", hour:"2-digit", minute:"2-digit"});
}

function formatCurrency(amount) {
    if (amount == null) return "\u20B90";
    return new Intl.NumberFormat("en-IN", {style:"currency", currency:"INR"}).format(amount);
}

function updateNavigation() {
    var authNav = document.getElementById("auth-nav-links");
    if (!authNav) return;
    if (window.currentUser) {
        var initial = window.currentUser.fullName.charAt(0).toUpperCase();
        authNav.innerHTML = "<li><button onclick=\"handleLogout()\" class=\"btn btn-ghost btn-sm\">Sign Out</button></li>" +
            "<li><div style=\"width:32px;height:32px;border-radius:50%;background:linear-gradient(135deg,#ff6b35,#ff8a5c);display:flex;align-items:center;justify-content:center;font-weight:700;font-size:0.85rem;\">" + initial + "</div></li>";
    } else {
        authNav.innerHTML = "<li><a href=\"login.html\" class=\"btn btn-ghost btn-sm\">Sign In</a></li>" +
            "<li><a href=\"register.html\" class=\"btn btn-primary btn-sm\">Sign Up</a></li>";
    }
}

async function handleLogout() {
    try {
        await fetchAPI("/api/auth/logout", {method:"POST"});
        window.currentUser = null;
        showToast("Logged out successfully", "success");
        setTimeout(function(){ window.location.href = "index.html"; }, 800);
    } catch (err) { showToast(err.message || "Failed to logout", "error"); }
}

async function checkAuthSession() {
    try {
        var user = await fetchAPI("/api/auth/me");
        if (user && !user.error) window.currentUser = user;
    } catch (err) {}
    finally { updateNavigation(); }
}

document.addEventListener("DOMContentLoaded", checkAuthSession);