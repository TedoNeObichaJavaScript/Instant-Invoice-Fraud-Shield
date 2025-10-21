// Instant Invoice: Fraud Shield - Payment Generation and Validation System
class PaymentFraudDetectionApp {
    constructor() {
        this.apiBaseUrl = '/api';
        this.authToken = null;
        this.currentUser = null;
        this.currentPayment = null;
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

        // Payment generation and validation buttons
        const generateBtn = document.getElementById('generatePaymentBtn');
        const validateBtn = document.getElementById('validatePaymentBtn');
        const fraudCheckBtn = document.getElementById('fraudCheckBtn');
        const unvalidateBtn = document.getElementById('unvalidatePaymentBtn');

        if (generateBtn) {
            generateBtn.addEventListener('click', () => this.generatePayment());
        }
        if (validateBtn) {
            validateBtn.addEventListener('click', () => this.validatePayment());
        }
        if (fraudCheckBtn) {
            fraudCheckBtn.addEventListener('click', () => this.performFraudCheck());
        }
        if (unvalidateBtn) {
            unvalidateBtn.addEventListener('click', () => this.unvalidatePayment());
        }
    }

    async handleLogin(e) {
        e.preventDefault();
        
        const username = document.getElementById('username').value;
        const password = document.getElementById('password').value;
        
        try {
            const response = await fetch(`${this.apiBaseUrl}/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ username, password })
            });

            if (response.ok) {
                const data = await response.json();
                this.authToken = data.token;
                this.currentUser = username;
                
                // Save to localStorage
                localStorage.setItem('fraudShieldToken', data.token);
                localStorage.setItem('fraudShieldUser', username);
                
                this.showDashboard();
                this.showMessage('Login successful!', 'success');
            } else {
                const error = await response.json();
                this.showMessage(error.message || 'Login failed', 'error');
            }
        } catch (error) {
            this.showMessage('Network error. Please try again.', 'error');
        }
    }

    handleLogout() {
        this.authToken = null;
        this.currentUser = null;
        this.currentPayment = null;
        
        // Clear localStorage
        localStorage.removeItem('fraudShieldToken');
        localStorage.removeItem('fraudShieldUser');
        
        this.hideDashboard();
        this.showMessage('Logged out successfully', 'info');
        this.resetButtons();
    }

    checkAuthStatus() {
        // Check if user is already logged in (in a real app, this would check localStorage or cookies)
        // For demo purposes, we'll start logged out
        const savedToken = localStorage.getItem('fraudShieldToken');
        if (savedToken) {
            this.authToken = savedToken;
            this.currentUser = localStorage.getItem('fraudShieldUser') || 'User';
            this.showDashboard();
        } else {
            this.hideDashboard();
        }
    }

    showDashboard() {
        document.getElementById('loginForm').style.display = 'none';
        document.getElementById('userInfo').style.display = 'block';
        document.getElementById('dashboard').style.display = 'block';
        document.getElementById('welcomeMessage').textContent = `Welcome, ${this.currentUser || 'User'}`;
        
        // Show footer on dashboard
        const footer = document.querySelector('.footer');
        if (footer) {
            footer.style.display = 'block';
        }
        
        // Allow vertical scrolling for dashboard, prevent horizontal
        document.body.style.overflow = 'auto';
        document.body.style.overflowX = 'hidden';
        document.documentElement.style.overflow = 'auto';
        document.documentElement.style.overflowX = 'hidden';
    }

    hideDashboard() {
        document.getElementById('loginForm').style.display = 'block';
        document.getElementById('userInfo').style.display = 'none';
        document.getElementById('dashboard').style.display = 'none';
        document.getElementById('generatedPayment').style.display = 'none';
        document.getElementById('results').style.display = 'none';
        
        // Hide footer on login page
        const footer = document.querySelector('.footer');
        if (footer) {
            footer.style.display = 'none';
        }
        
        // Prevent scrolling for login page
        document.body.style.overflow = 'hidden';
        document.body.style.overflowX = 'hidden';
        document.documentElement.style.overflow = 'hidden';
        document.documentElement.style.overflowX = 'hidden';
    }

    generatePayment() {
        // Generate a random payment with 33% distribution for risk levels
        const riskLevels = ['LOW', 'MEDIUM', 'HIGH'];
        const randomRisk = riskLevels[Math.floor(Math.random() * riskLevels.length)];
        
        // Generate random Bulgarian IBANs
        const ibans = this.generateRandomIBANs(2);
        
        this.currentPayment = {
            invoiceId: `INV-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
            amount: parseFloat((Math.random() * 50000 + 100).toFixed(2)),
            supplierIban: ibans[0],
            supplierName: this.generateRandomSupplierName(),
            supplierCountry: 'Bulgaria',
            paymentPurpose: this.generateRandomPurpose(),
            riskLevel: randomRisk,
            generatedAt: new Date().toISOString()
        };

        this.displayGeneratedPayment();
        this.enableButtons(['validatePaymentBtn', 'fraudCheckBtn', 'unvalidatePaymentBtn']);
        this.showMessage('Payment generated successfully!', 'success');
    }

    generateRandomIBANs(count) {
        const bankCodes = ['BANK', 'BNBG', 'CITI', 'UNCR', 'UBBS', 'DSK', 'FIB', 'PIR', 'POST', 'SGB'];
        const ibans = [];
        
        for (let i = 0; i < count; i++) {
            const bankCode = bankCodes[Math.floor(Math.random() * bankCodes.length)];
            const accountNumber = Math.floor(Math.random() * 100000000).toString().padStart(8, '0');
            const additionalDigits = Math.floor(Math.random() * 100).toString().padStart(2, '0');
            const checkDigits = Math.floor(Math.random() * 100).toString().padStart(2, '0');
            
            ibans.push(`BG${checkDigits}${bankCode}${accountNumber}${additionalDigits}`);
        }
        
        return ibans;
    }

    generateRandomSupplierName() {
        const companies = [
            'ACME Corporation Ltd', 'Global Solutions Inc', 'Tech Innovations Ltd',
            'Business Partners Co', 'Advanced Systems Ltd', 'Creative Solutions Inc',
            'Dynamic Enterprises Ltd', 'Future Technologies Co', 'Smart Business Ltd',
            'Professional Services Inc'
        ];
        return companies[Math.floor(Math.random() * companies.length)];
    }

    generateRandomPurpose() {
        const purposes = [
            'Invoice payment', 'Service fee', 'Product delivery',
            'Consulting services', 'Software license', 'Maintenance contract',
            'Training services', 'Equipment purchase', 'Project milestone',
            'Monthly subscription'
        ];
        return purposes[Math.floor(Math.random() * purposes.length)];
    }

    displayGeneratedPayment() {
        document.getElementById('genInvoiceId').textContent = this.currentPayment.invoiceId;
        document.getElementById('genAmount').textContent = `${this.currentPayment.amount} BGN`;
        document.getElementById('genSupplierIban').textContent = this.currentPayment.supplierIban;
        document.getElementById('genSupplierName').textContent = this.currentPayment.supplierName;
        document.getElementById('genSupplierCountry').textContent = this.currentPayment.supplierCountry;
        document.getElementById('genPaymentPurpose').textContent = this.currentPayment.paymentPurpose;
        
        document.getElementById('generatedPayment').style.display = 'block';
    }

    async validatePayment() {
        if (!this.currentPayment) {
            this.showMessage('No payment to validate. Please generate a payment first.', 'error');
            return;
        }

        try {
            const startTime = Date.now();
            
            // Simulate payment validation (in real app, this would call the API)
            const validationResult = await this.simulatePaymentValidation();
            
            const responseTime = Date.now() - startTime;
            
            this.showMessage(`Payment validated successfully in ${responseTime}ms`, 'success');
            this.updateStats(responseTime, true);
            
        } catch (error) {
            this.showMessage('Payment validation failed', 'error');
        }
    }

    async performFraudCheck() {
        if (!this.currentPayment) {
            this.showMessage('No payment to check. Please generate a payment first.', 'error');
            return;
        }

        if (!this.authToken) {
            this.showMessage('Please login first', 'error');
            return;
        }

        try {
            const startTime = Date.now();
            
            console.log('Making fraud check request to:', `${this.apiBaseUrl}/v1/fraud-detection/validate-payment`);
            console.log('Request payload:', this.currentPayment);
            console.log('Auth token:', this.authToken ? 'Present' : 'Missing');
            
            const response = await fetch(`${this.apiBaseUrl}/v1/fraud-detection/validate-payment`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.authToken}`
                },
                body: JSON.stringify(this.currentPayment)
            });

            const responseTime = Date.now() - startTime;
            
            if (response.ok) {
                const result = await response.json();
                this.displayFraudResults(result, responseTime);
                this.updateStats(responseTime, true);
                
                // Save to recent validations
                this.saveValidation({
                    ...this.currentPayment,
                    result: result,
                    responseTime: responseTime,
                    timestamp: new Date().toISOString()
                });
                
                this.loadRecentValidations();
            } else {
                const error = await response.json();
                this.showMessage(`Fraud check failed: ${error.reason || 'Unknown error'}`, 'error');
                this.updateStats(responseTime, false);
            }
            
        } catch (error) {
            console.error('Fraud check error details:', error);
            console.error('Error message:', error.message);
            console.error('Error stack:', error.stack);
            this.showMessage(`Network error during fraud check: ${error.message}`, 'error');
        }
    }

    unvalidatePayment() {
        this.currentPayment = null;
        document.getElementById('generatedPayment').style.display = 'none';
        document.getElementById('results').style.display = 'none';
        this.resetButtons();
        this.showMessage('Payment unvalidated and cleared', 'info');
    }

    async simulatePaymentValidation() {
        // Simulate API call delay
        await new Promise(resolve => setTimeout(resolve, Math.random() * 100 + 50));
        
        // Simulate validation result based on risk level
        const riskLevel = this.currentPayment.riskLevel;
        return {
            valid: riskLevel !== 'HIGH',
            riskLevel: riskLevel,
            message: riskLevel === 'HIGH' ? 'High risk payment detected' : 'Payment appears valid'
        };
    }

    displayFraudResults(result, responseTime) {
        const resultsDiv = document.getElementById('results');
        
        // Update result elements
        document.getElementById('riskStatus').textContent = result.riskStatus || 'UNKNOWN';
        document.getElementById('riskLevel').textContent = result.riskLevel || 'UNKNOWN';
        document.getElementById('resultInvoiceId').textContent = result.invoiceId || this.currentPayment.invoiceId;
        document.getElementById('resultSupplierName').textContent = result.supplierName || this.currentPayment.supplierName;
        document.getElementById('resultSupplierIban').textContent = result.supplierIban || this.currentPayment.supplierIban;
        document.getElementById('resultAmount').textContent = `${result.amount || this.currentPayment.amount} BGN`;
        document.getElementById('reason').textContent = result.reason || 'No specific reason provided';
        document.getElementById('transactionId').textContent = result.transactionId || 'N/A';
        document.getElementById('manualReview').textContent = result.requiresManualReview ? 'Yes' : 'No';
        
        // Add response time to risk status text if element exists
        const riskStatusText = document.getElementById('riskStatusText');
        if (riskStatusText) {
            riskStatusText.textContent = `${result.riskStatus || 'UNKNOWN'} (${responseTime}ms)`;
        }
        
        // Show results
        if (resultsDiv) {
            resultsDiv.style.display = 'block';
        }
        
        // Update anomalies
        const anomaliesList = document.getElementById('anomaliesList');
        if (anomaliesList) {
            anomaliesList.innerHTML = '';
            if (result.anomalies && result.anomalies.length > 0) {
                result.anomalies.forEach(anomaly => {
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
        }
        
        // Update risk status styling
        const riskStatusElement = document.getElementById('riskStatus');
        riskStatusElement.className = `fraud-status ${(result.riskStatus || '').toLowerCase()}`;
        
        resultsDiv.style.display = 'block';
        this.showMessage(`Fraud analysis completed in ${responseTime}ms`, 'success');
    }

    enableButtons(buttonIds) {
        buttonIds.forEach(id => {
            const btn = document.getElementById(id);
            if (btn) {
                btn.disabled = false;
                btn.classList.remove('disabled');
            }
        });
    }

    resetButtons() {
        const buttons = ['validatePaymentBtn', 'fraudCheckBtn', 'unvalidatePaymentBtn'];
        buttons.forEach(id => {
            const btn = document.getElementById(id);
            if (btn) {
                btn.disabled = true;
                btn.classList.add('disabled');
            }
        });
    }

    updateStats(responseTime, success) {
        this.stats.totalPayments++;
        if (!success) this.stats.fraudDetected++;
        
        // Update average response time
        this.stats.avgResponseTime = Math.round(
            (this.stats.avgResponseTime * (this.stats.totalPayments - 1) + responseTime) / this.stats.totalPayments
        );
        
        // Update success rate
        this.stats.successRate = Math.round(
            ((this.stats.totalPayments - this.stats.fraudDetected) / this.stats.totalPayments) * 100
        );
        
        this.updateStatsDisplay();
    }

    updateStatsDisplay() {
        document.getElementById('totalPayments').textContent = this.stats.totalPayments;
        document.getElementById('fraudDetected').textContent = this.stats.fraudDetected;
        document.getElementById('avgResponseTime').textContent = `${this.stats.avgResponseTime}ms`;
        document.getElementById('successRate').textContent = `${this.stats.successRate}%`;
    }

    loadRecentValidations() {
        // Load recent validations from localStorage or API
        const saved = localStorage.getItem('fraudShieldValidations');
        if (saved) {
            this.validations = JSON.parse(saved);
        }
        
        // Update the recent validations display
        this.updateRecentValidationsDisplay();
    }
    
    updateRecentValidationsDisplay() {
        const recentValidationsDiv = document.getElementById('recentValidations');
        if (!recentValidationsDiv) return;
        
        if (this.validations.length === 0) {
            recentValidationsDiv.innerHTML = '<p class="no-validations">No recent validations</p>';
            return;
        }
        
        const validationsHtml = this.validations.slice(0, 5).map(validation => `
            <div class="validation-item">
                <div class="validation-header">
                    <span class="invoice-id">${validation.invoiceId}</span>
                    <span class="risk-status ${(validation.result?.riskStatus || 'UNKNOWN').toLowerCase()}">
                        ${validation.result?.riskStatus || 'UNKNOWN'}
                    </span>
                </div>
                <div class="validation-details">
                    <span class="supplier">${validation.supplierName}</span>
                    <span class="amount">${validation.amount} BGN</span>
                    <span class="time">${validation.responseTime}ms</span>
                </div>
                <div class="validation-timestamp">
                    ${new Date(validation.timestamp).toLocaleString()}
                </div>
            </div>
        `).join('');
        
        recentValidationsDiv.innerHTML = validationsHtml;
    }

    saveValidation(validation) {
        this.validations.unshift(validation);
        if (this.validations.length > 10) {
            this.validations = this.validations.slice(0, 10);
        }
        localStorage.setItem('fraudShieldValidations', JSON.stringify(this.validations));
    }

    showMessage(message, type = 'info') {
        // Create or update message element
        let messageDiv = document.getElementById('messageDiv');
        if (!messageDiv) {
            messageDiv = document.createElement('div');
            messageDiv.id = 'messageDiv';
            messageDiv.className = 'message';
            document.body.appendChild(messageDiv);
        }
        
        messageDiv.textContent = message;
        messageDiv.className = `message message-${type}`;
        messageDiv.style.display = 'block';
        
        // Auto-hide after 3 seconds
        setTimeout(() => {
            messageDiv.style.display = 'none';
        }, 3000);
    }

    setupAnimations() {
        // Add smooth animations for better UX
        const style = document.createElement('style');
        style.textContent = `
            .message {
                position: fixed;
                top: 20px;
                right: 20px;
                padding: 12px 20px;
                border-radius: 6px;
                color: white;
                font-weight: 500;
                z-index: 1000;
                transition: all 0.3s ease;
            }
            .message-success { background-color: #10b981; }
            .message-error { background-color: #ef4444; }
            .message-warning { background-color: #f59e0b; }
            .message-info { background-color: #3b82f6; }
            
            .btn-primary { background-color: #3b82f6; color: white; }
            .btn-secondary { background-color: #6b7280; color: white; }
            .btn-warning { background-color: #f59e0b; color: white; }
            .btn-danger { background-color: #ef4444; color: white; }
            
            .btn-primary:hover { background-color: #2563eb; }
            .btn-secondary:hover { background-color: #4b5563; }
            .btn-warning:hover { background-color: #d97706; }
            .btn-danger:hover { background-color: #dc2626; }
            
            .disabled {
                opacity: 0.5;
                cursor: not-allowed;
            }
            
            .generated-payment {
                background: #f8fafc;
                border: 1px solid #e2e8f0;
                border-radius: 8px;
                padding: 20px;
                margin: 20px 0;
            }
            
            .payment-card h4 {
                margin: 0 0 15px 0;
                color: #1e293b;
            }
            
            .payment-details {
                display: grid;
                gap: 10px;
            }
            
            .payment-row {
                display: flex;
                justify-content: space-between;
                padding: 8px 0;
                border-bottom: 1px solid #e2e8f0;
            }
            
            .payment-row:last-child {
                border-bottom: none;
            }
            
            .payment-row .label {
                font-weight: 600;
                color: #64748b;
            }
            
            .payment-row .value {
                color: #1e293b;
                font-family: monospace;
            }
        `;
        document.head.appendChild(style);
    }
}

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    new PaymentFraudDetectionApp();
});