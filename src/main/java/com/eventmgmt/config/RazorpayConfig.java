package com.eventmgmt.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RazorpayConfig {

    private static final Logger log = LoggerFactory.getLogger(RazorpayConfig.class);

    @Value("${razorpay.key.id:rzp_test_YOUR_KEY_ID}")
    private String keyId;

    @Value("${razorpay.key.secret:YOUR_KEY_SECRET}")
    private String keySecret;

    @Value("${razorpay.mock:false}")
    private boolean mockMode;

    /**
     * Determines if we should use mock mode.
     * Auto-enables mock when keys are placeholder values.
     */
    @Bean
    @Primary
    public boolean useMockRazorpay() {
        boolean isPlaceholder = keyId.contains("YOUR_KEY") || keySecret.contains("YOUR_KEY")
                || keyId.equals("rzp_test_YOUR_KEY_ID") || keySecret.equals("YOUR_KEY_SECRET");
        boolean shouldMock = mockMode || isPlaceholder;
        if (shouldMock) {
            log.warn("========================================");
            log.warn("  RAZORPAY MOCK MODE ENABLED");
            log.warn("  Using simulated payment gateway.");
            log.warn("  Replace keys in application.properties");
            log.warn("  to switch to live Razorpay.");
            log.warn("========================================");
        }
        return shouldMock;
    }

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        if (useMockRazorpay()) {
            log.info("RazorpayClient not initialized (mock mode active)");
            return null;
        }
        return new RazorpayClient(keyId, keySecret);
    }

    @Bean
    public String razorpayKeyId() {
        return keyId;
    }

    public String getKeyId() {
        return keyId;
    }
}
