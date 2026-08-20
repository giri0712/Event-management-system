// =============================================
// EventHub - Auth (auth.js)
// =============================================
(function() {
    document.addEventListener('DOMContentLoaded', function() {
        var loginForm = document.getElementById('login-form');
        var registerForm = document.getElementById('register-form');

        if (loginForm) {
            loginForm.addEventListener('submit', handleLogin);
        }
        if (registerForm) {
            registerForm.addEventListener('submit', handleRegister);
        }

        // Redirect if already logged in
        if (window.currentUser) {
            window.location.href = 'dashboard.html';
        }
    });

    async function handleLogin(e) {
        e.preventDefault();
        var username = document.getElementById('username').value;
        var password = document.getElementById('password').value;

        try {
            var user = await fetchAPI('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify({ username: username, password: password })
            });
            window.currentUser = user;
            showToast('Welcome back, ' + user.fullName + '!', 'success');
            setTimeout(function() { window.location.href = 'dashboard.html'; }, 800);
        } catch (err) {
            showToast(err.message || 'Login failed', 'error');
        }
    }

    async function handleRegister(e) {
        e.preventDefault();
        var fullName = document.getElementById('fullName').value;
        var username = document.getElementById('username').value;
        var email = document.getElementById('email').value;
        var password = document.getElementById('password').value;
        var role = document.getElementById('role').value;

        try {
            var user = await fetchAPI('/api/auth/register', {
                method: 'POST',
                body: JSON.stringify({
                    fullName: fullName,
                    username: username,
                    email: email,
                    password: password,
                    role: role
                })
            });
            showToast('Account created! Please sign in.', 'success');
            setTimeout(function() { window.location.href = 'login.html'; }, 1000);
        } catch (err) {
            showToast(err.message || 'Registration failed', 'error');
        }
    }
})();