/**
 * ShopNexa Central Admin Management Hub Client
 * Handles full platform administration for Users, Merchants, Couriers, Products, and Orders
 */

const API_BASE = '/admin';

const state = {
    currentAdmin: null,
    stats: {},
    users: [],
    merchants: [],
    partners: [],
    products: [],
    orders: [],
    notifications: []
};

document.addEventListener('DOMContentLoaded', () => {
    initAuth();
    initTabs();
    checkSession();
});

// 1. Session & Auth
function checkSession() {
    const saved = localStorage.getItem('shopnexa_admin');
    if (saved) {
        try {
            state.currentAdmin = JSON.parse(saved);
            renderAuthenticatedView();
            refreshAllData();
            return;
        } catch (e) {
            localStorage.removeItem('shopnexa_admin');
        }
    }
    renderLoginView();
}

function renderLoginView() {
    document.getElementById('authSection').style.display = 'flex';
    document.getElementById('mainPortalSection').style.display = 'none';
    document.getElementById('navProfileBar').style.display = 'none';
    document.getElementById('navGuestBar').style.display = 'flex';
}

function renderAuthenticatedView() {
    document.getElementById('authSection').style.display = 'none';
    document.getElementById('mainPortalSection').style.display = 'block';
    document.getElementById('navProfileBar').style.display = 'flex';
    document.getElementById('navGuestBar').style.display = 'none';

    const admin = state.currentAdmin || {};
    const name = admin.adminName || 'System Admin';
    document.getElementById('adminNameDisplay').textContent = name;
    document.getElementById('userAvatarChip').textContent = name.charAt(0).toUpperCase();
}

function initAuth() {
    const form = document.getElementById('adminLoginForm');
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = document.getElementById('loginEmail').value.trim();
            const password = document.getElementById('loginPassword').value.trim();
            await performLogin(email, password);
        });
    }
}

function quickAdminLogin(email, password) {
    document.getElementById('loginEmail').value = email;
    document.getElementById('loginPassword').value = password;
    performLogin(email, password);
}

async function performLogin(email, password) {
    const alertBox = document.getElementById('authAlert');
    const btn = document.getElementById('loginSubmitBtn');

    if (!email || !password) {
        showAlert(alertBox, 'Please enter email and password.', 'danger');
        return;
    }

    try {
        btn.disabled = true;
        btn.textContent = 'Verifying Admin Access...';

        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await res.json();
        if (!res.ok || !data.success) {
            throw new Error(data.message || 'Invalid administrator credentials.');
        }

        state.currentAdmin = data;
        localStorage.setItem('shopnexa_admin', JSON.stringify(data));
        renderAuthenticatedView();
        await refreshAllData();
    } catch (err) {
        showAlert(alertBox, err.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Sign In to Admin Control Panel \u2192';
    }
}

function handleAdminLogout() {
    localStorage.removeItem('shopnexa_admin');
    state.currentAdmin = null;
    renderLoginView();
    showToast('Administrator signed out successfully.', 'info');
}

// 2. Tab Navigation
function initTabs() {
    const buttons = document.querySelectorAll('.tab-btn');
    const panels = document.querySelectorAll('.tab-panel');

    buttons.forEach(btn => {
        btn.addEventListener('click', () => {
            const target = btn.dataset.tab;
            buttons.forEach(b => b.classList.remove('active'));
            panels.forEach(p => p.classList.remove('active'));

            btn.classList.add('active');
            const activePanel = document.getElementById(target);
            if (activePanel) activePanel.classList.add('active');

            // Load module data
            if (target === 'users-tab') loadUsers();
            else if (target === 'merchants-tab') loadMerchants();
            else if (target === 'partners-tab') loadPartners();
            else if (target === 'products-tab') loadProducts();
            else if (target === 'orders-tab') loadOrders();
            else if (target === 'notifications-tab') loadNotifications();
        });
    });
}

// 3. Data Refresh Orchestrator
async function refreshAllData() {
    await Promise.all([
        loadStats(),
        loadUsers(),
        loadMerchants(),
        loadPartners(),
        loadProducts(),
        loadOrders(),
        loadNotifications()
    ]);
}

// 4. Load KPIs & Stats
async function loadStats() {
    try {
        const res = await fetch(`${API_BASE}/stats`);
        if (res.ok) {
            state.stats = await res.json();
            document.getElementById('kpiUsers').textContent = state.stats.totalUsers || 0;
            document.getElementById('kpiMerchants').textContent = state.stats.totalMerchants || 0;
            document.getElementById('kpiPartners').textContent = state.stats.totalDeliveryPartners || 0;
            document.getElementById('kpiProducts').textContent = state.stats.totalProducts || 0;
            document.getElementById('kpiOrders').textContent = state.stats.totalOrders || 0;
            document.getElementById('kpiRevenue').textContent = 'Rs. ' + Number(state.stats.totalRevenue || 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        }
    } catch (err) {
        console.error('Stats error:', err);
    }
}

// 5. Customer Users Management
async function loadUsers() {
    try {
        const res = await fetch(`${API_BASE}/users`);
        if (res.ok) {
            state.users = await res.json();
            document.getElementById('badgeUsers').textContent = state.users.length;
            renderUsersTable(state.users);
        }
    } catch (err) {
        console.error('Users error:', err);
    }
}

function renderUsersTable(list) {
    const tbody = document.getElementById('usersTableBody');
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding:2rem; color:#718096;">No customers found.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(u => `
        <tr>
            <td><strong>#${u.profileId}</strong></td>
            <td><strong style="color:#1e293b;">${escapeHtml(u.fullName || 'Customer')}</strong></td>
            <td>${escapeHtml(u.emailId || '')}</td>
            <td>${u.mobileNumber ? '+91 ' + u.mobileNumber : 'N/A'}</td>
            <td><span class="status-badge badge-customer">${escapeHtml(u.role || 'CUSTOMER')}</span></td>
            <td><span style="font-size:0.8rem; color:#64748b;">${escapeHtml(u.provider || 'LOCAL')}</span></td>
            <td>
                <button class="btn btn-sm btn-outline" style="color:#e53e3e; border-color:#fecaca;" onclick="handleDeleteUser(${u.profileId})">
                    🗑️ Delete
                </button>
            </td>
        </tr>
    `).join('');
}

function filterUsersTable() {
    const query = (document.getElementById('searchUsers').value || '').toLowerCase();
    const filtered = state.users.filter(u => 
        (u.fullName || '').toLowerCase().includes(query) || 
        (u.emailId || '').toLowerCase().includes(query) ||
        String(u.profileId).includes(query)
    );
    renderUsersTable(filtered);
}

async function handleAddUser(e) {
    e.preventDefault();
    const fullName = document.getElementById('userNameInput').value.trim();
    const emailId = document.getElementById('userEmailInput').value.trim();
    const password = document.getElementById('userPasswordInput').value.trim();
    const mobileNumber = parseInt(document.getElementById('userPhoneInput').value) || 9876543210;

    try {
        const res = await fetch(`${API_BASE}/users`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, emailId, password, mobileNumber, role: 'CUSTOMER' })
        });

        if (!res.ok) throw new Error('Failed to create customer');
        closeModal('modalAddUser');
        showToast(`Customer account for ${fullName} created!`, 'success');
        await loadUsers();
        await loadStats();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

async function handleDeleteUser(id) {
    if (!confirm(`Are you sure you want to remove Customer #${id}?`)) return;
    try {
        const res = await fetch(`${API_BASE}/users/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to delete customer');
        showToast(`Customer #${id} removed from platform.`, 'info');
        await loadUsers();
        await loadStats();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

// 6. Merchants Management
async function loadMerchants() {
    try {
        const res = await fetch(`${API_BASE}/merchants`);
        if (res.ok) {
            state.merchants = await res.json();
            document.getElementById('badgeMerchants').textContent = state.merchants.length;
            renderMerchantsTable(state.merchants);
        }
    } catch (err) {
        console.error('Merchants error:', err);
    }
}

function renderMerchantsTable(list) {
    const tbody = document.getElementById('merchantsTableBody');
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding:2rem; color:#718096;">No merchants found.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(m => `
        <tr>
            <td><strong>#${m.profileId}</strong></td>
            <td><strong style="color:#c05621;">🏪 ${escapeHtml(m.fullName || 'Seller')}</strong></td>
            <td>${escapeHtml(m.emailId || '')}</td>
            <td>${m.mobileNumber ? '+91 ' + m.mobileNumber : 'N/A'}</td>
            <td><span class="status-badge badge-merchant">${escapeHtml(m.role || 'MERCHANT')}</span></td>
            <td><span style="color:#16a34a; font-weight:700; font-size:0.85rem;">● Active Seller</span></td>
            <td>
                <button class="btn btn-sm btn-outline" style="color:#e53e3e; border-color:#fecaca;" onclick="handleDeleteMerchant(${m.profileId})">
                    🗑️ Remove
                </button>
            </td>
        </tr>
    `).join('');
}

function filterMerchantsTable() {
    const query = (document.getElementById('searchMerchants').value || '').toLowerCase();
    const filtered = state.merchants.filter(m => 
        (m.fullName || '').toLowerCase().includes(query) || 
        (m.emailId || '').toLowerCase().includes(query)
    );
    renderMerchantsTable(filtered);
}

async function handleAddMerchant(e) {
    e.preventDefault();
    const fullName = document.getElementById('merchantNameInput').value.trim();
    const emailId = document.getElementById('merchantEmailInput').value.trim();
    const password = document.getElementById('merchantPasswordInput').value.trim();
    const mobileNumber = parseInt(document.getElementById('merchantPhoneInput').value) || 9876512345;

    try {
        const res = await fetch(`${API_BASE}/merchants`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, emailId, password, mobileNumber, role: 'MERCHANT' })
        });

        if (!res.ok) throw new Error('Failed to register merchant');
        closeModal('modalAddMerchant');
        showToast(`Merchant account for ${fullName} registered!`, 'success');
        await loadMerchants();
        await loadStats();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

async function handleDeleteMerchant(id) {
    if (!confirm(`Are you sure you want to remove Merchant #${id}?`)) return;
    try {
        const res = await fetch(`${API_BASE}/merchants/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to delete merchant');
        showToast(`Merchant #${id} removed.`, 'info');
        await loadMerchants();
        await loadStats();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

// 7. Delivery Partners Management
async function loadPartners() {
    try {
        const res = await fetch(`${API_BASE}/delivery-partners`);
        if (res.ok) {
            state.partners = await res.json();
            document.getElementById('badgePartners').textContent = state.partners.length;
            renderPartnersTable(state.partners);
        }
    } catch (err) {
        console.error('Partners error:', err);
    }
}

function renderPartnersTable(list) {
    const tbody = document.getElementById('partnersTableBody');
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align:center; padding:2rem; color:#718096;">No delivery partners found.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(p => `
        <tr>
            <td><strong>#${p.partnerId}</strong></td>
            <td><strong style="color:#0284c7;">🛵 ${escapeHtml(p.partnerName || 'Courier')}</strong></td>
            <td>${escapeHtml(p.email || '')}</td>
            <td>${escapeHtml(p.phone || '')}</td>
            <td><span class="status-badge badge-partner">${escapeHtml(p.vehicleType || 'Bike')}</span></td>
            <td>${escapeHtml(p.serviceZone || 'Central')}</td>
            <td><strong style="color:#d97706;">★ ${Number(p.rating || 4.8).toFixed(1)}</strong></td>
            <td><strong>${p.totalDeliveries || 0} packages</strong></td>
            <td>
                <button class="btn btn-sm btn-outline" style="color:#e53e3e; border-color:#fecaca;" onclick="handleDeletePartner(${p.partnerId})">
                    🗑️ Deactivate
                </button>
            </td>
        </tr>
    `).join('');
}

function filterPartnersTable() {
    const query = (document.getElementById('searchPartners').value || '').toLowerCase();
    const filtered = state.partners.filter(p => 
        (p.partnerName || '').toLowerCase().includes(query) || 
        (p.email || '').toLowerCase().includes(query) ||
        (p.serviceZone || '').toLowerCase().includes(query)
    );
    renderPartnersTable(filtered);
}

async function handleAddPartner(e) {
    e.preventDefault();
    const partnerName = document.getElementById('partnerNameInput').value.trim();
    const email = document.getElementById('partnerEmailInput').value.trim();
    const phone = document.getElementById('partnerPhoneInput').value.trim();
    const vehicleType = document.getElementById('partnerVehicleInput').value;
    const serviceZone = document.getElementById('partnerZoneInput').value.trim();
    const password = document.getElementById('partnerPasswordInput').value.trim();

    try {
        const res = await fetch(`${API_BASE}/delivery-partners`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ partnerName, email, phone, vehicleType, serviceZone, password, active: true })
        });

        if (!res.ok) throw new Error('Failed to register courier');
        closeModal('modalAddPartner');
        showToast(`Courier ${partnerName} registered successfully!`, 'success');
        await loadPartners();
        await loadStats();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

async function handleDeletePartner(id) {
    if (!confirm(`Are you sure you want to deactivate Delivery Partner #${id}?`)) return;
    try {
        const res = await fetch(`${API_BASE}/delivery-partners/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to deactivate partner');
        showToast(`Delivery Partner #${id} deactivated.`, 'info');
        await loadPartners();
        await loadStats();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

// 8. Products Management
async function loadProducts() {
    try {
        const res = await fetch(`${API_BASE}/products`);
        if (res.ok) {
            state.products = await res.json();
            document.getElementById('badgeProducts').textContent = state.products.length;
            renderProductsTable(state.products);
        }
    } catch (err) {
        console.error('Products error:', err);
    }
}

function renderProductsTable(list) {
    const tbody = document.getElementById('productsTableBody');
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" style="text-align:center; padding:2rem; color:#718096;">No products in store catalog.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(prod => {
        const img = (prod.image && prod.image.length > 0) ? prod.image[0] : 'https://placehold.co/60x60?text=Item';
        const price = Number(prod.price || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
        return `
            <tr>
                <td style="width: 60px;">
                    <img src="${escapeHtml(img)}" alt="" style="width: 48px; height: 48px; object-fit: cover; border-radius: 6px; border: 1px solid #e2e8f0;"
                         onerror="this.src='https://placehold.co/48x48?text=No+Img'">
                </td>
                <td>
                    <div style="font-weight:700; color:#1e293b;">${escapeHtml(prod.productName || 'Unnamed')}</div>
                    <div style="font-size:0.78rem; color:#64748b;">ID: #${prod.productId}</div>
                </td>
                <td><span style="background:#f1f5f9; padding:0.2rem 0.5rem; border-radius:4px; font-size:0.8rem; font-weight:600;">${escapeHtml(prod.category || 'General')}</span></td>
                <td>${escapeHtml(prod.productType || 'Standard')}</td>
                <td><strong style="color:#1d4ed8;">Rs. ${price}</strong></td>
                <td><span style="font-size:0.85rem; color:#475569;">${escapeHtml(prod.sellerName || 'Merchant')}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline" style="color:#e53e3e; border-color:#fecaca;" onclick="handleDeleteProduct(${prod.productId})">
                        🗑️ Delete
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterProductsTable() {
    const query = (document.getElementById('searchProducts').value || '').toLowerCase();
    const filtered = state.products.filter(p => 
        (p.productName || '').toLowerCase().includes(query) || 
        (p.category || '').toLowerCase().includes(query) ||
        (p.sellerName || '').toLowerCase().includes(query)
    );
    renderProductsTable(filtered);
}

async function handleAddProduct(e) {
    e.preventDefault();
    const productName = document.getElementById('prodNameInput').value.trim();
    const category = document.getElementById('prodCategoryInput').value.trim() || 'General';
    const price = parseFloat(document.getElementById('prodPriceInput').value);
    const productType = document.getElementById('prodTypeInput').value.trim() || 'Physical';
    const imageUrl = document.getElementById('prodImageUrlInput').value.trim();
    const description = document.getElementById('prodDescInput').value.trim();

    try {
        const res = await fetch(`${API_BASE}/products`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                productName, category, price, productType, description,
                image: imageUrl ? [imageUrl] : [],
                merchantId: 5,
                sellerName: 'ShopNexa Platform Admin'
            })
        });

        if (!res.ok) throw new Error('Failed to create product');
        closeModal('modalAddProduct');
        showToast(`Product "${productName}" added to catalog!`, 'success');
        await loadProducts();
        await loadStats();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

async function handleDeleteProduct(id) {
    if (!confirm(`Are you sure you want to remove Product #${id} from the catalog?`)) return;
    try {
        const res = await fetch(`${API_BASE}/products/${id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to delete product');
        showToast(`Product #${id} removed.`, 'info');
        await loadProducts();
        await loadStats();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

// 9. Orders & Deliveries Management
async function loadOrders() {
    try {
        const res = await fetch(`${API_BASE}/orders`);
        if (res.ok) {
            state.orders = await res.json();
            document.getElementById('badgeOrders').textContent = state.orders.length;
            renderOrdersTable(state.orders);
        }
    } catch (err) {
        console.error('Orders error:', err);
    }
}

function renderOrdersTable(list) {
    const tbody = document.getElementById('ordersTableBody');
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align:center; padding:2rem; color:#718096;">No orders placed yet.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(ord => {
        const status = (ord.orderStatus || 'PLACED').toUpperCase();
        const badgeClass = getStatusClass(status);
        const prodName = ord.product?.productName || 'Catalog Item';
        const partner = ord.deliveryPartnerName ? `🛵 ${escapeHtml(ord.deliveryPartnerName)}` : '<span style="color:#a0aec0;">Unassigned</span>';
        const total = Number(ord.ammountPaid || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
        const date = ord.orderDate || 'Recent';

        return `
            <tr>
                <td><strong>#${ord.orderId}</strong></td>
                <td style="color:#64748b; font-size:0.85rem;">${escapeHtml(date)}</td>
                <td><strong style="color:#1e293b;">Customer #${ord.customerId}</strong></td>
                <td>${escapeHtml(prodName)}</td>
                <td><strong style="color:#1d4ed8;">Rs. ${total}</strong></td>
                <td><span style="font-size:0.82rem; color:#475569;">${escapeHtml(ord.modeOfPayment || 'WALLET')}</span></td>
                <td><div style="font-size:0.82rem; font-weight:600; color:#0369a1;">${partner}</div></td>
                <td><span class="status-badge ${badgeClass}">${status}</span></td>
                <td>
                    <button class="btn btn-sm btn-outline" onclick="openUpdateOrderModal(${ord.orderId}, '${status}')">
                        📝 Edit Status
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function filterOrdersTable() {
    const query = (document.getElementById('searchOrders').value || '').toLowerCase();
    const filtered = state.orders.filter(o => 
        String(o.orderId).includes(query) || 
        String(o.customerId).includes(query) ||
        (o.orderStatus || '').toLowerCase().includes(query) ||
        (o.deliveryPartnerName || '').toLowerCase().includes(query)
    );
    renderOrdersTable(filtered);
}

function openUpdateOrderModal(orderId, currentStatus) {
    document.getElementById('editOrderId').value = orderId;
    document.getElementById('displayEditOrderId').textContent = orderId;
    document.getElementById('editOrderStatus').value = currentStatus;
    openModal('modalUpdateOrder');
}

async function handleUpdateOrderStatus(e) {
    e.preventDefault();
    const orderId = document.getElementById('editOrderId').value;
    const status = document.getElementById('editOrderStatus').value;

    try {
        const res = await fetch(`${API_BASE}/orders/${orderId}/status?status=${status}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status })
        });

        if (!res.ok) throw new Error('Status update failed');
        closeModal('modalUpdateOrder');
        showToast(`Order #${orderId} status updated to ${status}!`, 'success');
        await loadOrders();
        await loadStats();
    } catch (err) {
        alert('Error: ' + err.message);
    }
}

// 10. Platform Notifications Audit Stream
async function loadNotifications() {
    try {
        const res = await fetch('http://localhost:8086/notify/history');
        if (res.ok) {
            state.notifications = await res.json();
            document.getElementById('badgeNotifs').textContent = state.notifications.length;
            renderNotificationsTable(state.notifications);
        }
    } catch (err) {
        console.error('Notifications error:', err);
    }
}

function renderNotificationsTable(list) {
    const tbody = document.getElementById('notificationsTableBody');
    if (!tbody) return;

    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding:2rem; color:#718096;">No dispatch emails recorded.</td></tr>';
        return;
    }

    tbody.innerHTML = list.map(n => `
        <tr>
            <td style="font-family:monospace; font-size:0.8rem; color:#64748b;">${escapeHtml(n.id ? n.id.substring(0, 8) + '...' : '')}</td>
            <td><strong style="color:#1e293b;">${escapeHtml(n.toEmail || '')}</strong></td>
            <td>${escapeHtml(n.subject || '')}</td>
            <td><span class="status-badge badge-partner">${escapeHtml(n.type || 'GENERAL')}</span></td>
            <td style="font-size:0.85rem; color:#64748b;">${escapeHtml(n.timestamp || 'Recent')}</td>
            <td><span style="color:#16a34a; font-weight:700;">● DISPATCHED</span></td>
        </tr>
    `).join('');
}

// Helper utilities
function getStatusClass(status) {
    const s = (status || '').toLowerCase();
    if (s.includes('shipped')) return 'status-shipped';
    if (s.includes('delivery') || s.includes('out')) return 'status-out-for-delivery';
    if (s.includes('delivered')) return 'status-delivered';
    if (s.includes('confirmed')) return 'status-confirmed';
    if (s.includes('cancelled')) return 'status-cancelled';
    return 'status-placed';
}

function openModal(id) {
    const m = document.getElementById(id);
    if (m) m.classList.add('active');
}

function closeModal(id) {
    const m = document.getElementById(id);
    if (m) m.classList.remove('active');
}

function showAlert(el, msg, type) {
    if (!el) return;
    el.style.display = 'block';
    el.className = `alert alert-${type}`;
    el.textContent = msg;
}

function showToast(msg, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `alert alert-${type === 'error' ? 'danger' : 'success'}`;
    toast.style.position = 'fixed';
    toast.style.bottom = '20px';
    toast.style.right = '20px';
    toast.style.zIndex = '9999';
    toast.style.boxShadow = '0 10px 15px -3px rgba(0,0,0,0.2)';
    toast.textContent = msg;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3500);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
