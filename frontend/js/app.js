// Instant Invoice: Fraud Shield - Supplier Payment Fraud Detection
class SupplierFraudDetectionApp {
    constructor() {
        this.apiBaseUrl = '/api';
        this.authToken = null;
        this.validations = [];
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.checkAuthStatus();
        this.loadRecentValidations();
    }

    setupEventListeners() {
        // Login form
        const loginForm = document.getElementById('loginFormElement');
        if (loginForm) {
            loginForm.addEventListener('submit', (e) => this.handleLogin(e));
        }

        // Logout button
        const logoutBtn = document.getElementById('logoutBtn');
        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => this.handleLogout());
        }

        // Payment validation form
        const validationForm = document.getElementById('paymentValidationForm');
        if (validationForm) {
            validationForm.addEventListener('submit', (e) => this.handlePaymentValidation(e));
        }
    }

    async handleLogin(e) {
        e.preventDefault();
        
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;

        try {
            this.showLoading(true);
            
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
                this.showMessage('Login successful!', 'success');
                this.updateAuthUI(true, data.user);
                this.showDashboard();
            } else {
                this.showMessage(data.error || 'Login failed', 'error');
            }
        } catch (error) {
            this.showMessage('Network error: ' + error.message, 'error');
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
    }

    async handlePaymentValidation(e) {
        e.preventDefault();
        
        if (!this.authToken) {
            this.showMessage('Please login first', 'error');
            return;
        }

        const formData = {
            supplierIban: document.getElementById('supplierIban').value,
            invoiceId: document.getElementById('invoiceId').value,
            supplierName: document.getElementById('supplierName').value,
            paymentAmount: parseFloat(document.getElementById('paymentAmount').value),
            currency: document.getElementById('currency').value,
            invoiceNumber: document.getElementById('invoiceNumber').value,
            supplierReference: document.getElementById('supplierReference').value
        };

        try {
            this.showLoading(true);
            const validateBtn = document.getElementById('validateBtn');
            validateBtn.disabled = true;

            const response = await fetch(`${this.apiBaseUrl}/v1/suppliers/payment-validation`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.authToken}`
                },
                body: JSON.stringify(formData)
            });

            const data = await response.json();

            if (response.ok) {
                this.displayValidationResult(data);
                this.addToRecentValidations(data);
                this.updateStats(data);
                
                if (data.acceptableResponseTime) {
                    this.showMessage('Payment validation completed successfully!', 'success');
                } else {
                    this.showMessage('Validation completed but response time exceeded 200ms', 'info');
                }
            } else {
                this.showMessage(data.error || 'Payment validation failed', 'error');
            }
        } catch (error) {
            this.showMessage('Network error: ' + error.message, 'error');
        } finally {
            this.showLoading(false);
            const validateBtn = document.getElementById('validateBtn');
            validateBtn.disabled = false;
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

        fraudStatusSpan.textContent = data.fraudStatus;
        fraudStatusSpan.className = `fraud-status ${data.fraudStatus}`;
        
        riskLevelSpan.textContent = data.riskLevel;
        invoiceIdSpan.textContent = data.invoiceId;
        supplierNameSpan.textContent = data.supplierName;
        supplierIbanSpan.textContent = data.supplierIban;
        recommendationSpan.textContent = data.recommendation;
        responseTimeSpan.textContent = data.responseTimeMs;
        timestampSpan.textContent = new Date(data.timestamp).toLocaleString();

        // Display anomalies
        anomaliesList.innerHTML = '';
        if (data.anomalies && data.anomalies.length > 0) {
            data.anomalies.forEach(anomaly => {
                const li = document.createElement('li');
                li.textContent = anomaly;
                anomaliesList.appendChild(li);
            });
        } else {
            const li = document.createElement('li');
            li.textContent = 'No anomalies detected';
            li.className = 'no-anomalies';
            anomaliesList.appendChild(li);
        }

        resultsDiv.style.display = 'block';
        resultsDiv.scrollIntoView({ behavior: 'smooth' });
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
    }

    renderRecentValidations() {
        const recentList = document.getElementById('recentList');
        recentList.innerHTML = '';

        this.validations.forEach(validation => {
            const item = document.createElement('div');
            item.className = `validation-item ${validation.fraudStatus}`;
            
            item.innerHTML = `
                <div class="validation-item-header">
                    <span class="validation-item-status ${validation.fraudStatus}">${validation.fraudStatus}</span>
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
        // Update total payments
        const totalElement = document.getElementById('totalPayments');
        if (totalElement) {
            totalElement.textContent = this.validations.length;
        }

        // Calculate fraud detected
        const fraudCount = this.validations.filter(v => 
            v.fraudStatus === 'SUSPICIOUS' || v.fraudStatus === 'HIGH_RISK' || v.fraudStatus === 'BLOCKED'
        ).length;
        const fraudElement = document.getElementById('fraudDetected');
        if (fraudElement) {
            fraudElement.textContent = fraudCount;
        }

        // Calculate average response time
        if (this.validations.length > 0) {
            const avgResponseTime = this.validations.reduce((sum, v) => sum + v.responseTimeMs, 0) / this.validations.length;
            const avgElement = document.getElementById('avgResponseTime');
            if (avgElement) {
                avgElement.textContent = Math.round(avgResponseTime);
            }

            // Calculate success rate (acceptable response times)
            const acceptableCount = this.validations.filter(v => v.responseTimeMs <= 200).length;
            const successRate = (acceptableCount / this.validations.length) * 100;
            const successElement = document.getElementById('successRate');
            if (successElement) {
                successElement.textContent = Math.round(successRate);
            }
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
        document.getElementById('dashboard').style.display = 'block';
        document.getElementById('loginRequired').style.display = 'none';
    }

    showLoginRequired() {
        document.getElementById('dashboard').style.display = 'none';
        document.getElementById('loginRequired').style.display = 'block';
    }

    showLoading(show) {
        const overlay = document.getElementById('loadingOverlay');
        overlay.style.display = show ? 'flex' : 'none';
    }

    showMessage(message, type = 'info') {
        const container = document.getElementById('messageContainer');
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${type}`;
        messageDiv.textContent = message;

        container.appendChild(messageDiv);

        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (messageDiv.parentNode) {
                messageDiv.parentNode.removeChild(messageDiv);
            }
        }, 5000);
    }

    loadRecentValidations() {
        // Load from localStorage if available
        const saved = localStorage.getItem('recentValidations');
        if (saved) {
            try {
                this.validations = JSON.parse(saved).map(v => ({
                    ...v,
                    timestamp: new Date(v.timestamp)
                }));
                this.renderRecentValidations();
            } catch (error) {
                console.error('Error loading recent validations:', error);
            }
        }
    }

    saveRecentValidations() {
        localStorage.setItem('recentValidations', JSON.stringify(this.validations));
    }
}

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