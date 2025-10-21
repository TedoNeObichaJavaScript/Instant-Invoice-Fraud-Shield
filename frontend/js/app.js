// Instant Invoice: Fraud Shield - Frontend Application
class FraudShieldApp {
    constructor() {
        this.apiBaseUrl = '/api';
        this.authToken = null;
        this.assessments = [];
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.checkAuthStatus();
        this.loadRecentAssessments();
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

        // Risk assessment form
        const riskForm = document.getElementById('riskAssessmentForm');
        if (riskForm) {
            riskForm.addEventListener('submit', (e) => this.handleRiskAssessment(e));
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

    async handleRiskAssessment(e) {
        e.preventDefault();
        
        if (!this.authToken) {
            this.showMessage('Please login first', 'error');
            return;
        }

        const formData = {
            iban: document.getElementById('iban').value,
            invoiceId: document.getElementById('invoiceId').value,
            amount: parseFloat(document.getElementById('amount').value),
            currency: document.getElementById('currency').value,
            merchantId: document.getElementById('merchantId').value
        };

        try {
            this.showLoading(true);
            const assessBtn = document.getElementById('assessBtn');
            assessBtn.disabled = true;

            const response = await fetch(`${this.apiBaseUrl}/v1/accounts/risk-assessment`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.authToken}`
                },
                body: JSON.stringify(formData)
            });

            const data = await response.json();

            if (response.ok) {
                this.displayAssessmentResult(data);
                this.addToRecentAssessments(data);
                this.updateStats(data);
                
                if (data.acceptableResponseTime) {
                    this.showMessage('Risk assessment completed successfully!', 'success');
                } else {
                    this.showMessage('Risk assessment completed but response time exceeded 200ms', 'info');
                }
            } else {
                this.showMessage(data.error || 'Risk assessment failed', 'error');
            }
        } catch (error) {
            this.showMessage('Network error: ' + error.message, 'error');
        } finally {
            this.showLoading(false);
            const assessBtn = document.getElementById('assessBtn');
            assessBtn.disabled = false;
        }
    }

    displayAssessmentResult(data) {
        const resultsDiv = document.getElementById('results');
        const decisionSpan = document.getElementById('decision');
        const riskLevelSpan = document.getElementById('riskLevel');
        const invoiceIdSpan = document.getElementById('resultInvoiceId');
        const reasonSpan = document.getElementById('reason');
        const responseTimeSpan = document.getElementById('responseTime');
        const timestampSpan = document.getElementById('timestamp');

        decisionSpan.textContent = data.decision;
        decisionSpan.className = `decision ${data.decision}`;
        
        riskLevelSpan.textContent = data.riskLevel;
        invoiceIdSpan.textContent = data.invoiceId;
        reasonSpan.textContent = data.reason;
        responseTimeSpan.textContent = data.responseTimeMs;
        timestampSpan.textContent = new Date(data.timestamp).toLocaleString();

        resultsDiv.style.display = 'block';
        resultsDiv.scrollIntoView({ behavior: 'smooth' });
    }

    addToRecentAssessments(data) {
        this.assessments.unshift({
            ...data,
            timestamp: new Date(data.timestamp)
        });

        // Keep only last 10 assessments
        if (this.assessments.length > 10) {
            this.assessments = this.assessments.slice(0, 10);
        }

        this.renderRecentAssessments();
    }

    renderRecentAssessments() {
        const recentList = document.getElementById('recentList');
        recentList.innerHTML = '';

        this.assessments.forEach(assessment => {
            const item = document.createElement('div');
            item.className = `assessment-item ${assessment.decision}`;
            
            item.innerHTML = `
                <div class="assessment-item-header">
                    <span class="assessment-item-decision ${assessment.decision}">${assessment.decision}</span>
                    <span class="assessment-item-time">${assessment.timestamp.toLocaleString()}</span>
                </div>
                <div class="assessment-item-details">
                    <strong>${assessment.invoiceId}</strong> - ${assessment.riskLevel} Risk
                    <br>
                    <small>${assessment.responseTimeMs}ms - ${assessment.reason}</small>
                </div>
            `;
            
            recentList.appendChild(item);
        });
    }

    updateStats(data) {
        // Update total assessments
        const totalElement = document.getElementById('totalAssessments');
        if (totalElement) {
            totalElement.textContent = this.assessments.length;
        }

        // Calculate average response time
        if (this.assessments.length > 0) {
            const avgResponseTime = this.assessments.reduce((sum, a) => sum + a.responseTimeMs, 0) / this.assessments.length;
            const avgElement = document.getElementById('avgResponseTime');
            if (avgElement) {
                avgElement.textContent = Math.round(avgResponseTime);
            }

            // Calculate success rate (acceptable response times)
            const acceptableCount = this.assessments.filter(a => a.responseTimeMs <= 200).length;
            const successRate = (acceptableCount / this.assessments.length) * 100;
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

    loadRecentAssessments() {
        // Load from localStorage if available
        const saved = localStorage.getItem('recentAssessments');
        if (saved) {
            try {
                this.assessments = JSON.parse(saved).map(a => ({
                    ...a,
                    timestamp: new Date(a.timestamp)
                }));
                this.renderRecentAssessments();
            } catch (error) {
                console.error('Error loading recent assessments:', error);
            }
        }
    }

    saveRecentAssessments() {
        localStorage.setItem('recentAssessments', JSON.stringify(this.assessments));
    }
}

// Initialize the application when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.fraudShieldApp = new FraudShieldApp();
    
    // Save assessments periodically
    setInterval(() => {
        if (window.fraudShieldApp) {
            window.fraudShieldApp.saveRecentAssessments();
        }
    }, 30000); // Every 30 seconds
});

// Handle page unload
window.addEventListener('beforeunload', () => {
    if (window.fraudShieldApp) {
        window.fraudShieldApp.saveRecentAssessments();
    }
});
