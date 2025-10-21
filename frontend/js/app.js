// Instant Invoice: Fraud Shield - Advanced Payment Fraud Detection
class SupplierFraudDetectionApp {
    constructor() {
        this.apiBaseUrl = '/api';
        this.authToken = null;
        this.validations = [];
        this.stats = {
            totalPayments: 0,
            fraudDetected: 0,
            avgResponseTime: 0,
            successRate: 0
        };
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.checkAuthStatus();
        this.loadRecentValidations();
        this.setupAnimations();
    }

    setupEventListeners() {
        // Login form with enhanced validation
        const loginForm = document.getElementById('loginFormElement');
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => this.handleLogin(e));
            
            // Real-time validation
            const inputs = loginForm.querySelectorAll('input');
            inputs.forEach(input => {
                input.addEventListener('blur', () => this.validateInput(input));
                input.addEventListener('input', () => this.clearInputError(input));
            });
        }

        // Logout button
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => this.handleLogout());
        }

        // Payment validation form with enhanced UX
        const validationForm = document.getElementById('paymentValidationForm');
        if (validationForm) {
            validationForm.addEventListener('submit', (e) => this.handlePaymentValidation(e));
            
            // Real-time IBAN validation
            const ibanInput = document.getElementById('supplierIban');
            if (ibanInput) {
                ibanInput.addEventListener('input', () => this.validateIBAN(ibanInput));
            }
            
            // Form field validation
            const inputs = validationForm.querySelectorAll('input, select');
            inputs.forEach(input => {
                input.addEventListener('blur', () => this.validateFormField(input));
                input.addEventListener('input', () => this.clearFieldError(input));
            });
        }

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => this.handleKeyboardShortcuts(e));
    }

    setupAnimations() {
        // Add smooth transitions to stat cards
        const statCards = document.querySelectorAll('.stat-card');
        statCards.forEach(card => {
            card.addEventListener('mouseenter', () => {
                card.style.transform = 'translateY(-4px)';
            });
            card.addEventListener('mouseleave', () => {
                card.style.transform = 'translateY(0)';
            });
        });
    }

    validateInput(input) {
        const value = input.value.trim();
        const type = input.type;
        
        if (type === 'email') {
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (value && !emailRegex.test(value)) {
                this.showInputError(input, 'Please enter a valid email address');
                return false;
            }
        } else if (input.required && !value) {
            this.showInputError(input, 'This field is required');
            return false;
        }
        
        this.clearInputError(input);
        return true;
    }

    validateFormField(field) {
        const value = field.value.trim();
        const type = field.type;
        const name = field.name || field.id;
        
        if (field.required && !value) {
            this.showFieldError(field, 'This field is required');
            return false;
        }
        
        if (type === 'number' && value) {
            const num = parseFloat(value);
            if (isNaN(num) || num < 0) {
                this.showFieldError(field, 'Please enter a valid positive number');
                return false;
            }
        }
        
        this.clearFieldError(field);
        return true;
    }

    validateIBAN(input) {
        const iban = input.value.replace(/\s/g, '').toUpperCase();
        
        if (iban.length > 0) {
            // Basic IBAN format validation
            const ibanRegex = /^[A-Z]{2}[0-9]{2}[A-Z0-9]+$/;
            if (!ibanRegex.test(iban)) {
                this.showFieldError(input, 'Invalid IBAN format');
                return false;
            }
            
            // Bulgarian IBAN specific validation
            if (iban.startsWith('BG') && iban.length !== 22) {
                this.showFieldError(input, 'Bulgarian IBAN must be 22 characters long');
                return false;
            }
        }
        
        this.clearFieldError(input);
        return true;
    }

    showInputError(input, message) {
        this.clearInputError(input);
        input.style.borderColor = 'var(--danger-500)';
        const errorDiv = document.createElement('div');
        errorDiv.className = 'input-error';
        errorDiv.textContent = message;
        errorDiv.style.color = 'var(--danger-500)';
        errorDiv.style.fontSize = '0.75rem';
        errorDiv.style.marginTop = '0.25rem';
        input.parentNode.appendChild(errorDiv);
    }

    showFieldError(field, message) {
        this.clearFieldError(field);
        field.style.borderColor = 'var(--danger-500)';
        const errorDiv = document.createElement('div');
        errorDiv.className = 'field-error';
        errorDiv.textContent = message;
        errorDiv.style.color = 'var(--danger-500)';
        errorDiv.style.fontSize = '0.75rem';
        errorDiv.style.marginTop = '0.25rem';
        field.parentNode.appendChild(errorDiv);
    }

    clearInputError(input) {
        input.style.borderColor = '';
        const errorDiv = input.parentNode.querySelector('.input-error');
        if (errorDiv) {
            errorDiv.remove();
        }
    }

    clearFieldError(field) {
        field.style.borderColor = '';
        const errorDiv = field.parentNode.querySelector('.field-error');
        if (errorDiv) {
            errorDiv.remove();
        }
    }

    handleKeyboardShortcuts(e) {
        // Ctrl+Enter to submit forms
        if (e.ctrlKey && e.key === 'Enter') {
            const activeForm = document.activeElement.closest('form');
            if (activeForm) {
                e.preventDefault();
                activeForm.dispatchEvent(new Event('submit'));
            }
        }
        
        // Escape to close messages
        if (e.key === 'Escape') {
            this.clearAllMessages();
        }
    }

    async handleLogin(e) {
        e.preventDefault();
        
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;

        // Validate inputs
        if (!this.validateInput(document.getElementById('username')) || 
            !this.validateInput(document.getElementById('password'))) {
            return;
        }

        try {
            this.showLoading(true, 'Authenticating...');
            
            const response = await fetch(`${this.apiBaseUrl}/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password })
            });

            const data = await response.json();

            if (response.ok) {
                this.authToken = data.token;
                localStorage.setItem('authToken', this.authToken);
                this.showMessage('Login successful! Welcome to Fraud Shield', 'success');
                this.updateAuthUI(true, data.user);
                this.showDashboard();
                this.animateLoginSuccess();
            } else {
                this.showMessage(data.error || 'Login failed. Please check your credentials.', 'error');
                this.animateLoginError();
            }
        } catch (error) {
            this.showMessage('Network error: Unable to connect to server', 'error');
            console.error('Login error:', error);
        } finally {
            this.showLoading(false);
        }
    }

    handleLogout() {
        this.authToken = null;
        localStorage.removeItem('authToken');
        this.updateAuthUI(false);
        this.showLoginRequired();
        this.showMessage('Logged out successfully', 'info');
        this.animateLogout();
    }

    async handlePaymentValidation(e) {
        e.preventDefault();
        
        if (!this.authToken) {
            this.showMessage('Please login first', 'error');
            return;
        }

        // Validate all form fields
        const form = e.target;
        const inputs = form.querySelectorAll('input[required], select[required]');
        let isValid = true;
        
        inputs.forEach(input => {
            if (!this.validateFormField(input)) {
                isValid = false;
            }
        });

        if (!isValid) {
            this.showMessage('Please fix the form errors before submitting', 'error');
            return;
        }

        const formData = {
            supplierIban: document.getElementById('supplierIban').value.trim(),
            invoiceId: document.getElementById('invoiceId').value.trim(),
            supplierName: document.getElementById('supplierName').value.trim(),
            paymentAmount: parseFloat(document.getElementById('paymentAmount').value),
            currency: document.getElementById('currency').value,
            invoiceNumber: document.getElementById('invoiceNumber').value.trim(),
            supplierReference: document.getElementById('supplierReference').value.trim()
        };

        try {
            this.showLoading(true, 'Analyzing payment for fraud...');
            const validateBtn = document.getElementById('validateBtn');
            validateBtn.disabled = true;
            validateBtn.innerHTML = `
                <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                Validating...
            `;

            const startTime = performance.now();
            const response = await fetch(`${this.apiBaseUrl}/v1/suppliers/validate-payment`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.authToken}`
                },
                body: JSON.stringify(formData)
            });

            const endTime = performance.now();
            const networkTime = Math.round(endTime - startTime);

            const data = await response.json();

            if (response.ok) {
                // Add network time to response time
                data.responseTimeMs = (data.responseTimeMs || 0) + networkTime;
                
                this.displayValidationResult(data);
                this.addToRecentValidations(data);
                this.updateStats(data);
                
                if (data.acceptableResponseTime) {
                    this.showMessage('Payment validation completed successfully!', 'success');
                } else {
                    this.showMessage('Validation completed but response time exceeded 200ms', 'warning');
                }
                
                this.animateValidationSuccess();
            } else {
                this.showMessage(data.error || 'Payment validation failed', 'error');
                this.animateValidationError();
            }
        } catch (error) {
            this.showMessage('Network error: Unable to validate payment', 'error');
            console.error('Validation error:', error);
        } finally {
            this.showLoading(false);
            const validateBtn = document.getElementById('validateBtn');
            validateBtn.disabled = false;
            validateBtn.innerHTML = `
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
                Validate Payment
            `;
        }
    }

    displayValidationResult(data) {
        const resultsDiv = document.getElementById('results');
        const fraudStatusSpan = document.getElementById('fraudStatus');
        const riskLevelSpan = document.getElementById('riskLevel');
        const invoiceIdSpan = document.getElementById('resultInvoiceId');
        const supplierNameSpan = document.getElementById('resultSupplierName');
        const supplierIbanSpan = document.getElementById('resultSupplierIban');
        const recommendationSpan = document.getElementById('recommendation');
        const responseTimeSpan = document.getElementById('responseTime');
        const timestampSpan = document.getElementById('timestamp');
        const anomaliesList = document.getElementById('anomaliesList');

        // Update fraud status with animation
        fraudStatusSpan.textContent = data.fraudStatus;
        fraudStatusSpan.className = `fraud-status ${data.fraudStatus}`;
        
        // Add pulse animation for high-risk results
        if (data.fraudStatus === 'HIGH_RISK' || data.fraudStatus === 'BLOCKED') {
            fraudStatusSpan.style.animation = 'pulse 2s infinite';
        } else {
            fraudStatusSpan.style.animation = '';
        }
        
        riskLevelSpan.textContent = data.riskLevel;
        invoiceIdSpan.textContent = data.invoiceId;
        supplierNameSpan.textContent = data.supplierName;
        supplierIbanSpan.textContent = data.supplierIban;
        recommendationSpan.textContent = data.recommendation;
        responseTimeSpan.textContent = data.responseTimeMs;
        timestampSpan.textContent = new Date(data.timestamp).toLocaleString();

        // Display anomalies with better formatting
        anomaliesList.innerHTML = '';
        if (data.anomalies && data.anomalies.length > 0) {
            data.anomalies.forEach((anomaly, index) => {
                const li = document.createElement('li');
                li.innerHTML = `
                    <span class="anomaly-icon">⚠️</span>
                    <span class="anomaly-text">${anomaly}</span>
                `;
                li.style.animationDelay = `${index * 0.1}s`;
                li.style.animation = 'fadeInUp 0.5s ease-out forwards';
                anomaliesList.appendChild(li);
            });
        } else {
            const li = document.createElement('li');
            li.innerHTML = `
                <span class="anomaly-icon">✅</span>
                <span class="anomaly-text no-anomalies">No anomalies detected</span>
            `;
            anomaliesList.appendChild(li);
        }

        // Show results with animation
        resultsDiv.style.display = 'block';
        resultsDiv.style.opacity = '0';
        resultsDiv.style.transform = 'translateY(20px)';
        
        setTimeout(() => {
            resultsDiv.style.transition = 'all 0.5s ease-out';
            resultsDiv.style.opacity = '1';
            resultsDiv.style.transform = 'translateY(0)';
            resultsDiv.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }, 100);
    }

    addToRecentValidations(data) {
        this.validations.unshift({
            ...data,
            timestamp: new Date(data.timestamp)
        });

        // Keep only last 10 validations
        if (this.validations.length > 10) {
            this.validations = this.validations.slice(0, 10);
        }

        this.renderRecentValidations();
        this.saveRecentValidations();
    }

    renderRecentValidations() {
        const recentList = document.getElementById('recentList');
        recentList.innerHTML = '';

        if (this.validations.length === 0) {
            recentList.innerHTML = `
                <div class="empty-state">
                    <svg class="w-12 h-12 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                    </svg>
                    <p class="text-gray-500 text-center">No validations yet</p>
                    <p class="text-gray-400 text-sm text-center">Submit your first payment validation to see results here</p>
                </div>
            `;
            return;
        }

        this.validations.forEach((validation, index) => {
            const item = document.createElement('div');
            item.className = `validation-item ${validation.fraudStatus}`;
            item.style.animationDelay = `${index * 0.1}s`;
            item.style.animation = 'fadeInUp 0.5s ease-out forwards';
            
            const statusColors = {
                'SAFE': 'var(--success-500)',
                'SUSPICIOUS': 'var(--warning-500)',
                'HIGH_RISK': '#f97316',
                'BLOCKED': 'var(--danger-500)'
            };
            
            item.innerHTML = `
                <div class="validation-item-header">
                    <span class="validation-item-status" style="background-color: ${statusColors[validation.fraudStatus]}">
                        ${validation.fraudStatus}
                    </span>
                    <span class="validation-item-time">${validation.timestamp.toLocaleString()}</span>
                </div>
                <div class="validation-item-details">
                    <strong>${validation.invoiceId}</strong> - ${validation.supplierName}
                    <br>
                    <small>${validation.responseTimeMs}ms - ${validation.recommendation}</small>
                </div>
            `;
            
            recentList.appendChild(item);
        });
    }

    updateStats(data) {
        this.stats.totalPayments = this.validations.length;
        
        // Calculate fraud detected
        this.stats.fraudDetected = this.validations.filter(v => 
            v.fraudStatus === 'SUSPICIOUS' || v.fraudStatus === 'HIGH_RISK' || v.fraudStatus === 'BLOCKED'
        ).length;

        // Calculate average response time
        if (this.validations.length > 0) {
            this.stats.avgResponseTime = Math.round(
                this.validations.reduce((sum, v) => sum + v.responseTimeMs, 0) / this.validations.length
            );

            // Calculate success rate (acceptable response times)
            const acceptableCount = this.validations.filter(v => v.responseTimeMs <= 200).length;
            this.stats.successRate = Math.round((acceptableCount / this.validations.length) * 100);
        }

        // Update UI with animation
        this.animateStatUpdate('totalPayments', this.stats.totalPayments);
        this.animateStatUpdate('fraudDetected', this.stats.fraudDetected);
        this.animateStatUpdate('avgResponseTime', this.stats.avgResponseTime);
        this.animateStatUpdate('successRate', this.stats.successRate);
    }

    animateStatUpdate(statId, newValue) {
        const element = document.getElementById(statId);
        if (element) {
            const currentValue = parseInt(element.textContent) || 0;
            const increment = (newValue - currentValue) / 20;
            let current = currentValue;
            
            const timer = setInterval(() => {
                current += increment;
                if ((increment > 0 && current >= newValue) || (increment < 0 && current <= newValue)) {
                    element.textContent = newValue;
                    clearInterval(timer);
                } else {
                    element.textContent = Math.round(current);
                }
            }, 50);
        }
    }

    checkAuthStatus() {
        const token = localStorage.getItem('authToken');
        if (token) {
            this.authToken = token;
            this.validateToken();
        } else {
            this.showLoginRequired();
        }
    }

    async validateToken() {
        try {
            const response = await fetch(`${this.apiBaseUrl}/auth/validate`, {
                headers: {
                    'Authorization': `Bearer ${this.authToken}`
                }
            });

            const data = await response.json();

            if (response.ok && data.valid) {
                this.updateAuthUI(true, { username: 'admin' });
                this.showDashboard();
            } else {
                this.handleLogout();
            }
        } catch (error) {
            console.error('Token validation error:', error);
            this.handleLogout();
        }
    }

    updateAuthUI(isLoggedIn, user = null) {
        const loginForm = document.getElementById('loginForm');
        const userInfo = document.getElementById('userInfo');
        const welcomeMessage = document.getElementById('welcomeMessage');

        if (isLoggedIn) {
            loginForm.style.display = 'none';
            userInfo.style.display = 'flex';
            if (welcomeMessage && user) {
                welcomeMessage.textContent = `Welcome, ${user.username}!`;
            }
        } else {
            loginForm.style.display = 'block';
            userInfo.style.display = 'none';
        }
    }

    showDashboard() {
        const dashboard = document.getElementById('dashboard');
        const loginRequired = document.getElementById('loginRequired');
        
        dashboard.style.display = 'block';
        loginRequired.style.display = 'none';
        
        // Animate dashboard appearance
        dashboard.style.opacity = '0';
        dashboard.style.transform = 'translateY(20px)';
        setTimeout(() => {
            dashboard.style.transition = 'all 0.5s ease-out';
            dashboard.style.opacity = '1';
            dashboard.style.transform = 'translateY(0)';
        }, 100);
    }

    showLoginRequired() {
        const dashboard = document.getElementById('dashboard');
        const loginRequired = document.getElementById('loginRequired');
        
        dashboard.style.display = 'none';
        loginRequired.style.display = 'block';
        
        // Animate login required appearance
        loginRequired.style.opacity = '0';
        loginRequired.style.transform = 'translateY(20px)';
        setTimeout(() => {
            loginRequired.style.transition = 'all 0.5s ease-out';
            loginRequired.style.opacity = '1';
            loginRequired.style.transform = 'translateY(0)';
        }, 100);
    }

    showLoading(show, message = 'Loading...') {
        const overlay = document.getElementById('loadingOverlay');
        const loadingText = overlay.querySelector('p');
        
        if (loadingText) {
            loadingText.textContent = message;
        }
        
        overlay.style.display = show ? 'flex' : 'none';
        
        if (show) {
            overlay.style.opacity = '0';
            setTimeout(() => {
                overlay.style.transition = 'opacity 0.3s ease-out';
                overlay.style.opacity = '1';
            }, 10);
        }
    }

    showMessage(message, type = 'info') {
        const container = document.getElementById('messageContainer');
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        messageDiv.innerHTML = `
            <div class="flex items-center">
                <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    ${this.getMessageIcon(type)}
                </svg>
                <span>${message}</span>
            </div>
        `;

        container.appendChild(messageDiv);

        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (messageDiv.parentNode) {
                messageDiv.style.transition = 'all 0.3s ease-out';
                messageDiv.style.transform = 'translateX(100%)';
                messageDiv.style.opacity = '0';
                setTimeout(() => {
                    if (messageDiv.parentNode) {
                        messageDiv.parentNode.removeChild(messageDiv);
                    }
                }, 300);
            }
        }, 5000);
    }

    getMessageIcon(type) {
        const icons = {
            success: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>',
            error: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>',
            warning: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z"></path>',
            info: '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>'
        };
        return icons[type] || icons.info;
    }

    clearAllMessages() {
        const container = document.getElementById('messageContainer');
        container.innerHTML = '';
    }

    loadRecentValidations() {
        const saved = localStorage.getItem('recentValidations');
        if (saved) {
            try {
                this.validations = JSON.parse(saved).map(v => ({
                    ...v,
                    timestamp: new Date(v.timestamp)
                }));
                this.renderRecentValidations();
                this.updateStats({});
            } catch (error) {
                console.error('Error loading recent validations:', error);
            }
        }
    }

    saveRecentValidations() {
        localStorage.setItem('recentValidations', JSON.stringify(this.validations));
    }

    // Animation methods
    animateLoginSuccess() {
        const loginForm = document.getElementById('loginForm');
        loginForm.style.transform = 'scale(0.95)';
        loginForm.style.transition = 'transform 0.2s ease-out';
        setTimeout(() => {
            loginForm.style.transform = 'scale(1)';
        }, 200);
    }

    animateLoginError() {
        const loginForm = document.getElementById('loginForm');
        loginForm.style.animation = 'shake 0.5s ease-in-out';
        setTimeout(() => {
            loginForm.style.animation = '';
        }, 500);
    }

    animateLogout() {
        const userInfo = document.getElementById('userInfo');
        userInfo.style.animation = 'fadeOut 0.3s ease-out';
    }

    animateValidationSuccess() {
        const validateBtn = document.getElementById('validateBtn');
        validateBtn.style.animation = 'pulse 0.5s ease-out';
        setTimeout(() => {
            validateBtn.style.animation = '';
        }, 500);
    }

    animateValidationError() {
        const validateBtn = document.getElementById('validateBtn');
        validateBtn.style.animation = 'shake 0.5s ease-in-out';
        setTimeout(() => {
            validateBtn.style.animation = '';
        }, 500);
    }
}

// Add CSS animations
const style = document.createElement('style');
style.textContent = `
    @keyframes shake {
        0%, 100% { transform: translateX(0); }
        25% { transform: translateX(-5px); }
        75% { transform: translateX(5px); }
    }
    
    @keyframes pulse {
        0%, 100% { transform: scale(1); }
        50% { transform: scale(1.05); }
    }
    
    @keyframes fadeInUp {
        from {
            opacity: 0;
            transform: translateY(20px);
        }
        to {
            opacity: 1;
            transform: translateY(0);
        }
    }
    
    @keyframes fadeOut {
        from {
            opacity: 1;
            transform: scale(1);
        }
        to {
            opacity: 0;
            transform: scale(0.95);
        }
    }
    
    .anomaly-icon {
        margin-right: 0.5rem;
    }
    
    .anomaly-text {
        font-size: 0.875rem;
    }
    
    .empty-state {
        text-align: center;
        padding: 2rem;
        color: var(--gray-500);
    }
    
    .input-error, .field-error {
        display: block;
        margin-top: 0.25rem;
        font-size: 0.75rem;
        color: var(--danger-500);
    }
`;
document.head.appendChild(style);

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.supplierFraudApp = new SupplierFraudDetectionApp();
    
    // Save validations periodically
    setInterval(() => {
        if (window.supplierFraudApp) {
            window.supplierFraudApp.saveRecentValidations();
        }
    }, 30000); // Every 30 seconds
});

// Handle page unload
window.addEventListener('beforeunload', () => {
    if (window.supplierFraudApp) {
        window.supplierFraudApp.saveRecentValidations();
    }
});