// =============================================
// EventHub - Landing Page (main.js)
// =============================================
(function() {
    document.addEventListener('DOMContentLoaded', init);

    async function init() {
        await Promise.all([loadEvents(), loadRankingsPreview()]);
        setupSearch();
    }

    async function loadEvents() {
        try {
            const events = await fetchAPI('/api/events');
            renderEvents(events);
        } catch (err) {
            document.getElementById('events-grid').innerHTML =
                '<div class="empty-state"><h3>Could not load events</h3><p>' + err.message + '</p></div>';
        }
    }

    function renderEvents(events) {
        const grid = document.getElementById('events-grid');
        if (!events || events.length === 0) {
            grid.innerHTML = '<div class="empty-state"><h3>No upcoming events</h3><p>Check back soon!</p></div>';
            return;
        }

        grid.innerHTML = events.map(ev => {
            const pct = ev.capacity > 0 ? Math.round((ev.ticketsSold / ev.capacity) * 100) : 0;
            const remaining = ev.capacity - ev.ticketsSold;
            const isSoldOut = remaining <= 0;
            const isFillingFast = pct > 70 && !isSoldOut;

            let badgeClass = '';
            let badgeText = remaining + ' spots left';
            if (isSoldOut) { badgeClass = 'sold-out'; badgeText = 'Sold Out'; }
            else if (isFillingFast) { badgeClass = 'filling-fast'; badgeText = 'Filling Fast'; }

            const priceHtml = ev.price > 0
                ? '<span class="event-price">' + formatCurrency(ev.price) + '</span>'
                : '<span class="event-price free">Free</span>';

            const bannerUrl = ev.bannerUrl || 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800';

            return '<div class="event-card" onclick="window.location.href=\'event.html?id=' + ev.id + '\'">' +
                '<img class="event-card-image" src="' + bannerUrl + '" alt="' + ev.title + '" onerror="this.src=\'https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800\'" />' +
                '<div class="event-card-body">' +
                    '<div class="event-card-meta">' +
                        '<span class="event-badge ' + badgeClass + '">' + badgeText + '</span>' +
                        '<span class="event-date-badge">' + formatDate(ev.dateTime) + '</span>' +
                    '</div>' +
                    '<h3>' + ev.title + '</h3>' +
                    '<p class="event-card-desc">' + (ev.description || '') + '</p>' +
                    '<div class="event-location">&#128205; ' + (ev.location || 'TBD') + '</div>' +
                    '<div class="event-card-footer">' +
                        priceHtml +
                        '<div class="event-registrations">' +
                            '<div class="registration-bar"><div class="registration-fill" style="width:' + pct + '%"></div></div>' +
                            '<span>' + ev.ticketsSold + ' registered</span>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
        }).join('');
    }

    async function loadRankingsPreview() {
        try {
            const teams = await fetchAPI('/api/teams');
            renderRankingsPreview(teams.slice(0, 3));
        } catch (err) {
            document.getElementById('rankings-preview').innerHTML =
                '<div class="empty-state"><h3>Rankings unavailable</h3></div>';
        }
    }

    function renderRankingsPreview(teams) {
        const el = document.getElementById('rankings-preview');
        if (!teams || teams.length === 0) {
            el.innerHTML = '<div class="empty-state"><h3>No teams yet</h3></div>';
            return;
        }

        var html = '<table class="rankings-table"><thead><tr>' +
            '<th>Rank</th><th>Team</th><th>Events</th><th>Registrations</th><th>Total Spent</th>' +
            '</tr></thead><tbody>';

        teams.forEach(function(t) {
            var rankClass = t.ranking <= 3 ? 'rank-' + t.ranking : 'rank-other';
            var initials = t.name.split(' ').map(function(w) { return w[0]; }).join('').substring(0, 2);
            html += '<tr>' +
                '<td><span class="rank-number ' + rankClass + '">' + t.ranking + '</span></td>' +
                '<td><div class="team-info"><div class="team-avatar">' + initials + '</div><div>' +
                    '<div class="team-name">' + t.name + '</div>' +
                    '<div class="team-dept">' + t.department + '</div></div></div></td>' +
                '<td class="stat-value">' + t.eventsAttended + '</td>' +
                '<td class="stat-value">' + t.totalRegistrations + '</td>' +
                '<td class="stat-value">' + formatCurrency(t.totalSpent) + '</td>' +
            '</tr>';
        });

        html += '</tbody></table>';
        el.innerHTML = html;
    }

    function setupSearch() {
        var input = document.getElementById('search-input');
        var btn = document.getElementById('search-btn');
        if (btn) {
            btn.addEventListener('click', function() { doSearch(input.value); });
        }
        if (input) {
            input.addEventListener('keypress', function(e) {
                if (e.key === 'Enter') doSearch(input.value);
            });
        }
    }

    async function doSearch(query) {
        if (!query || !query.trim()) {
            loadEvents();
            return;
        }
        try {
            var events = await fetchAPI('/api/events?search=' + encodeURIComponent(query.trim()));
            renderEvents(events);
        } catch (err) {
            showToast('Search failed: ' + err.message, 'error');
        }
    }
})();