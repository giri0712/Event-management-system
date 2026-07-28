package com.eventmgmt.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.ServletRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<Map<String, Object>> handlePaymentFailed(PaymentFailedException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(TicketCapacityExceededException.class)
    public ResponseEntity<Map<String, Object>> handleTicketCapacityExceeded(TicketCapacityExceededException ex) {
        return createErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedAccess(UnauthorizedAccessException ex) {
        return createErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleConcurrencyConflict(ObjectOptimisticLockingFailureException ex) {
        return createErrorResponse(HttpStatus.CONFLICT, "Transaction conflict: tickets were purchased by another user. Please retry booking.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        body.put("validationErrors", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        body.put("validationErrors", errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        return createErrorResponse(HttpStatus.BAD_REQUEST, "Missing request parameter: " + ex.getParameterName"""
    container.appendChild(toast)

    // Auto-remove after 4 seconds
    const timer = setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, 4000)

    toast.querySelector('.toast-close').addEventListener('click', () => {
        clearTimeout(timer)
        toast.remove()
    })
}

// Format Date & Time locally
function formatDate(dateStr) {
    const d = new Date(dateStr)
    return d.toLocaleDateString('en-US', {
        weekday: 'short',
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    })
}

// Format Currency
function formatCurrency(amount) {
    return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount)
}

// Dynamic Navigation update based on Auth status
function updateNavigation() {
    const authNav = document.getElementById('auth-nav-links')
    if (!authNav) return

    if (window.currentUser) {
        authNav.innerHTML = `
            <li><a href="index.html" class="${isActivePage('index.html')}">Explore</a></li>
            <li><a href="dashboard.html" class="${isActivePage('dashboard.html')}">Dashboard</a></li>
            <li>
                <button onclick="handleLogout()" class="btn btn-secondary" style="padding: 0.4rem 1rem; font-size: 0.85rem;">
                    Sign Out
                </button>
            </li>
            <li style="margin-left: 0.5rem; display: flex; align-items: center; gap: 0.5rem;">
                <div style="width: 32px; height: 32px; border-radius: 50%; background: linear-gradient(135deg, var(--primary), var(--secondary)); display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 0.85rem;">
                    ${window.currentUser.fullName.charAt(0).toUpperCase()}
                </div>
            </li>
        `
    } else {
        authNav.innerHTML = `
            <li><a href="index.html" class="${isActivePage('index.html')}">Explore</a></li>
            <li><a href="login.html" class="btn btn-primary" style="padding: 0.5rem 1.2rem; font-size: 0.85rem;">Sign In</a></li>
        `
    }
}

function isActivePage(filename) {
    const currentPage = window.location.pathname.split('/').pop()
    if (currentPage === '' && filename === 'index.html') return 'active'
    return currentPage === filename ? 'active' : ''
}

// Global Sign-Out execution
async function handleLogout() {
    try {
        await fetchAPI('/api/auth/logout', { method: 'POST' })
        window.currentUser = null
        showToast('Logged out successfully', 'success')
        setTimeout(() => {
            window.location.href = 'index.html'
        }, 1000)
    } catch (err) {
        showToast(err.message || 'Failed to logout', 'error')
    }
}

// Check active session on load
async function checkAuthSession() {
    try {
        const user = await fetchAPI('/api/auth/me')
        if (user && !user.error) {
            window.currentUser = user
        }
    } catch (err) {
        // Ignored: User not logged in
    } finally {
        updateNavigation()
    }
}

// Generate Mock QR Code matrix
function generateMockQRCode(parentEl, payload) {
    if (!parentEl) return
    parentEl.innerHTML = ''

    // Hash payload to pseudo-randomize pixel distribution
    let hash = 0
    for (let i = 0; i < payload.length; i++) {
        hash = payload.charCodeAt(i) + ((hash << 5) - hash)
    }

    const size = 16 // 16x16 QR grid
    parentEl.style.width = '100px'
    parentEl.style.height = '100px'
    parentEl.style.display = 'flex'
    parentEl.style.flexWrap = 'wrap'
    parentEl.style.background = 'white'
    parentEl.style.padding = '4px'

    for (let row = 0; row < size; row++) {
        for (let col = 0; col < size; col++) {
            const pixel = document.createElement('div')
            pixel.style.width = '6px'
            pixel.style.height = '6px'

            // Generate standard positioning block markers in corners
            const isCorner =
                (row < 4 && col < 4) ||
                (row < 4 && col >= size - 4) ||
                (row >= size - 4 && col < 4)

            if (isCorner) {
                // Outer ring black, inner dot black, middle white
                const inner = (row === 0 || row === 3 || col === 0 || col === 3 ||
                               row === size - 1 || row === size - 4 || col === size - 1 || col === size - 4)
                pixel.style.backgroundColor = inner ? '#000' : '#fff'
                // Adjust for middle dot
                if ((row === 1 && col === 1) || (row === 1 && col === size - 2) || (row === size - 2 && col === 1) ||
                    (row === 2 && col === 2) || (row === 2 && col === size - 3) || (row === size - 3 && col === 2)) {
                    pixel.style.backgroundColor = '#000'
                }
            } else {
                // Pseudo-random distribution based on hash logic
                const val = Math.abs(Math.sin(hash + (row * size) + col) * 1000)
                pixel.style.backgroundColor = (val % 2 < 1) ? '#000' : '#fff'
            }
            parentEl.appendChild(pixel)
        }
    }
}

// Initialise auth session extraction
document.addEventListener('DOMContentLoaded', checkAuthSession)