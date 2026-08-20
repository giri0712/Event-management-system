// =============================================
// Mock Razorpay Checkout - Realistic Payment Modal
// =============================================

(function() {
    // Check if real Razorpay SDK is loaded
    var isRealRazorpay = typeof window.Razorpay !== 'undefined';

    // Mock payment secret (must match backend MOCK_SECRET)
    var MOCK_SECRET = 'mock_secret_eventhub_dev_2026';
    var mockMode = true; // Set via backend config

    // Override the Razorpay constructor if real SDK is not loaded
    if (!isRealRazorpay) {
        window.Razorpay = MockRazorpay;
    }

    function MockRazorpay(options) {
        this.options = options;
        this.handler = options.handler || function() {};
        this.modal_ondismiss = (options.modal && options.modal.ondismiss) || function() {};
    }

    MockRazorpay.prototype.open = function() {
        showMockCheckout(this.options, this.handler, this.modal_ondismiss);
    };

    function formatAmount(amount) {
        return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount / 100);
    }

    function generateMockPaymentId() {
        return 'pay_' + Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
    }

    async function hmacSha256(message, secret) {
        const encoder = new TextEncoder();
        const keyData = encoder.encode(secret);
        const msgData = encoder.encode(message);
        const key = await crypto.subtle.importKey('raw', keyData, { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
        const signature = await crypto.subtle.sign('HMAC', key, msgData);
        return Array.from(new Uint8Array(signature)).map(b => b.toString(16).padStart(2, '0')).join('');
    }

    function showMockCheckout(options, onSuccess, onDismiss) {
        // Create overlay
        var overlay = document.createElement('div');
        overlay.className = 'rzp-overlay';

        // Build modal HTML
        var merchantInitial = (options.name || 'E').charAt(0).toUpperCase();
        var amountFormatted = formatAmount(options.amount);

        var html = '<div class="rzp-modal" style="position:relative;">';

        // Header
        html += '<div class="rzp-header">';
        html += '<div><h3>' + (options.name || 'EventHub') + '</h3>';
        html += '<div class="rzp-amount">' + amountFormatted + '</div></div>';
        html += '<button class="rzp-close" id="rzp-close-btn">&times;</button>';
        html += '</div>';

        // Body
        html += '<div class="rzp-body">';

        // Merchant info
        html += '<div class="rzp-merchant">';
        html += '<div class="rzp-merchant-icon">' + merchantInitial + '</div>';
        html += '<div class="rzp-merchant-info">';
        html += '<div class="rzp-merchant-name">' + (options.name || 'EventHub') + '</div>';
        html += '<div class="rzp-merchant-desc">' + (options.description || 'Payment') + '</div>';
        html += '</div></div>';

        // Test mode hint
        html += '<div class="rzp-test-hint">';
        html += '<span>&#9432;</span> <span><strong>Test Mode</strong> - Use card 4111 1111 1111 1111, any future expiry, CVV 123</span>';
        html += '</div>';

        // Card icons
        html += '<div class="rzp-card-icons">';
        html += '<span class="rzp-card-icon active">VISA</span>';
        html += '<span class="rzp-card-icon">MasterCard</span>';
        html += '<span class="rzp-card-icon">RuPay</span>';
        html += '</div>';

        // Card form
        html += '<form id="rzp-card-form">';

        // Card Number
        html += '<div class="rzp-form-group">';
        html += '<label>Card Number</label>';
        html += '<input class="rzp-input" type="text" id="rzp-card-number" placeholder="1234 5678 9012 3456" maxlength="19" required />';
        html += '</div>';

        // Expiry & CVV row
        html += '<div class="rzp-row">';
        html += '<div class="rzp-form-group">';
        html += '<label>Expiry</label>';
        html += '<input class="rzp-input" type="text" id="rzp-card-expiry" placeholder="MM/YY" maxlength="5" required />';
        html += '</div>';
        html += '<div class="rzp-form-group">';
        html += '<label>CVV</label>';
        html += '<input class="rzp-input" type="password" id="rzp-card-cvv" placeholder="***" maxlength="4" required />';
        html += '</div>';
        html += '</div>';

        // Cardholder Name
        html += '<div class="rzp-form-group">';
        html += '<label>Cardholder Name</label>';
        var prefillName = (options.prefill && options.prefill.name) || '';
        html += '<input class="rzp-input" type="text" id="rzp-card-name" placeholder="Name on card" value="' + prefillName + '" required />';
        html += '</div>';

        // Email (prefill)
        var prefillEmail = (options.prefill && options.prefill.email) || '';
        if (prefillEmail) {
            html += '<div class="rzp-form-group">';
            html += '<label>Email</label>';
            html += '<input class="rzp-input" type="email" id="rzp-email" value="' + prefillEmail + '" readonly style="background:#f9f9f9;" />';
            html += '</div>';
        }

        // Pay button
        html += '<button type="submit" class="rzp-pay-btn" id="rzp-pay-btn">Pay ' + amountFormatted + '</button>';
        html += '</form>';

        // Processing overlay (hidden initially)
        html += '<div class="rzp-processing" id="rzp-processing" style="display:none;">';
        html += '<div class="rzp-spinner"></div>';
        html += '<div class="rzp-processing-text">Processing payment securely...</div>';
        html += '</div>';

        html += '</div>';

        // Footer
        html += '<div class="rzp-footer">';
        html += '<div class="rzp-footer-text"><span class="rzp-lock-icon">&#128274;</span> Secured by Razorpay</div>';
        html += '</div>';

        html += '</div>';

        overlay.innerHTML = html;
        document.body.appendChild(overlay);

        // Close button
        document.getElementById('rzp-close-btn').addEventListener('click', function() {
            overlay.remove();
            onDismiss();
        });

        // Click outside to close
        overlay.addEventListener('click', function(e) {
            if (e.target === overlay) {
                overlay.remove();
                onDismiss();
            }
        });

        // Card number formatting
        document.getElementById('rzp-card-number').addEventListener('input', function(e) {
            var val = this.value.replace(/[^0-9]/g, '');
            var formatted = val.match(/.{1,4}/g);
            this.value = formatted ? formatted.join(' ') : '';
        });

        // Expiry formatting
        document.getElementById('rzp-card-expiry').addEventListener('input', function(e) {
            var val = this.value.replace(/[^0-9]/g, '');
            if (val.length >= 2) {
                this.value = val.substring(0,2) + '/' + val.substring(2,4);
            } else {
                this.value = val;
            }
        });

        // Form submit
        document.getElementById('rzp-card-form').addEventListener('submit', async function(e) {
            e.preventDefault();

            var cardNumber = document.getElementById('rzp-card-number').value.replace(/\s/g, '');
            var expiry = document.getElementById('rzp-card-expiry').value;
            var cvv = document.getElementById('rzp-card-cvv').value;
            var name = document.getElementById('rzp-card-name').value;

            // Basic validation
            if (cardNumber.length < 13) { alert('Please enter a valid card number.'); return; }
            if (!expiry.match(/^(0[1-9]|1[0-2])\/([0-9]{2})$/)) { alert('Please enter a valid expiry date (MM/YY).'); return; }
            if (cvv.length < 3) { alert('Please enter a valid CVV.'); return; }
            if (!name.trim()) { alert('Please enter the cardholder name.'); return; }

            // Show processing
            var payBtn = document.getElementById('rzp-pay-btn');
            var processing = document.getElementById('rzp-processing');
            payBtn.disabled = true;
            processing.style.display = 'flex';

            // Simulate network delay (1.5-2.5s)
            var delay = 1500 + Math.random() * 1000;
            await new Promise(function(r) { setTimeout(r, delay); });

            // Check for decline scenarios
            if (cardNumber.endsWith('0000')) {
                processing.style.display = 'none';
                payBtn.disabled = false;
                alert('Payment Declined: Card was declined by the bank.');
                return;
            }

            if (cardNumber.endsWith('9999')) {
                processing.style.display = 'none';
                payBtn.disabled = false;
                alert('Payment Error: Unable to process transaction. Please try again.');
                return;
            }

            // Generate mock payment response
            var paymentId = generateMockPaymentId();
            var orderId = options.order_id;

            // Generate HMAC signature
            var signature = await hmacSha256(orderId + '|' + paymentId, MOCK_SECRET);

            // Show success
            processing.innerHTML = '<div class="rzp-success"><div class="rzp-success-check">&#10003;</div><h3>Payment Successful</h3><p>Transaction ID: ' + paymentId + '</p></div>';

            // Close and callback after brief delay
            setTimeout(function() {
                overlay.remove();
                onSuccess({
                    razorpay_order_id: orderId,
                    razorpay_payment_id: paymentId,
                    razorpay_signature: signature
                });
            }, 1200);
        });
    }
})();