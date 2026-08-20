// =============================================
// EventHub - Event Detail Page (event-detail.js)
// =============================================
(function() {
    var currentEvent = null;
    var ticketCount = 1;
    var razorpayKey = null;

    document.addEventListener('DOMContentLoaded', init);

    async function init() {
        // Fetch Razorpay config
        try {
            var config = await fetchAPI('/api/bookings/razorpay-config');
            razorpayKey = config.keyId;
            window.RAZORPAY_KEY = config.keyId;
        } catch(e) { console.log('Could not load Razorpay config:', e.message); }

        var params = new URLSearchParams(window.location.search);
        var eventId = params.get('id');
        if (!eventId) {
            document.getElementById('event-detail-container').innerHTML =
                '<div class="empty-state"><h3>Event not found</h3><p><a href="index.html" class="btn btn-primary">Browse events</a></p></div>';
            return;
        }
        await loadEvent(eventId);
    }

    async function loadEvent(eventId) {
        try {
            currentEvent = await fetchAPI('/api/events/' + eventId);
            renderEvent(currentEvent);
        } catch (err) {
            document.getElementById('event-detail-container').innerHTML =
                '<div class="empty-state"><h3>Error loading event</h3><p>' + err.message + '</p></div>';
        }
    }

    function renderEvent(ev) {
        var c = document.getElementById('event-detail-container');
        var isPast = new Date(ev.dateTime) < new Date();
        var remaining = ev.capacity - ev.ticketsSold;
        var pct = ev.capacity > 0 ? Math.round((ev.ticketsSold / ev.capacity) * 100) : 0;

        var bannerUrl = ev.bannerUrl || 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800';

        var html = '<div class="event-detail-card">';
        html += '<img class="event-detail-banner" src="' + bannerUrl + '" alt="' + ev.title + '" onerror="this.src=\'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800\'" />';
        html += '<div class="event-detail-content">';
        html += '<h1>' + ev.title + '</h1>';

        // Meta grid
        html += '<div class="event-meta-grid">';
        html += '<div class="meta-item"><div class="meta-label">Date & Time</div><div class="meta-value">' + formatDate(ev.dateTime) + '</div></div>';
        html += '<div class="meta-item"><div class="meta-label">Location</div><div class="meta-value">' + (ev.location || 'TBD') + '</div></div>';
        html += '<div class="meta-item"><div class="meta-label">Price</div><div class="meta-value">' + (ev.price > 0 ? formatCurrency(ev.price) : 'Free') + '</div></div>';
        html += '<div class="meta-item"><div class="meta-label">Registrations</div><div class="meta-value">' + ev.ticketsSold + ' / ' + ev.capacity + '</div></div>';
        html += '</div>';

        // Registration progress
        html += '<div style="margin: 1rem 0;">';
        html += '<div class="registration-bar" style="width:100%;height:6px;"><div class="registration-fill" style="width:' + pct + '%"></div></div>';
        html += '<div style="display:flex;justify-content:space-between;margin-top:0.5rem;font-size:0.8rem;color:var(--text-muted);">';
        html += '<span>' + ev.ticketsSold + ' registered</span>';
        html += '<span>' + remaining + ' spots left</span>';
        html += '</div></div>';

        html += '<div class="event-description-full">' + (ev.description || '') + '</div>';
        html += '</div></div>';

        // Booking section
        if (!isPast && remaining > 0 && window.currentUser) {
            html += '<div class="booking-section">';
            html += '<h3>Book Tickets</h3>';
            html += '<div class="ticket-selector">';
            html += '<label>Number of Tickets:</label>';
            html += '<input type="number" id="ticket-count" min="1" max="' + Math.min(remaining, 10) + '" value="1" />';
            html += '</div>';
            html += '<div class="total-display">Total: <span id="total-amount">' + formatCurrency(ev.price) + '</span></div>';
            html += '<button id="pay-now-btn" class="btn btn-primary btn-lg" style="width:100%;">Pay with Razorpay</button>';
            html += '</div>';
        } else if (!window.currentUser && !isPast && remaining > 0) {
            html += '<div class="booking-section" style="text-align:center;">';
            html += '<p style="margin-bottom:1rem;color:var(--text-secondary);">Sign in to book tickets for this event</p>';
            html += '<a href="login.html" class="btn btn-primary btn-lg">Sign In</a>';
            html += '</div>';
        } else if (isPast) {
            html += '<div class="booking-section" style="text-align:center;">';
            html += '<p style="color:var(--text-muted);">This event has ended.</p>';
            html += '</div>';
        } else if (remaining <= 0) {
            html += '<div class="booking-section" style="text-align:center;">';
            html += '<p style="color:var(--error);font-weight:600;">This event is sold out.</p>';
            html += '</div>';
        }

        c.innerHTML = html;

        // Event listeners
        var ti = document.getElementById('ticket-count');
        if (ti) {
            ti.addEventListener('input', function() {
                ticketCount = parseInt(this.value) || 1;
                document.getElementById('total-amount').textContent = formatCurrency(ev.price * ticketCount);
            });
        }
        var pb = document.getElementById('pay-now-btn');
        if (pb) pb.addEventListener('click', startPayment);
    }

    async function startPayment() {
        if (!currentEvent) return;
        var btn = document.getElementById('pay-now-btn');
        btn.disabled = true;
        btn.textContent = 'Creating order...';

        try {
            var orderData = await fetchAPI('/api/bookings/create-order', {
                method: 'POST',
                body: JSON.stringify({ eventId: currentEvent.id, ticketCount: ticketCount })
            });

            btn.textContent = 'Opening payment...';

            var pr = await initiateRazorpayCheckout({
                key: razorpayKey || 'rzp_test_YOUR_KEY_ID',
                amount: orderData.amount,
                currency: orderData.currency || 'INR',
                name: 'EventHub',
                description: currentEvent.title + ' - ' + ticketCount + ' ticket(s)',
                orderId: orderData.orderId,
                prefillName: window.currentUser.fullName,
                prefillEmail: window.currentUser.email
            });

            btn.textContent = 'Verifying...';

            var booking = await fetchAPI('/api/bookings/verify-payment', {
                method: 'POST',
                body: JSON.stringify({
                    razorpayOrderId: pr.razorpay_order_id,
                    razorpayPaymentId: pr.razorpay_payment_id,
                    razorpaySignature: pr.razorpay_signature,
                    eventId: currentEvent.id,
                    ticketCount: ticketCount
                })
            });

            showToast('Payment successful! Booking confirmed.', 'success');

            document.getElementById('event-detail-container').innerHTML =
                '<div class="booking-confirmation">' +
                    '<div class="confirmation-icon">&#10003;</div>' +
                    '<h2>Booking Confirmed!</h2>' +
                    '<div class="confirmation-details">' +
                        '<p><span>Transaction ID</span><span>' + booking.transactionId + '</span></p>' +
                        '<p><span>Event</span><span>' + currentEvent.title + '</span></p>' +
                        '<p><span>Tickets</span><span>' + booking.ticketCount + '</span></p>' +
                        '<p><span>Total</span><span>' + formatCurrency(booking.totalAmount) + '</span></p>' +
                        '<p><span>Status</span><span class="badge badge-success">CONFIRMED</span></p>' +
                    '</div>' +
                    '<div class="hero-actions">' +
                        '<a href="dashboard.html" class="btn btn-primary">View My Bookings</a>' +
                        '<a href="index.html" class="btn btn-secondary">Browse More Events</a>' +
                    '</div>' +
                '</div>';

        } catch (err) {
            showToast(err.message || 'Payment failed', 'error');
            btn.disabled = false;
            btn.textContent = 'Pay with Razorpay';
        }
    }
})();