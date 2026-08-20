// =============================================
// EventHub - Dashboard (dashboard.js)
// =============================================
(function() {
    document.addEventListener('DOMContentLoaded', init);

    async function init() {
        if (!window.currentUser) {
            document.getElementById('dashboard-container').innerHTML =
                '<div class="empty-state"><h3>Please sign in</h3><p><a href="login.html" class="btn btn-primary">Sign In</a></p></div>';
            return;
        }
        document.getElementById('dashboard-container').innerHTML =
            '<div class="dashboard-header"><h1>Welcome, ' + window.currentUser.fullName + '</h1>' +
            '<p>Manage your bookings and track your event activity.</p></div>' +
            '<div class="dashboard-stats" id="dash-stats"></div>' +
            '<div class="section-header"><h2>My Bookings</h2></div>' +
            '<div class="bookings-list" id="bookings-list">' +
            '<div class="loading-spinner"><div class="spinner"></div><p>Loading bookings...</p></div></div>';
        await loadBookings();
    }

    async function loadBookings() {
        try {
            var bookings = await fetchAPI('/api/bookings/my-bookings');
            renderBookings(bookings);
        } catch (err) {
            document.getElementById('bookings-list').innerHTML =
                '<div class="empty-state"><h3>No bookings yet</h3><p>Start exploring events!</p>' +
                '<a href="index.html" class="btn btn-primary" style="margin-top:1rem;">Browse Events</a></div>';
            document.getElementById('dash-stats').innerHTML =
                '<div class="stat-card"><div class="stat-number">0</div><div class="stat-label">Total Bookings</div></div>' +
                '<div class="stat-card"><div class="stat-number">' + formatCurrency(0) + '</div><div class="stat-label">Total Spent</div></div>' +
                '<div class="stat-card"><div class="stat-number">0</div><div class="stat-label">Tickets</div></div>';
        }
    }

    function renderBookings(bookings) {
        var totalSpent = 0;
        var totalTickets = 0;

        bookings.forEach(function(b) {
            totalSpent += b.totalAmount || 0;
            totalTickets += b.ticketCount || 0;
        });

        document.getElementById('dash-stats').innerHTML =
            '<div class="stat-card"><div class="stat-number">' + bookings.length + '</div><div class="stat-label">Total Bookings</div></div>' +
            '<div class="stat-card"><div class="stat-number">' + formatCurrency(totalSpent) + '</div><div class="stat-label">Total Spent</div></div>' +
            '<div class="stat-card"><div class="stat-number">' + totalTickets + '</div><div class="stat-label">Tickets</div></div>';

        var list = document.getElementById('bookings-list');
        if (bookings.length === 0) {
            list.innerHTML = '<div class="empty-state"><h3>No bookings yet</h3><p>Start exploring events!</p>' +
                '<a href="index.html" class="btn btn-primary" style="margin-top:1rem;">Browse Events</a></div>';
            return;
        }

        list.innerHTML = bookings.map(function(b) {
            var statusClass = b.status === 'CONFIRMED' ? 'badge-success' : 'badge-error';
            var eventTitle = b.event ? b.event.title : 'Unknown Event';
            var eventDate = b.event ? formatDate(b.event.dateTime) : '';

            return '<div class="booking-item">' +
                '<div class="booking-info">' +
                    '<h3>' + eventTitle + '</h3>' +
                    '<div class="booking-meta">' +
                        '<span>' + eventDate + '</span>' +
                        '<span>' + b.ticketCount + ' ticket(s)</span>' +
                        '<span class="badge ' + statusClass + '">' + b.status + '</span>' +
                    '</div>' +
                '</div>' +
                '<div class="booking-amount">' +
                    '<div class="amount">' + formatCurrency(b.totalAmount) + '</div>' +
                    '<div style="font-size:0.75rem;color:var(--text-muted);">' + b.transactionId + '</div>' +
                '</div>' +
            '</div>';
        }).join('');
    }
})();