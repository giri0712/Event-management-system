// =============================================
// EventHub - Rankings Page (rankings.js)
// =============================================
(function() {
    document.addEventListener('DOMContentLoaded', loadRankings);

    async function loadRankings() {
        try {
            var teams = await fetchAPI('/api/teams');
            renderFullRankings(teams);
        } catch (err) {
            document.getElementById('rankings-full').innerHTML =
                '<div class="empty-state"><h3>Could not load rankings</h3><p>' + err.message + '</p></div>';
        }
    }

    function renderFullRankings(teams) {
        var el = document.getElementById('rankings-full');
        if (!teams || teams.length === 0) {
            el.innerHTML = '<div class="empty-state"><h3>No teams found</h3><p>Teams will appear here once they start participating in events.</p></div>';
            return;
        }

        var html = '<table class="rankings-table"><thead><tr>' +
            '<th>Rank</th><th>Team</th><th>Department</th><th>Events Attended</th><th>Registrations</th><th>Total Spent</th>' +
            '</tr></thead><tbody>';

        teams.forEach(function(t) {
            var rankClass = t.ranking <= 3 ? 'rank-' + t.ranking : 'rank-other';
            var initials = t.name.split(' ').map(function(w) { return w[0]; }).join('').substring(0, 2);

            html += '<tr>' +
                '<td><span class="rank-number ' + rankClass + '">' + t.ranking + '</span></td>' +
                '<td><div class="team-info"><div class="team-avatar">' + initials + '</div>' +
                    '<div class="team-name">' + t.name + '</div></div></td>' +
                '<td><span class="badge badge-info">' + t.department + '</span></td>' +
                '<td class="stat-value">' + t.eventsAttended + '</td>' +
                '<td class="stat-value">' + t.totalRegistrations + '</td>' +
                '<td class="stat-value">' + formatCurrency(t.totalSpent) + '</td>' +
            '</tr>';
        });

        html += '</tbody></table>';
        el.innerHTML = html;
    }
})();