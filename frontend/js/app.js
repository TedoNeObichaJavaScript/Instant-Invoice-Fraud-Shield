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
        // For testing purposes, always show dashboard (authentication disabled)
        this.authToken = 'test-token'; // Set a dummy token for testing
        this.currentUser = 'Test User';
        this.showDashboard();
        
        // Check if user is already logged in (in a real app, this would check localStorage or cookies)
        // For demo purposes, we'll start logged out
        const savedToken = localStorage.getItem('fraudShieldToken');
        if (savedToken) {
            this.authToken = savedToken;
            this.currentUser = localStorage.getItem('fraudShieldUser') || 'User';
            this.showDashboard();
        } else {
            // For testing, show dashboard even without token
            this.showDashboard();
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

    async generatePayment() {
        try {
            // Get random IBANs from database
            const ibans = await this.getRandomIBANsFromDatabase(2);
            
            this.currentPayment = {
                invoiceId: `INV-${Date.now()}-${Math.floor(Math.random() * 1000)}`,
                amount: parseFloat((Math.random() * 50000 + 100).toFixed(2)),
                supplierIban: ibans[0],
                supplierName: this.generateRandomSupplierName(),
                supplierCountry: 'Bulgaria',
                paymentPurpose: this.generateRandomPurpose(),
                riskLevel: 'UNKNOWN', // Will be determined by fraud check
                generatedAt: new Date().toISOString()
            };

            this.displayGeneratedPayment();
            this.enableButtons(['validatePaymentBtn', 'fraudCheckBtn', 'unvalidatePaymentBtn']);
            this.showMessage('Payment generated successfully with real IBANs!', 'success');
        } catch (error) {
            console.error('Error generating payment:', error);
            this.showMessage('Using fallback IBAN generation method.', 'warning');
            
            // Fallback to random generation
            const riskLevels = ['GOOD', 'REVIEW', 'BLOCK'];
            const randomRisk = riskLevels[Math.floor(Math.random() * riskLevels.length)];
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
            this.showMessage('Payment generated with fallback method!', 'warning');
        }
    }

    async getRandomIBANsFromDatabase(count) {
        try {
            const response = await fetch(`${this.apiBaseUrl}/v1/fraud-detection/ibans/random?count=${count}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                    // Removed Authorization header for testing
                }
            });

            if (response.ok) {
                const data = await response.json();
                return data.ibans || [];
            } else {
                throw new Error('Failed to fetch IBANs from database');
            }
        } catch (error) {
            console.error('Error fetching IBANs from database:', error);
            // Fallback to random generation
            return this.generateRandomIBANs(count);
        }
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
        
        // Update IBAN status display
        this.updateIbanStatusDisplay('UNKNOWN', 'Not Checked');
        
        document.getElementById('generatedPayment').style.display = 'block';
    }

    updateIbanStatusDisplay(riskLevel, status) {
        const riskElement = document.getElementById('genIbanRiskLevel');
        const statusElement = document.getElementById('genIbanStatus');
        
        console.log('Updating IBAN status display:', { riskLevel, status });
        console.log('Risk element found:', !!riskElement);
        console.log('Status element found:', !!statusElement);
        
        if (riskElement) {
            riskElement.textContent = riskLevel;
            // Remove all existing risk classes and add the new one
            riskElement.className = riskElement.className.replace(/risk-\w+/g, '');
            riskElement.className += ` risk-${riskLevel.toLowerCase()}`;
            console.log('Updated risk element:', riskElement.textContent, riskElement.className);
        }
        
        if (statusElement) {
            statusElement.textContent = status;
            // Remove all existing status classes and add the new one
            statusElement.className = statusElement.className.replace(/status-\w+/g, '');
            statusElement.className += ` status-${status.toLowerCase().replace(/\s+/g, '-')}`;
            console.log('Updated status element:', statusElement.textContent, statusElement.className);
        }
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
            
            // Update IBAN status display based on validation result
            this.updateIbanStatusDisplay(validationResult.riskLevel, 'Validated');
            
            this.showMessage(`Payment validated successfully in ${responseTime}ms`, 'success');
            
            // Update stats with proper risk status
            const riskStatus = validationResult.valid ? 'ALLOW' : 'REVIEW';
            this.updateStats(responseTime, true, riskStatus);
            
            // Save to recent validations
            this.saveValidation({
                ...this.currentPayment,
                result: {
                    riskStatus: validationResult.valid ? 'ALLOW' : 'REVIEW',
                    riskLevel: validationResult.riskLevel,
                    reason: validationResult.message,
                    requiresManualReview: !validationResult.valid
                },
                responseTime: responseTime,
                timestamp: new Date().toISOString(),
                validationType: 'manual'
            });
            
            this.loadRecentValidations();
            
        } catch (error) {
            this.showMessage('Payment validation failed', 'error');
        }
    }

    async performFraudCheck() {
        if (!this.currentPayment) {
            this.showMessage('No payment to check. Please generate a payment first.', 'error');
            return;
        }

        try {
            const startTime = Date.now();
            
            console.log('Making fraud check request to:', `${this.apiBaseUrl}/v1/fraud-detection/validate-payment`);
            console.log('Request payload:', this.currentPayment);
            
            const response = await fetch(`${this.apiBaseUrl}/v1/fraud-detection/validate-payment`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                    // Removed Authorization header for testing
                },
                body: JSON.stringify(this.currentPayment)
            });

            const responseTime = Date.now() - startTime;
            
            if (response.ok) {
                const result = await response.json();
                this.displayFraudResults(result, responseTime);
                this.updateStats(responseTime, true, result.riskStatus);
                
                // Update IBAN status display based on fraud check result
                this.updateIbanStatusDisplay(result.riskLevel, result.riskStatus);
                
                // Save to recent validations
                this.saveValidation({
                    ...this.currentPayment,
                    result: result,
                    responseTime: responseTime,
                    timestamp: new Date().toISOString(),
                    validationType: 'fraud_check'
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
        
        // For validate payment, we need to determine the risk level based on the IBAN
        // Since we don't have the actual risk level from database, we'll simulate it
        const riskLevels = ['GOOD', 'REVIEW', 'BLOCK'];
        const randomRisk = riskLevels[Math.floor(Math.random() * riskLevels.length)];
        
        return {
            valid: randomRisk === 'GOOD',
            riskLevel: randomRisk,
            message: randomRisk === 'BLOCK' ? 'Payment blocked due to high risk' : 
                    randomRisk === 'REVIEW' ? 'Payment requires manual review' : 'Payment appears valid'
        };
    }

    displayFraudResults(result, responseTime) {
        const resultsDiv = document.getElementById('results');
        
        // Map API response to display values
        const riskStatus = result.riskStatus || 'UNKNOWN';
        const riskLevel = result.riskLevel || 'UNKNOWN';
        
        // Convert risk status to display format
        let displayStatus, displayLevel, statusClass;
        
        switch (riskStatus) {
            case 'ALLOW':
                displayStatus = 'ALLOWED';
                displayLevel = 'GOOD';
                statusClass = 'allow';
                break;
            case 'REVIEW':
                displayStatus = 'REVIEW';
                displayLevel = 'REVIEW';
                statusClass = 'review';
                break;
            case 'BLOCK':
                displayStatus = 'BLOCKED';
                displayLevel = 'BLOCK';
                statusClass = 'block';
                break;
            default:
                displayStatus = 'UNKNOWN';
                displayLevel = 'UNKNOWN';
                statusClass = 'unknown';
        }
        
        // Update result elements
        document.getElementById('riskStatus').textContent = displayStatus;
        document.getElementById('riskLevel').textContent = displayLevel;
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
            riskStatusText.textContent = `${displayStatus} (${responseTime}ms)`;
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
        
        // Update risk status styling with new classes
        const riskStatusElement = document.getElementById('riskStatus');
        riskStatusElement.className = `fraud-status ${statusClass}`;
        
        // Auto-detection and action based on risk level
        this.handleAutoDetection(riskStatus, riskLevel, result);
        
        resultsDiv.style.display = 'block';
        this.showMessage(`Fraud analysis completed in ${responseTime}ms`, 'success');
    }

    handleAutoDetection(riskStatus, riskLevel, result) {
        // Auto-detection logic based on risk level
        switch (riskStatus) {
            case 'ALLOW':
                // Payment is automatically approved
                this.showMessage('✅ Payment automatically approved - Low risk detected', 'success');
                this.updateStats(0, true, 'ALLOW'); // Update stats for successful payment
                break;
                
            case 'REVIEW':
                // Payment requires manual review
                this.showMessage('⚠️ Payment flagged for manual review - Medium risk detected', 'warning');
                this.showManualReviewPrompt(result);
                break;
                
            case 'BLOCK':
                // Payment is automatically blocked
                this.showMessage('🚫 Payment automatically blocked - High risk detected', 'error');
                this.updateStats(0, false, 'BLOCK'); // Update stats for blocked payment
                this.showBlockedPaymentDetails(result);
                break;
                
            default:
                this.showMessage('❓ Unknown risk level - Manual review required', 'warning');
        }
    }

    showManualReviewPrompt(result) {
        // Create a modal or notification for manual review
        const reviewModal = document.createElement('div');
        reviewModal.className = 'review-modal';
        reviewModal.innerHTML = `
            <div class="review-content">
                <h3>Manual Review Required</h3>
                <p>This payment has been flagged for manual review due to medium risk factors.</p>
                <div class="review-details">
                    <p><strong>IBAN:</strong> ${result.supplierIban || this.currentPayment.supplierIban}</p>
                    <p><strong>Amount:</strong> ${result.amount || this.currentPayment.amount} BGN</p>
                    <p><strong>Reason:</strong> ${result.reason || 'Medium risk detected'}</p>
                </div>
                <div class="review-actions">
                    <button class="btn-primary" onclick="this.parentElement.parentElement.parentElement.remove()">Approve</button>
                    <button class="btn-danger" onclick="this.parentElement.parentElement.parentElement.remove()">Reject</button>
                </div>
            </div>
        `;
        
        // Add modal styles
        const style = document.createElement('style');
        style.textContent = `
            .review-modal {
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(0, 0, 0, 0.5);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 1000;
            }
            .review-content {
                background: white;
                padding: 2rem;
                border-radius: 8px;
                max-width: 500px;
                box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
            }
            .review-actions {
                display: flex;
                gap: 1rem;
                margin-top: 1rem;
            }
        `;
        document.head.appendChild(style);
        document.body.appendChild(reviewModal);
    }

    showBlockedPaymentDetails(result) {
        // Show additional details for blocked payments
        const blockedDetails = document.createElement('div');
        blockedDetails.className = 'blocked-details';
        blockedDetails.innerHTML = `
            <div class="blocked-content">
                <h4>🚫 Payment Blocked</h4>
                <p>This payment has been automatically blocked due to high risk factors.</p>
                <p><strong>Reason:</strong> ${result.reason || 'High risk detected'}</p>
                <p><strong>Anomalies:</strong> ${result.anomalies ? result.anomalies.join(', ') : 'None detected'}</p>
            </div>
        `;
        
        // Add to results section
        const resultsDiv = document.getElementById('results');
        if (resultsDiv) {
            resultsDiv.appendChild(blockedDetails);
        }
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

    updateStats(responseTime, success, riskStatus = null) {
        this.stats.totalPayments++;
        
        // Increment fraud detected only for actual fraud cases (BLOCK or REVIEW)
        if (riskStatus === 'BLOCK' || riskStatus === 'REVIEW') {
            this.stats.fraudDetected++;
        }
        
        // Track blocked payments specifically
        if (riskStatus === 'BLOCK') {
            this.stats.blockedPayments = (this.stats.blockedPayments || 0) + 1;
        }
        
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
        
        // Update blocked payments if element exists
        const blockedElement = document.getElementById('blockedPayments');
        if (blockedElement) {
            blockedElement.textContent = this.stats.blockedPayments || 0;
        }
    }

    loadRecentValidations() {
        // Load recent validations from localStorage or API
        const saved = localStorage.getItem('fraudShieldValidations');
        if (saved) {
            this.validations = JSON.parse(saved);
        }
        
        // Update the recent validations display
        this.updateRecentValidationsDisplay();
        this.updateBlockedPaymentsDisplay();
    }
    
    updateRecentValidationsDisplay() {
        const recentValidationsDiv = document.getElementById('recentValidations');
        if (!recentValidationsDiv) return;
        
        if (this.validations.length === 0) {
            recentValidationsDiv.innerHTML = '<p class="no-validations">No recent validations</p>';
            return;
        }
        
        // Show all validations (increased limit from 5 to 50)
        const validationsHtml = this.validations.slice(0, 50).map(validation => {
            const riskStatus = validation.result?.riskStatus || 'UNKNOWN';
            const riskLevel = validation.result?.riskLevel || 'UNKNOWN';
            const statusClass = this.getRiskStatusClass(riskStatus);
            const validationType = validation.validationType || 'unknown';
            const typeLabel = validationType === 'fraud_check' ? 'Fraud Check' : 
                             validationType === 'manual' ? 'Validated' : 'Unknown';
            
            return `
                <div class="validation-item">
                    <div class="validation-header">
                        <span class="invoice-id">${validation.invoiceId}</span>
                        <span class="validation-type">${typeLabel}</span>
                        <span class="risk-status ${statusClass}">
                            ${this.formatRiskStatus(riskStatus)}
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
            `;
        }).join('');
        
        recentValidationsDiv.innerHTML = validationsHtml;
    }

    updateBlockedPaymentsDisplay() {
        // Create or update blocked payments section
        let blockedSection = document.getElementById('blockedPayments');
        if (!blockedSection) {
            blockedSection = document.createElement('div');
            blockedSection.id = 'blockedPayments';
            blockedSection.className = 'blocked-payments';
            blockedSection.innerHTML = `
                <div class="blocked-header">
                    <div class="blocked-icon">
                        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.732-.833-2.5 0L4.268 19.5c-.77.833.192 2.5 1.732 2.5z"></path>
                        </svg>
                    </div>
                    <h3>Blocked Payments</h3>
                </div>
                <div id="blockedPaymentsList" class="blocked-list">
                    <!-- Dynamic content will be added here -->
                </div>
            `;
            
            // Insert after recent validations
            const recentSection = document.querySelector('.recent-validations');
            if (recentSection) {
                recentSection.parentNode.insertBefore(blockedSection, recentSection.nextSibling);
            }
        }
        
        const blockedList = document.getElementById('blockedPaymentsList');
        if (!blockedList) return;
        
        // Filter blocked payments
        const blockedPayments = this.validations.filter(v => 
            v.result?.riskStatus === 'BLOCK' || v.result?.riskLevel === 'BLOCK'
        );
        
        if (blockedPayments.length === 0) {
            blockedList.innerHTML = '<p class="no-blocked">No blocked payments</p>';
            return;
        }
        
        const blockedHtml = blockedPayments.map(payment => `
            <div class="blocked-item">
                <div class="blocked-header-item">
                    <span class="invoice-id">${payment.invoiceId}</span>
                    <span class="blocked-status">BLOCKED</span>
                </div>
                <div class="blocked-details">
                    <span class="supplier">${payment.supplierName}</span>
                    <span class="iban">${payment.supplierIban}</span>
                    <span class="amount">${payment.amount} BGN</span>
                </div>
                <div class="blocked-reason">
                    <strong>Reason:</strong> ${payment.result?.reason || 'High risk detected'}
                </div>
                <div class="blocked-timestamp">
                    ${new Date(payment.timestamp).toLocaleString()}
                </div>
            </div>
        `).join('');
        
        blockedList.innerHTML = blockedHtml;
    }

    getRiskStatusClass(riskStatus) {
        switch (riskStatus) {
            case 'ALLOW': return 'allow';
            case 'REVIEW': return 'review';
            case 'BLOCK': return 'block';
            default: return 'unknown';
        }
    }

    formatRiskStatus(riskStatus) {
        switch (riskStatus) {
            case 'ALLOW': return 'ALLOWED';
            case 'REVIEW': return 'REVIEW';
            case 'BLOCK': return 'BLOCKED';
            default: return 'UNKNOWN';
        }
    }

    saveValidation(validation) {
        this.validations.unshift(validation);
        if (this.validations.length > 100) {
            this.validations = this.validations.slice(0, 100);
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