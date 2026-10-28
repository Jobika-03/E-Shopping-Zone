/**
 * EShoppingZone Merchant Portal Client App
 * Handles: Authentication (Manual + Google/GitHub), Manual Product Addition, Interactive Order Tracking
 */

const API_BASE = '/merchant';

// Global App State
const state = {
    currentMerchant: null,
    products: [],
    orders: [],
    deliveryPartners: [],
    selectedOrder: null,
    activeStatusFilter: 'ALL',
    orderSearchQuery: '',
    orderCurrentPage: 1,
    orderPageLimit: 5,
    walletBalance: 0.0,
    walletStatements: [],
    walletCurrentPage: 1,
    walletPageLimit: 10
};

// Tracking Step Definitions
const TRACKING_STEPS = [
    { key: 'PLACED', label: 'Ordered', icon: '1' },
    { key: 'CONFIRMED', label: 'Confirmed', icon: '2' },
    { key: 'SHIPPED', label: 'Shipped', icon: '3' },
    { key: 'OUT_FOR_DELIVERY', label: 'Out for Delivery', icon: '4' },
    { key: 'DELIVERED', label: 'Delivered', icon: '5' }
];

document.addEventListener('DOMContentLoaded', () => {
    initAuth();
    initTabs();
    initProductForm();
    initOrderModal();
    initFilterButtons();
    checkExistingSession();
});

// Check Existing Session
function checkExistingSession() {
    const saved = sessionStorage.getItem('merchant_session');
    if (saved) {
        try {
            state.currentMerchant = JSON.parse(saved);
            renderAuthenticatedView();
            loadAllData();
            return;
        } catch (e) {
            sessionStorage.removeItem('merchant_session');
        }
    }
    renderLoginView();
}

function renderLoginView() {
    const authSec = document.getElementById('authSection');
    const mainSec = document.getElementById('mainPortalSection');
    const navProf = document.getElementById('navProfileBar');
    const navGuest = document.getElementById('navGuestBar');

    if (authSec) authSec.style.display = 'flex';
    if (mainSec) mainSec.style.display = 'none';
    if (navProf) navProf.style.display = 'none';
    if (navGuest) navGuest.style.display = 'flex';
}

function renderAuthenticatedView() {
    const authSec = document.getElementById('authSection');
    const mainSec = document.getElementById('mainPortalSection');
    const navProf = document.getElementById('navProfileBar');
    const navGuest = document.getElementById('navGuestBar');

    if (authSec) authSec.style.display = 'none';
    if (mainSec) mainSec.style.display = 'block';
    if (navProf) navProf.style.display = 'flex';
    if (navGuest) navGuest.style.display = 'none';

    const merchant = state.currentMerchant || {};
    const name = merchant.merchantName || merchant.fullName || 'Seller';
    const id = merchant.merchantId || 1;

    const nameEl = document.getElementById('currentSellerNameDisplay');
    const idEl = document.getElementById('currentSellerIdBadge');
    const chip = document.getElementById('userAvatarChip');

    if (nameEl) nameEl.textContent = name;
    if (idEl) idEl.textContent = '#' + id;
    if (chip) chip.textContent = name.charAt(0).toUpperCase();
}

function quickMerchantLogin(email, password) {
    const emailInput = document.getElementById('loginEmail');
    const pwdInput = document.getElementById('loginPassword');
    if (emailInput) emailInput.value = email;
    if (pwdInput) pwdInput.value = password;
    handleManualLogin();
}

// Authentication Forms & Tabs
function initAuth() {
    const loginForm = document.getElementById('merchantLoginForm');
    const registerForm = document.getElementById('merchantRegisterForm');
    const socialForm = document.getElementById('socialAuthForm');

    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await handleManualLogin();
        });
    }

    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await handleManualRegister();
        });
    }

    if (socialForm) {
        socialForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await handleSocialAuthSubmit();
        });
    }
}

function switchAuthTab(tab) {
    const btnSignIn = document.getElementById('tabBtnSignIn');
    const btnRegister = document.getElementById('tabBtnRegister');
    const paneSignIn = document.getElementById('signInTabPane');
    const paneRegister = document.getElementById('registerTabPane');
    const alertBox = document.getElementById('authAlert');
    if (alertBox) alertBox.style.display = 'none';

    if (tab === 'signin') {
        btnSignIn.classList.add('active');
        btnRegister.classList.remove('active');
        paneSignIn.style.display = 'block';
        paneRegister.style.display = 'none';
    } else {
        btnSignIn.classList.remove('active');
        btnRegister.classList.add('active');
        paneSignIn.style.display = 'none';
        paneRegister.style.display = 'block';
    }
}

// Manual Login
async function handleManualLogin() {
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value.trim();
    const btn = document.getElementById('loginSubmitBtn');
    const alertBox = document.getElementById('authAlert');

    if (!email || !password) {
        showAlert(alertBox, 'Please enter both your email and password.', 'error');
        return;
    }

    try {
        btn.disabled = true;
        btn.textContent = 'Signing in...';

        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        const data = await res.json();
        if (!res.ok || !data.success) {
            throw new Error(data.message || 'Invalid seller credentials or access denied.');
        }

        state.currentMerchant = data;
        sessionStorage.setItem('merchant_session', JSON.stringify(data));
        renderAuthenticatedView();
        await loadAllData();
    } catch (err) {
        showAlert(alertBox, err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Sign In as Merchant \u2192';
    }
}

// Manual Register
async function handleManualRegister() {
    const fullName = document.getElementById('regName').value.trim();
    const emailId = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value.trim();
    const mobile = document.getElementById('regPhone').value.trim();
    const btn = document.getElementById('regSubmitBtn');
    const alertBox = document.getElementById('authAlert');

    try {
        btn.disabled = true;
        btn.textContent = 'Creating Account...';

        const payload = {
            fullName,
            emailId,
            password,
            mobileNumber: mobile ? parseInt(mobile.replace(/\D/g, ''), 10) : null
        };

        const res = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        if (!res.ok || !data.success) {
            throw new Error(data.message || 'Registration failed. Email might already exist.');
        }

        state.currentMerchant = data;
        sessionStorage.setItem('merchant_session', JSON.stringify(data));
        renderAuthenticatedView();
        await loadAllData();
    } catch (err) {
        showAlert(alertBox, err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = 'Register Merchant Account \u2192';
    }
}

// Social Authentication Modal (Google / GitHub)
function openSocialAuthModal(provider) {
    const modal = document.getElementById('socialAccountModal');
    const title = document.getElementById('socialModalTitle');
    const desc = document.getElementById('socialModalDesc');
    const providerInput = document.getElementById('socialProvider');
    const emailLabel = document.getElementById('socialEmailLabel');
    const emailInput = document.getElementById('socialEmail');

    providerInput.value = provider.toUpperCase();

    if (provider.toLowerCase() === 'google') {
        title.textContent = 'Sign In with Google Account';
        desc.textContent = 'Enter your Google Account email to securely access your Seller Hub.';
        emailLabel.textContent = 'Google Email Address';
        emailInput.placeholder = 'yourname@gmail.com';
    } else {
        title.textContent = 'Sign In with GitHub Account';
        desc.textContent = 'Enter your GitHub email or account username to access your Seller Hub.';
        emailLabel.textContent = 'GitHub Account Email';
        emailInput.placeholder = 'username@users.noreply.github.com';
    }

    modal.classList.add('active');
}

function closeSocialAuthModal() {
    document.getElementById('socialAccountModal').classList.remove('active');
}

async function handleSocialAuthSubmit() {
    const provider = document.getElementById('socialProvider').value;
    const email = document.getElementById('socialEmail').value.trim();
    const name = document.getElementById('socialName').value.trim();
    const btn = document.getElementById('socialSubmitBtn');

    if (!email) return;

    try {
        btn.disabled = true;
        btn.textContent = 'Authenticating...';

        const res = await fetch(`${API_BASE}/auth/social-login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ provider, email, name })
        });

        const data = await res.json();
        if (!res.ok || !data.success) {
            throw new Error(data.message || 'Social authentication failed.');
        }

        state.currentMerchant = data;
        sessionStorage.setItem('merchant_session', JSON.stringify(data));
        closeSocialAuthModal();
        renderAuthenticatedView();
        await loadAllData();
    } catch (err) {
        alert('Authentication error: ' + err.message);
    } finally {
        btn.disabled = false;
        btn.textContent = 'Continue as Merchant \u2192';
    }
}

// Sign Out
function handleMerchantLogout() {
    sessionStorage.removeItem('merchant_session');
    state.currentMerchant = null;
    state.products = [];
    state.orders = [];
    renderLoginView();
    showNotification('You have signed out of the Merchant Hub.', 'info');
}

// Initialize Tab Switching
function initTabs() {
    const tabButtons = document.querySelectorAll('.tab-btn');
    const tabPanels = document.querySelectorAll('.tab-panel');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.dataset.tab;
            
            tabButtons.forEach(b => b.classList.remove('active'));
            tabPanels.forEach(p => p.classList.remove('active'));

            btn.classList.add('active');
            const activePanel = document.getElementById(targetTab);
            if (activePanel) {
                activePanel.classList.add('active');
            }

            if (targetTab === 'orders-tab') {
                loadOrders();
            } else if (targetTab === 'products-tab') {
                loadProducts();
            } else if (targetTab === 'wallet-tab') {
                loadMerchantWallet();
            }
        });
    });
}

function switchTab(tabId) {
    const btn = document.querySelector(`.tab-btn[data-tab="${tabId}"]`);
    if (btn) btn.click();
}

// Product Form & Image Preview
function initProductForm() {
    const form = document.getElementById('addProductForm');
    const imageUrlInput = document.getElementById('productImageUrl');
    const previewContainer = document.getElementById('imagePreviewContainer');
    const previewImg = document.getElementById('imagePreviewImg');

    if (imageUrlInput && previewImg) {
        imageUrlInput.addEventListener('input', (e) => {
            const url = e.target.value.trim();
            if (url) {
                previewImg.src = url;
                previewContainer.style.display = 'flex';
                previewImg.onerror = () => {
                    previewImg.src = 'https://placehold.co/100x100?text=No+Image';
                };
            } else {
                previewContainer.style.display = 'none';
            }
        });
    }

    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            await handleAddProduct();
        });
    }
}

// Order Tracking Modal & Actions
function initOrderModal() {
    const modal = document.getElementById('orderModal');
    const closeBtn = document.getElementById('modalCloseBtn');
    const cancelBtn = document.getElementById('modalCancelBtn');
    const statusForm = document.getElementById('updateStatusForm');
    const statusPills = document.querySelectorAll('.status-pill');
    const manualInput = document.getElementById('manualStatusInput');

    const closeModal = () => {
        modal.classList.remove('active');
        state.selectedOrder = null;
    };

    if (closeBtn) closeBtn.addEventListener('click', closeModal);
    if (cancelBtn) cancelBtn.addEventListener('click', closeModal);

    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal.classList.contains('active')) {
            closeModal();
        }
    });

    const assignForm = document.getElementById('assignPartnerForm');
    if (assignForm) {
        assignForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await handleAssignDeliveryPartner();
        });
    }
}

// Filter buttons
function initFilterButtons() {
    const filterBtns = document.querySelectorAll('.filter-btn');
    filterBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            filterBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.activeStatusFilter = btn.dataset.filter;
            state.orderCurrentPage = 1;
            renderOrdersTable();
        });
    });
}

// Load All Data
async function loadAllData() {
    await Promise.all([
        loadDashboard(),
        loadProducts(),
        loadOrders(),
        loadDeliveryPartners(),
        loadMerchantWallet()
    ]);
}

// Helper: Get active merchant ID
function getMerchantId() {
    return state.currentMerchant?.merchantId || 1;
}

function getMerchantName() {
    return state.currentMerchant?.merchantName || state.currentMerchant?.fullName || 'Seller';
}

// 1. Dashboard KPIs
async function loadDashboard() {
    const mId = getMerchantId();
    try {
        const res = await fetch(`${API_BASE}/${mId}/dashboard`);
        if (!res.ok) throw new Error('Failed to load dashboard');
        const data = await res.json();

        document.getElementById('kpiProducts').textContent = data.totalProducts ?? 0;
        document.getElementById('kpiOrders').textContent = data.totalOrders ?? 0;
        document.getElementById('kpiRevenue').textContent = 'Rs. ' + Number(data.totalRevenue ?? 0).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        document.getElementById('kpiPending').textContent = data.pendingOrders ?? 0;
    } catch (err) {
        document.getElementById('kpiProducts').textContent = state.products.length;
        document.getElementById('kpiOrders').textContent = state.orders.length;
        const rev = state.orders.filter(o => o.orderStatus?.toUpperCase() !== 'CANCELLED').reduce((sum, o) => sum + (o.ammountPaid || 0), 0);
        document.getElementById('kpiRevenue').textContent = 'Rs. ' + rev.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
        const pending = state.orders.filter(o => ['PLACED', 'CONFIRMED', 'PLACED_COD', 'SHIPPED', 'OUT_FOR_DELIVERY', 'SHIPPING'].includes(o.orderStatus?.toUpperCase())).length;
        document.getElementById('kpiPending').textContent = pending;
    }
}

// 2. Load Merchant Products
async function loadProducts() {
    const mId = getMerchantId();
    try {
        const res = await fetch(`${API_BASE}/${mId}/products`);
        if (!res.ok) throw new Error('Failed to fetch products');
        state.products = await res.json();
        renderProductsTable();
        document.getElementById('productsCountBadge').textContent = state.products.length;
    } catch (err) {
        console.error('Products error:', err);
    }
}

function renderProductsTable() {
    const tbody = document.getElementById('productsTableBody');
    const emptyState = document.getElementById('productsEmptyState');
    if (!tbody) return;

    if (!state.products || state.products.length === 0) {
        tbody.innerHTML = '';
        if (emptyState) emptyState.style.display = 'block';
        return;
    }

    if (emptyState) emptyState.style.display = 'none';

    tbody.innerHTML = state.products.map(prod => {
        const img = (prod.image && prod.image.length > 0) ? prod.image[0] : 'https://placehold.co/60x60?text=Item';
        const price = Number(prod.price || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
        const stockCount = prod.stock != null ? prod.stock : 50;
        const isOutOfStock = prod.status === 'OUT_OF_STOCK' || stockCount <= 0;

        return `
            <tr>
                <td style="width: 70px;">
                    <img src="${escapeHtml(img)}" alt="${escapeHtml(prod.productName || '')}" 
                         style="width: 50px; height: 50px; object-fit: cover; border-radius: 6px; border: 1px solid #e2e8f0;"
                         onerror="this.src='https://placehold.co/50x50?text=No+Img'">
                </td>
                <td>
                    <div style="font-weight: 700; color: #1e293b;">${escapeHtml(prod.productName || 'Unnamed')}</div>
                    <div style="font-size: 0.8rem; color: #64748b;">ID: #${prod.productId}</div>
                </td>
                <td>
                    <span style="background: #f1f5f9; color: #334155; padding: 0.2rem 0.5rem; border-radius: 4px; font-size: 0.82rem; font-weight: 600;">
                        ${escapeHtml(prod.category || 'General')}
                    </span>
                </td>
                <td>
                    <strong style="color: #1d4ed8;">Rs. ${price}</strong>
                    ${prod.offerActive && prod.offerPrice ? `<div style="color: #dc2626; font-size: 0.8rem; font-weight: 700;">Offer: Rs. ${parseFloat(prod.offerPrice).toFixed(2)}</div>` : ''}
                </td>
                <td>
                    <!-- Merchant Offer Status & Trigger -->
                    ${prod.offerActive && prod.offerDiscountPercent > 0 ? `
                        <div style="margin-bottom: 0.25rem;">
                            <span style="background: #fef3c7; color: #92400e; font-size: 0.75rem; font-weight: 700; padding: 2px 6px; border-radius: 4px; display: inline-block;">
                                🏷️ ${prod.offerDiscountPercent}% OFF
                            </span>
                            <div style="font-size: 0.72rem; color: #78350f; font-weight: 600;">${escapeHtml(prod.offerTitle || 'Deal')}</div>
                            <button class="btn btn-sm btn-outline" style="font-size: 0.68rem; padding: 0.1rem 0.35rem; color: #ef4444; border-color: #fca5a5; margin-top: 0.2rem;"
                                    onclick="handleRemoveProductOffer(${prod.productId})">
                                ✕ Remove
                            </button>
                        </div>
                    ` : ''}
                    <button class="btn btn-sm btn-outline" style="font-size: 0.75rem; padding: 0.2rem 0.45rem; color: #2563eb;"
                            onclick="openOfferModal(${prod.productId}, '${escapeHtml(prod.productName || '')}', ${prod.offerDiscountPercent || 10}, '${escapeHtml(prod.offerTitle || 'Special Offer')}')">
                        ${prod.offerActive ? '✏️ Edit Offer' : '🏷️ + Add Offer'}
                    </button>
                </td>
                <td>
                    <!-- Quick Stock Quantity Updater -->
                    <div style="display: flex; align-items: center; gap: 0.35rem;">
                        <input type="number" id="inlineStock_${prod.productId}" value="${stockCount}" min="0" max="9999" 
                               class="form-control" style="width: 70px; padding: 0.2rem 0.4rem; font-size: 0.85rem; font-weight: 700; height: 32px;" />
                        <button class="btn btn-sm btn-outline" style="padding: 0.2rem 0.5rem; font-size: 0.75rem; height: 32px;" 
                                onclick="handleQuickUpdateStock(${prod.productId})" title="Save Stock Quantity">
                            💾
                        </button>
                    </div>
                </td>
                <td>
                    <!-- Out of Stock / In Stock Toggle Button -->
                    ${isOutOfStock ? `
                        <button class="btn btn-sm" style="background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0; font-size: 0.78rem; font-weight: 600; padding: 0.25rem 0.5rem; border-radius: 4px;"
                                onclick="handleToggleProductStatus(${prod.productId}, 'ACTIVE')" title="Click to restock and mark In Stock">
                            ✓ Mark In Stock
                        </button>
                    ` : `
                        <button class="btn btn-sm" style="background: #fef2f2; color: #dc2626; border: 1px solid #fecaca; font-size: 0.78rem; font-weight: 600; padding: 0.25rem 0.5rem; border-radius: 4px;"
                                onclick="handleToggleProductStatus(${prod.productId}, 'OUT_OF_STOCK')" title="Click to mark as Out of Stock">
                            ✕ Mark Out of Stock
                        </button>
                    `}
                </td>
                <td style="text-align: right;">
                    <div style="display: inline-flex; gap: 0.35rem;">
                        <button class="btn btn-sm btn-outline" onclick="openEditProductModal(${prod.productId})">
                            ✏️ Edit
                        </button>
                        <button class="btn btn-sm btn-outline" style="color: #ef4444; border-color: #fecaca;" 
                                onclick="handleDeleteProduct(${prod.productId})">
                            🗑️
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// 3. Add Product Manually
async function handleAddProduct() {
    const btn = document.getElementById('submitProductBtn');
    const alertBox = document.getElementById('addProductAlert');
    
    const productName = document.getElementById('productName').value.trim();
    const category = document.getElementById('productCategory').value.trim();
    const productType = document.getElementById('productType').value.trim();
    const price = parseFloat(document.getElementById('productPrice').value);
    const stock = parseInt(document.getElementById('productStock')?.value) || 50;
    const status = document.getElementById('productStatus')?.value || 'ACTIVE';
    const description = document.getElementById('productDescription').value.trim();
    const imageUrl = document.getElementById('productImageUrl').value.trim();

    if (!productName || isNaN(price) || price < 0) {
        showAlert(alertBox, 'Please provide valid product name and price.', 'error');
        return;
    }

    const payload = {
        productName,
        category: category || 'General',
        productType: productType || 'Physical',
        price,
        stock: Math.max(0, stock),
        status: status,
        description,
        merchantId: getMerchantId(),
        sellerName: getMerchantName(),
        image: imageUrl ? [imageUrl] : []
    };

    try {
        btn.disabled = true;
        btn.textContent = 'Saving Product...';

        const res = await fetch(`${API_BASE}/products`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!res.ok) throw new Error('Server returned ' + res.status);
        const created = await res.json();

        showAlert(alertBox, `✅ Product "${created.productName || productName}" created successfully with ${stock} in stock!`, 'success');
        
        document.getElementById('addProductForm').reset();
        document.getElementById('imagePreviewContainer').style.display = 'none';

        await loadProducts();
        await loadDashboard();

        setTimeout(() => {
            switchTab('products-tab');
        }, 1200);
    } catch (err) {
        showAlert(alertBox, 'Failed to create product: ' + err.message, 'error');
    } finally {
        btn.disabled = false;
        btn.textContent = '➕ Add Product to Catalog';
    }
}

// Quick Stock Quantity Update
async function handleQuickUpdateStock(productId) {
    const input = document.getElementById(`inlineStock_${productId}`);
    if (!input) return;
    const newStock = Math.max(0, parseInt(input.value) || 0);

    try {
        const res = await fetch(`${API_BASE}/products/${productId}/stock?stock=${newStock}`, {
            method: 'PUT'
        });
        if (!res.ok) throw new Error('Failed to update stock');
        
        // Also update status if stock became 0 or positive
        const newStatus = newStock > 0 ? 'ACTIVE' : 'OUT_OF_STOCK';
        await fetch(`${API_BASE}/products/${productId}/status?status=${newStatus}`, { method: 'PUT' });

        showNotification(`Stock for Product #${productId} updated to ${newStock} units.`, 'success');
        await loadProducts();
    } catch (err) {
        showNotification('Error updating stock: ' + err.message, 'error');
    }
}

// Toggle In-Stock / Out-of-Stock
async function handleToggleProductStatus(productId, targetStatus) {
    try {
        const res = await fetch(`${API_BASE}/products/${productId}/status?status=${targetStatus}`, {
            method: 'PUT'
        });
        if (!res.ok) throw new Error('Failed to update status');

        if (targetStatus === 'ACTIVE') {
            // Restore positive stock if currently 0
            const p = state.products.find(x => x.productId === productId);
            if (p && (p.stock == null || p.stock <= 0)) {
                await fetch(`${API_BASE}/products/${productId}/stock?stock=50`, { method: 'PUT' });
            }
        }
        showNotification(`Product #${productId} is now ${targetStatus === 'ACTIVE' ? 'In Stock (Active)' : 'Out of Stock'}.`, 'success');
        await loadProducts();
    } catch (err) {
        showNotification('Error updating status: ' + err.message, 'error');
    }
}

// Full Edit Product Modal ("and all")
function openEditProductModal(productId) {
    const prod = state.products.find(p => p.productId === productId);
    if (!prod) return;

    document.getElementById('editProductId').value = prod.productId;
    document.getElementById('editProductName').value = prod.productName || '';
    document.getElementById('editProductCategory').value = prod.category || '';
    document.getElementById('editProductType').value = prod.productType || '';
    document.getElementById('editProductPrice').value = prod.price || 0;
    document.getElementById('editProductStock').value = prod.stock != null ? prod.stock : 50;
    document.getElementById('editProductStatus').value = prod.status === 'OUT_OF_STOCK' ? 'OUT_OF_STOCK' : 'ACTIVE';
    document.getElementById('editProductImageUrl').value = (prod.image && prod.image.length > 0) ? prod.image[0] : '';
    document.getElementById('editProductDesc').value = prod.description || '';

    document.getElementById('editProductModal').classList.add('active');
}

function closeEditProductModal() {
    document.getElementById('editProductModal').classList.remove('active');
}

async function handleSaveEditProduct(e) {
    e.preventDefault();
    const btn = document.getElementById('btnSaveEditProduct');
    const productId = parseInt(document.getElementById('editProductId').value);
    const productName = document.getElementById('editProductName').value.trim();
    const category = document.getElementById('editProductCategory').value.trim();
    const productType = document.getElementById('editProductType').value.trim();
    const price = parseFloat(document.getElementById('editProductPrice').value);
    const stock = parseInt(document.getElementById('editProductStock').value) || 0;
    const status = document.getElementById('editProductStatus').value;
    const imageUrl = document.getElementById('editProductImageUrl').value.trim();
    const description = document.getElementById('editProductDesc').value.trim();

    const payload = {
        productId,
        productName,
        category,
        productType,
        price,
        stock: Math.max(0, stock),
        status,
        description,
        merchantId: getMerchantId(),
        sellerName: getMerchantName(),
        image: imageUrl ? [imageUrl] : []
    };

    try {
        btn.disabled = true;
        btn.textContent = 'Saving Changes...';

        const res = await fetch(`${API_BASE}/products`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!res.ok) throw new Error('Failed to update product');
        showNotification(`✅ Product #${productId} successfully updated!`, 'success');
        closeEditProductModal();
        await loadProducts();
    } catch (err) {
        alert('Failed to save product changes: ' + err.message);
    } finally {
        btn.disabled = false;
        btn.textContent = 'Save Changes';
    }
}

// 4. Delete Product
async function handleDeleteProduct(productId) {
    if (!confirm(`Are you sure you want to remove Product #${productId} from the store catalog?`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/products/${productId}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Delete failed');
        showNotification(`Product #${productId} deleted.`, 'info');
        await loadProducts();
        await loadDashboard();
    } catch (err) {
        showNotification('Failed to delete product: ' + err.message, 'error');
    }
}

// 5. Load Customer Orders
async function loadOrders() {
    try {
        let res = await fetch(`${API_BASE}/orders`);
        if (res.ok) {
            state.orders = await res.json();
            renderOrdersTable();
            const badge = document.getElementById('ordersCountBadge');
            if (badge) badge.textContent = state.orders.length;
        }
    } catch (err) {
        console.error('Orders error:', err);
    }
}

function handleOrderSearch(query) {
    state.orderSearchQuery = (query || '').trim().toLowerCase();
    state.orderCurrentPage = 1;
    const clearBtn = document.getElementById('orderSearchClearBtn');
    if (clearBtn) {
        clearBtn.style.display = state.orderSearchQuery ? 'block' : 'none';
    }
    renderOrdersTable();
}

function clearOrderSearch() {
    const input = document.getElementById('orderSearchInput');
    if (input) input.value = '';
    handleOrderSearch('');
}

function goToOrderPage(page) {
    state.orderCurrentPage = page;
    renderOrdersTable();
}

function renderOrdersTable() {
    const tbody = document.getElementById('ordersTableBody');
    const emptyState = document.getElementById('ordersEmptyState');
    const paginationBar = document.getElementById('ordersPaginationBar');
    const paginationInfo = document.getElementById('ordersPaginationInfo');
    const paginationControls = document.getElementById('ordersPaginationControls');
    const summaryEl = document.getElementById('ordersCountSummary');
    if (!tbody) return;

    // 1. Filter by Status
    let filtered = state.orders || [];
    if (state.activeStatusFilter !== 'ALL') {
        filtered = filtered.filter(o => {
            const st = (o.orderStatus || '').toUpperCase();
            if (state.activeStatusFilter === 'SHIPPED') {
                return st === 'SHIPPED' || st === 'SHIPPING';
            }
            if (state.activeStatusFilter === 'OUT_FOR_DELIVERY') {
                return st === 'OUT_FOR_DELIVERY' || st.includes('DELIVERY');
            }
            return st === state.activeStatusFilter;
        });
    }

    // 2. Filter by Search Query
    if (state.orderSearchQuery) {
        const q = state.orderSearchQuery.replace('#', '');
        filtered = filtered.filter(o => {
            const id = String(o.orderId || '');
            const cust = (o.customerName || (o.address && o.address.fullName) || ('Customer #' + o.customerId) || '').toLowerCase();
            const prod = (o.product?.productName || '').toLowerCase();
            const st = (o.orderStatus || '').toLowerCase();
            const pay = (o.modeOfPayment || '').toLowerCase();
            const city = (o.address?.city || '').toLowerCase();
            const partner = (o.deliveryPartnerName || '').toLowerCase();
            return id.includes(q) || cust.includes(q) || prod.includes(q) || st.includes(q) || pay.includes(q) || city.includes(q) || partner.includes(q);
        });
    }

    const totalOrders = filtered.length;
    const limit = state.orderPageLimit || 5;
    const totalPages = Math.max(1, Math.ceil(totalOrders / limit));

    if (summaryEl) {
        summaryEl.textContent = `Found ${totalOrders} order${totalOrders === 1 ? '' : 's'}`;
    }

    if (state.orderCurrentPage > totalPages) {
        state.orderCurrentPage = totalPages;
    }
    if (state.orderCurrentPage < 1) {
        state.orderCurrentPage = 1;
    }

    if (totalOrders === 0) {
        tbody.innerHTML = '';
        if (emptyState) emptyState.style.display = 'block';
        if (paginationBar) paginationBar.style.display = 'none';
        return;
    }

    if (emptyState) emptyState.style.display = 'none';
    if (paginationBar) paginationBar.style.display = 'flex';

    // 3. Slice page items (Limit 5)
    const startIdx = (state.orderCurrentPage - 1) * limit;
    const endIdx = startIdx + limit;
    const pageItems = filtered.slice(startIdx, endIdx);

    tbody.innerHTML = pageItems.map(ord => {
        const status = ord.orderStatus || 'PLACED';
        const badgeClass = getStatusBadgeClass(status);
        const prodName = ord.product?.productName || 'Store Catalog Item';
        const qty = ord.quantity || 1;
        const total = Number(ord.ammountPaid || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
        const date = ord.orderDate || 'Recent';
        const partnerName = ord.deliveryPartnerName;

        const partnerBadge = partnerName ? `
            <div style="font-size: 0.76rem; color: #15803d; font-weight: 700; margin-top: 3px; display: inline-flex; align-items: center; gap: 3px;">
                <span>🛵</span> ${escapeHtml(partnerName)}
            </div>
        ` : '';

        const isReturnReq = (status === 'RETURN_REQUESTED');
        const returnNotice = (isReturnReq && ord.returnReason) ? `
            <div style="font-size: 0.74rem; background: #fffbeb; color: #b45309; padding: 2px 6px; border-radius: 4px; border: 1px solid #fde68a; margin-top: 3px; max-width: 180px;">
                ⚠️ <strong>Mistake:</strong> ${escapeHtml(ord.returnReason)}
            </div>
        ` : '';

        const actionText = isReturnReq ? '🔄 Review Return' : (partnerName ? '📋 Fulfillment Info' : '🛵 Assign Partner');
        const actionClass = isReturnReq ? 'btn-warning' : (partnerName ? 'btn-outline' : 'btn-primary');

        return `
            <tr onclick="openOrderModal(${ord.orderId})" title="Click to view tracking or assign courier" style="cursor: pointer;">
                <td>
                    <strong style="color: #0f172a; font-size: 0.95rem;">#${ord.orderId}</strong>
                </td>
                <td style="color: #64748b; font-size: 0.88rem;">${escapeHtml(date)}</td>
                <td>
                    <div style="font-weight: 600; color: #1e293b;">${escapeHtml(prodName)}</div>
                    <span style="font-size: 0.8rem; color: #64748b;">Qty: ${qty}</span>
                </td>
                <td>
                    <span style="font-weight: 600; color: #334155;">${escapeHtml(ord.customerName || (ord.address && ord.address.fullName) || ('Customer #' + ord.customerId))}</span>
                </td>
                <td>
                    <strong style="color: #1d4ed8;">Rs. ${total}</strong>
                </td>
                <td>
                    <span style="font-size: 0.82rem; font-weight: 600; background: #f1f5f9; padding: 0.2rem 0.45rem; border-radius: 4px;">
                        ${escapeHtml(ord.modeOfPayment || 'WALLET')}
                    </span>
                </td>
                <td>
                    <span class="status-badge ${badgeClass}">${escapeHtml(status)}</span>
                    ${returnNotice}
                    ${partnerBadge}
                </td>
                <td style="text-align: right;">
                    <button class="btn btn-sm ${actionClass}" onclick="event.stopPropagation(); openOrderModal(${ord.orderId});">
                        ${actionText}
                    </button>
                </td>
            </tr>
        `;
    }).join('');

    if (paginationInfo) {
        paginationInfo.textContent = `Showing ${startIdx + 1} to ${Math.min(endIdx, totalOrders)} of ${totalOrders} orders`;
    }
    if (paginationControls) {
        paginationControls.innerHTML = renderPaginationButtons(
            state.orderCurrentPage,
            totalPages,
            'goToOrderPage'
        );
    }
}

// Generic Pagination Buttons Builder
function renderPaginationButtons(currentPage, totalPages, goToFunc) {
    if (totalPages <= 1) {
        return `<button type="button" class="pagination-btn active" disabled>1</button>`;
    }

    let html = '';
    html += `<button type="button" class="pagination-btn" ${currentPage === 1 ? 'disabled' : ''} onclick="${goToFunc}(${currentPage - 1})" title="Previous Page">&laquo; Prev</button>`;

    const maxButtons = 5;
    let startPage = Math.max(1, currentPage - 2);
    let endPage = Math.min(totalPages, startPage + maxButtons - 1);
    if (endPage - startPage + 1 < maxButtons) {
        startPage = Math.max(1, endPage - maxButtons + 1);
    }

    if (startPage > 1) {
        html += `<button type="button" class="pagination-btn" onclick="${goToFunc}(1)">1</button>`;
        if (startPage > 2) {
            html += `<span style="padding: 0 4px; color: #94a3b8;">...</span>`;
        }
    }

    for (let p = startPage; p <= endPage; p++) {
        html += `<button type="button" class="pagination-btn ${p === currentPage ? 'active' : ''}" onclick="${goToFunc}(${p})">${p}</button>`;
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) {
            html += `<span style="padding: 0 4px; color: #94a3b8;">...</span>`;
        }
        html += `<button type="button" class="pagination-btn" onclick="${goToFunc}(${totalPages})">${totalPages}</button>`;
    }

    html += `<button type="button" class="pagination-btn" ${currentPage === totalPages ? 'disabled' : ''} onclick="${goToFunc}(${currentPage + 1})" title="Next Page">Next &raquo;</button>`;

    return html;
}

// Global window bindings for inline HTML onclick handlers
window.goToOrderPage = goToOrderPage;
window.goToWalletPage = goToWalletPage;
window.handleOrderSearch = handleOrderSearch;
window.clearOrderSearch = clearOrderSearch;
window.renderOrdersTable = renderOrdersTable;
window.renderWalletTable = renderWalletTable;
window.handleMerchantDeposit = handleMerchantDeposit;

// 7. Merchant Wallet & Settlement History
async function loadMerchantWallet() {
    if (!state.currentMerchant) return;
    const mId = state.currentMerchant.profileId || 1;

    try {
        const balRes = await fetch(`${API_BASE}/${mId}/wallet`);
        if (balRes.ok) {
            const data = await balRes.json();
            state.walletBalance = data.balance != null ? Number(data.balance) : 0.0;
        }

        const stmtsRes = await fetch(`${API_BASE}/${mId}/wallet/statements`);
        if (stmtsRes.ok) {
            state.walletStatements = await stmtsRes.json();
        } else {
            state.walletStatements = [];
        }

        renderWalletTable();
    } catch (err) {
        console.error('Wallet error:', err);
    }
}

function renderWalletTable() {
    const balDisplay = document.getElementById('walletBalanceDisplay');
    if (balDisplay) {
        balDisplay.textContent = `Rs. ${state.walletBalance.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`;
    }

    const tbody = document.getElementById('walletTableBody');
    const emptyState = document.getElementById('walletEmptyState');
    const paginationBar = document.getElementById('walletPaginationBar');
    const paginationInfo = document.getElementById('walletPaginationInfo');
    const paginationControls = document.getElementById('walletPaginationControls');
    if (!tbody) return;

    const list = state.walletStatements || [];
    const total = list.length;
    const limit = state.walletPageLimit || 10;
    const totalPages = Math.max(1, Math.ceil(total / limit));

    if (state.walletCurrentPage > totalPages) {
        state.walletCurrentPage = totalPages;
    }
    if (state.walletCurrentPage < 1) {
        state.walletCurrentPage = 1;
    }

    if (total === 0) {
        tbody.innerHTML = '';
        if (emptyState) emptyState.style.display = 'block';
        if (paginationBar) paginationBar.style.display = 'none';
        return;
    }

    if (emptyState) emptyState.style.display = 'none';
    if (paginationBar) paginationBar.style.display = 'flex';

    // 10-item limit pagination
    const startIdx = (state.walletCurrentPage - 1) * limit;
    const endIdx = startIdx + limit;
    const pageItems = list.slice(startIdx, endIdx);

    tbody.innerHTML = pageItems.map(stmt => {
        const id = stmt.statementId != null ? stmt.statementId : '-';
        const type = (stmt.transactionType || 'SETTLEMENT').toUpperCase();
        const amt = Number(stmt.amount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
        const isCredit = type === 'DEPOSIT' || type === 'SETTLEMENT' || type === 'CREDIT';
        const amtColor = isCredit ? '#16a34a' : '#dc2626';
        const amtSign = isCredit ? '+' : '-';
        const orderId = stmt.orderId ? `#${stmt.orderId}` : '<span style="color: #94a3b8;">General</span>';
        const date = stmt.dateTime ? stmt.dateTime.replace('T', ' ').substring(0, 19) : 'Recent';
        const remarks = stmt.transactionRemarks || 'Merchant Transaction';

        return `
            <tr>
                <td><strong style="color: #0f172a;">#${id}</strong></td>
                <td>
                    <span style="font-size: 0.78rem; font-weight: 700; padding: 0.2rem 0.5rem; border-radius: 4px; background: ${isCredit ? '#dcfce7; color: #166534;' : '#fee2e2; color: #991b1b;'}">
                        ${escapeHtml(type)}
                    </span>
                </td>
                <td><strong style="color: ${amtColor};">${amtSign} Rs. ${amt}</strong></td>
                <td>${orderId}</td>
                <td style="color: #64748b; font-size: 0.88rem;">${escapeHtml(date)}</td>
                <td style="color: #334155; font-size: 0.88rem;">${escapeHtml(remarks)}</td>
            </tr>
        `;
    }).join('');

    if (paginationInfo) {
        paginationInfo.textContent = `Showing ${startIdx + 1} to ${Math.min(endIdx, total)} of ${total} transactions`;
    }
    if (paginationControls) {
        paginationControls.innerHTML = renderPaginationButtons(
            state.walletCurrentPage,
            totalPages,
            'goToWalletPage'
        );
    }
}

function goToWalletPage(page) {
    state.walletCurrentPage = page;
    renderWalletTable();
}

async function handleMerchantDeposit() {
    const amtInput = document.getElementById('quickDepositAmount');
    const alertEl = document.getElementById('walletAlert');
    if (!amtInput) return;
    const val = parseFloat(amtInput.value);
    if (!val || val <= 0) {
        if (alertEl) {
            alertEl.className = 'alert alert-danger';
            alertEl.textContent = 'Please enter a valid deposit amount greater than 0';
            alertEl.style.display = 'block';
        }
        return;
    }

    if (!state.currentMerchant) return;
    const mId = state.currentMerchant.profileId || 1;

    try {
        const res = await fetch(`${API_BASE}/${mId}/wallet/deposit?amount=${val}&remarks=Merchant+Settlement+Recharge`, {
            method: 'POST'
        });
        if (res.ok) {
            amtInput.value = '';
            if (alertEl) {
                alertEl.className = 'alert alert-success';
                alertEl.textContent = `Successfully added Rs. ${val.toFixed(2)} to your Merchant Settlement Wallet!`;
                alertEl.style.display = 'block';
                setTimeout(() => { alertEl.style.display = 'none'; }, 4000);
            }
            await loadMerchantWallet();
        }
    } catch (e) {
        if (alertEl) {
            alertEl.className = 'alert alert-danger';
            alertEl.textContent = 'Could not process deposit: ' + e.message;
            alertEl.style.display = 'block';
        }
    }
}

// 6. Open Order Tracking & Status Modal
async function openOrderModal(orderId) {
    let order = state.orders.find(o => o.orderId === orderId);

    try {
        const res = await fetch(`${API_BASE}/orders/${orderId}`);
        if (res.ok) {
            order = await res.json();
            const idx = state.orders.findIndex(o => o.orderId === orderId);
            if (idx >= 0) state.orders[idx] = order;
        }
    } catch (ignored) {}

    if (!order) {
        showNotification('Order not found', 'error');
        return;
    }

    state.selectedOrder = order;

    document.getElementById('modalOrderId').textContent = '#' + order.orderId;
    document.getElementById('modalOrderDate').textContent = order.orderDate || 'Recent';

    const prodName = order.product?.productName || 'Catalog Product';
    document.getElementById('modalProductName').textContent = prodName;
    document.getElementById('modalProductQty').textContent = (order.quantity || 1) + ' Item(s)';
    document.getElementById('modalAmountPaid').textContent = 'Rs. ' + Number(order.ammountPaid || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
    document.getElementById('modalPaymentMode').textContent = order.modeOfPayment || 'WALLET';
    document.getElementById('modalCustomerId').textContent = order.customerName || (order.address && order.address.fullName) || ('Customer #' + order.customerId);

    const addr = order.address;
    let addrStr = 'Customer Standard Shipping Address';
    if (addr) {
        addrStr = [
            addr.streetName || addr.flatNumber,
            addr.city,
            addr.state,
            addr.pincode ? `PIN: ${addr.pincode}` : ''
        ].filter(Boolean).join(', ');
    }
    document.getElementById('modalShippingAddress').textContent = addrStr;

    renderTrackingStepper(order.orderStatus);

    populateDeliveryPartnerSelect();

    const alreadyAssigned = Boolean(order.deliveryPartnerName || order.deliveryPartnerId);
    const assignedCard = document.getElementById('alreadyAssignedCard');
    const partnerSelect = document.getElementById('deliveryPartnerSelect');
    const btn = document.getElementById('btnAssignPartner');

    if (assignedCard) {
        if (alreadyAssigned) {
            assignedCard.style.display = 'block';
            document.getElementById('assignedPartnerNameDisplay').textContent = order.deliveryPartnerName || 'Assigned Delivery Partner';
            const phoneStr = order.deliveryPartnerPhone ? ` • 📞 ${order.deliveryPartnerPhone}` : '';
            document.getElementById('assignedPartnerDetailsDisplay').textContent = 
                `Assigned: ${order.deliveryAssignedDate || 'Recent'}${phoneStr} • Milestones managed via Delivery Hub`;
            
            if (partnerSelect && order.deliveryPartnerId) {
                partnerSelect.value = order.deliveryPartnerId;
                updatePartnerStatusNotice(order.deliveryPartnerId);
            }
            const portalLink = document.getElementById('assignedPartnerPortalLink');
            if (portalLink) {
                portalLink.href = `http://localhost:8088/?partnerId=${order.deliveryPartnerId || 1}&orderId=${order.orderId}`;
                portalLink.textContent = `🚚 Open Delivery Hub (Log in as ${order.deliveryPartnerName || 'Courier'}) →`;
            }
            if (btn) btn.textContent = '🔄 Re-assign Delivery Partner';
        } else {
            assignedCard.style.display = 'none';
            if (btn) btn.textContent = '🚀 Assign Partner & Notify';
        }
    }

    // Return review handling
    const returnCard = document.getElementById('returnReviewCard');
    const returnReasonDisp = document.getElementById('modalReturnReasonDisplay');
    const deliverySection = document.getElementById('deliveryAssignmentSection');

    if (order.orderStatus === 'RETURN_REQUESTED') {
        if (returnCard) returnCard.style.display = 'block';
        if (returnReasonDisp) returnReasonDisp.textContent = order.returnReason || 'Product mistake reported by customer';
        if (deliverySection) deliverySection.style.display = 'none';

        // Render customer shared location map details
        const addrDisp = document.getElementById('modalReturnAddressDisplay');
        const coordDisp = document.getElementById('modalReturnCoordinatesDisplay');
        const mapLink = document.getElementById('modalReturnMapLink');

        const addr = order.returnLocationAddress || (order.address ? `${order.address.streetName || ''}, ${order.address.city || ''} ${order.address.state || ''}` : 'Customer delivery address on file');
        if (addrDisp) addrDisp.textContent = addr;

        if (order.returnLatitude && order.returnLongitude) {
            if (coordDisp) coordDisp.textContent = `Lat: ${order.returnLatitude.toFixed(5)}, Lng: ${order.returnLongitude.toFixed(5)}`;
        } else {
            if (coordDisp) coordDisp.textContent = 'GPS location not shared (using shipping address)';
        }

        const mapUrl = order.returnLocationMapUrl || (order.returnLatitude && order.returnLongitude ? `https://www.google.com/maps?q=${order.returnLatitude},${order.returnLongitude}` : `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(addr)}`);
        if (mapLink) {
            mapLink.href = mapUrl;
            mapLink.style.display = 'inline-flex';
        }

        // Populate returnDeliveryPartnerSelect with available couriers
        const retPartnerSel = document.getElementById('returnDeliveryPartnerSelect');
        if (retPartnerSel && state.deliveryPartners) {
            const sortedPartners = [...state.deliveryPartners].sort((a, b) => {
                const order = { 'AVAILABLE': 1, 'BUSY': 2, 'OFFLINE': 3 };
                const sa = order[(a.status || 'AVAILABLE').toUpperCase()] || 4;
                const sb = order[(b.status || 'AVAILABLE').toUpperCase()] || 4;
                return sa - sb;
            });
            retPartnerSel.innerHTML = sortedPartners.map(p => {
                const st = (p.status || 'AVAILABLE').toUpperCase();
                const icon = st === 'AVAILABLE' ? '🟢 [Available]' : (st === 'BUSY' ? '🟡 [Busy]' : '⚪ [Offline]');
                return `<option value="${p.partnerId}" ${order.deliveryPartnerId === p.partnerId ? 'selected' : ''}>
                    ${icon} ${p.partnerName} (${p.vehicleType || 'Courier'}) - Zone: ${p.serviceZone || 'All Areas'}
                </option>`;
            }).join('');
        }
    } else {
        if (returnCard) returnCard.style.display = 'none';
        if (deliverySection) deliverySection.style.display = 'block';
    }

    const modalAlert = document.getElementById('modalAlert');
    if (modalAlert) modalAlert.style.display = 'none';

    document.getElementById('orderModal').classList.add('active');
}

// Render Visual Stepper
function renderTrackingStepper(statusStr) {
    const container = document.getElementById('trackingStepper');
    if (!container) return;

    const raw = (statusStr || 'PLACED').toUpperCase().replace('-', '_');
    let activeIndex = 0;

    if (raw === 'CONFIRMED') activeIndex = 1;
    else if (raw === 'SHIPPED' || raw === 'SHIPPING') activeIndex = 2;
    else if (raw === 'OUT_FOR_DELIVERY' || raw.includes('DELIVERY')) activeIndex = 3;
    else if (raw === 'DELIVERED') activeIndex = 4;
    else if (raw === 'CANCELLED') activeIndex = -1;

    if (raw === 'RETURN_REQUESTED') {
        container.innerHTML = `
            <div style="width: 100%; text-align: center; padding: 0.85rem; background: #fffbeb; color: #92400e; border: 1px solid #fde68a; border-radius: 8px; font-weight: 700;">
                🔄 Return Request Pending: Customer received product and requested return to merchant.
            </div>
        `;
        return;
    }

    if (raw === 'REFUNDED' || raw === 'RETURNED') {
        container.innerHTML = `
            <div style="width: 100%; text-align: center; padding: 0.85rem; background: #f0fdf4; color: #166534; border: 1px solid #bbf7d0; border-radius: 8px; font-weight: 700;">
                ✅ Return Accepted &amp; Refunded: Product returned to merchant and customer E-Wallet refunded.
            </div>
        `;
        return;
    }

    if (raw === 'CANCELLED') {
        container.innerHTML = `
            <div style="width: 100%; text-align: center; padding: 0.75rem; background: #fee2e2; color: #991b1b; border-radius: 8px; font-weight: 700;">
                ⚠️ This order was CANCELLED.
            </div>
        `;
        return;
    }

    container.innerHTML = TRACKING_STEPS.map((step, idx) => {
        const isCompleted = idx < activeIndex || (idx === 4 && activeIndex === 4);
        const isActive = idx === activeIndex && activeIndex !== 4;

        let dotClass = '';
        let dotContent = step.icon;
        let labelClass = '';

        if (isCompleted) {
            dotClass = 'completed';
            dotContent = '✓';
            labelClass = 'completed';
        } else if (isActive) {
            dotClass = 'active';
            labelClass = 'active';
        }

        return `
            <div class="tracker-step">
                <div class="tracker-dot ${dotClass}">
                    ${dotContent}
                </div>
                <div class="tracker-label ${labelClass}">
                    ${escapeHtml(step.label)}
                </div>
            </div>
        `;
    }).join('');
}

// 7. Load Delivery Partners
async function loadDeliveryPartners() {
    try {
        const res = await fetch(`${API_BASE}/delivery-partners`);
        if (res.ok) {
            state.deliveryPartners = await res.json();
            populateDeliveryPartnerSelect();
        }
    } catch (err) {
        console.error('Failed to load delivery partners:', err);
    }
}

function populateDeliveryPartnerSelect() {
    const sel = document.getElementById('deliveryPartnerSelect');
    if (!sel) return;
    if (!state.deliveryPartners || state.deliveryPartners.length === 0) {
        sel.innerHTML = '<option value="1">🟢 [Available] Alex Rivera (Express Bike) - Central Metro</option>';
        updatePartnerStatusNotice(1);
        return;
    }

    // Sort: AVAILABLE first, then BUSY, then OFFLINE
    const sorted = [...state.deliveryPartners].sort((a, b) => {
        const order = { 'AVAILABLE': 1, 'BUSY': 2, 'OFFLINE': 3 };
        const sa = order[(a.status || 'AVAILABLE').toUpperCase()] || 4;
        const sb = order[(b.status || 'AVAILABLE').toUpperCase()] || 4;
        return sa - sb;
    });

    sel.innerHTML = sorted.map(p => {
        const st = (p.status || 'AVAILABLE').toUpperCase();
        let badge = '🟢 [Available]';
        if (st === 'BUSY') badge = '🟡 [Busy - In Transit]';
        else if (st === 'OFFLINE') badge = '⚪ [Offline]';

        return `<option value="${p.partnerId}" data-status="${st}">
            ${badge} ${escapeHtml(p.partnerName)} (${p.vehicleType || 'Courier'}) - Zone: ${escapeHtml(p.serviceZone || 'All Zones')}
        </option>`;
    }).join('');

    const initialId = sel.value ? parseInt(sel.value) : sorted[0]?.partnerId;
    updatePartnerStatusNotice(initialId);
}

function handlePartnerSelectChange(partnerId) {
    updatePartnerStatusNotice(parseInt(partnerId));
}

function updatePartnerStatusNotice(partnerId) {
    const noticeCard = document.getElementById('partnerStatusNoticeCard');
    const noticeIcon = document.getElementById('partnerNoticeIcon');
    const noticeText = document.getElementById('partnerNoticeText');
    const miniBadge = document.getElementById('partnerStatusMiniBadge');

    if (!noticeCard) return;

    const partner = (state.deliveryPartners || []).find(p => p.partnerId === partnerId);
    const status = ((partner && partner.status) || 'AVAILABLE').toUpperCase();

    if (status === 'AVAILABLE') {
        noticeCard.style.background = '#f0fdf4';
        noticeCard.style.borderColor = '#bbf7d0';
        noticeCard.style.color = '#166534';
        if (noticeIcon) noticeIcon.textContent = '🟢';
        if (noticeText) noticeText.textContent = `Online & Available for immediate pickup (${partner ? partner.serviceZone : 'Operational'}).`;
        if (miniBadge) {
            miniBadge.textContent = '🟢 Available';
            miniBadge.style.background = '#dcfce7';
            miniBadge.style.color = '#15803d';
        }
    } else if (status === 'BUSY') {
        noticeCard.style.background = '#fefce8';
        noticeCard.style.borderColor = '#fef08a';
        noticeCard.style.color = '#854d0e';
        if (noticeIcon) noticeIcon.textContent = '🟡';
        if (noticeText) noticeText.textContent = 'Courier is currently delivering other orders. Pickup may take slightly longer.';
        if (miniBadge) {
            miniBadge.textContent = '🟡 Busy';
            miniBadge.style.background = '#fef9c3';
            miniBadge.style.color = '#a16207';
        }
    } else {
        // OFFLINE
        noticeCard.style.background = '#fef2f2';
        noticeCard.style.borderColor = '#fecaca';
        noticeCard.style.color = '#991b1b';
        if (noticeIcon) noticeIcon.textContent = '⚠️';
        if (noticeText) noticeText.textContent = 'Courier is currently OFFLINE / Off-Duty. Assignment will wait until they come online.';
        if (miniBadge) {
            miniBadge.textContent = '⚪ Offline';
            miniBadge.style.background = '#f1f5f9';
            miniBadge.style.color = '#64748b';
        }
    }
}

// 8. Handle Delivery Partner Assignment
async function handleAssignDeliveryPartner() {
    if (!state.selectedOrder) return;

    const partnerSelect = document.getElementById('deliveryPartnerSelect');
    const remarksInput = document.getElementById('assignRemarksInput');
    const assignBtn = document.getElementById('btnAssignPartner');
    const modalAlert = document.getElementById('modalAlert');

    const partnerId = partnerSelect ? parseInt(partnerSelect.value) : 1;
    const remarks = remarksInput ? remarksInput.value.trim() : '';

    const selectedPartnerInfo = (state.deliveryPartners || []).find(p => p.partnerId === partnerId);
    if (selectedPartnerInfo && (selectedPartnerInfo.status || '').toUpperCase() === 'OFFLINE') {
        const confirmOffline = confirm(
            `⚠️ Notice: ${selectedPartnerInfo.partnerName} is currently OFFLINE / Off-Duty.\n\n` +
            `The assignment will be queued, but they may take longer to acknowledge and pick up.\n\n` +
            `Do you still want to proceed with assigning this courier?`
        );
        if (!confirmOffline) {
            return;
        }
    }

    try {
        assignBtn.disabled = true;
        assignBtn.textContent = 'Assigning & Notifying...';

        const orderId = state.selectedOrder.orderId;
        let assignment = null;

        // 1. Direct assignment to Delivery Partner Service (Port 8088)
        try {
            const dpRes = await fetch('http://localhost:8088/delivery/assign', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orderId: orderId, partnerId: partnerId, remarks: remarks })
            });
            if (dpRes.ok) {
                assignment = await dpRes.json();
            }
        } catch (dpErr) {
            console.warn('Direct delivery service call warning:', dpErr);
        }

        // 2. Merchant service assignment & status confirmation (Port 8087)
        try {
            const res = await fetch(`${API_BASE}/orders/${orderId}/assign-delivery?partnerId=${partnerId}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ orderId: orderId, partnerId: partnerId, remarks: remarks })
            });
            if (res.ok && !assignment) {
                assignment = await res.json();
            }
        } catch (mErr) {
            console.warn('Merchant service assign call warning:', mErr);
        }

        const selectedPartner = (state.deliveryPartners || []).find(p => p.partnerId === partnerId);
        const pName = (assignment && assignment.partnerName) ? assignment.partnerName : (selectedPartner ? selectedPartner.partnerName : 'Delivery Partner');

        state.selectedOrder.deliveryPartnerId = partnerId;
        state.selectedOrder.deliveryPartnerName = pName;
        if (modalAlert) {
            modalAlert.className = 'alert alert-success';
            modalAlert.innerHTML = `
                <div style="font-weight: 700; font-size: 0.95rem;">✅ Order #${orderId} assigned to ${escapeHtml(pName)}!</div>
                <div style="margin-top: 0.25rem; font-size: 0.85rem; color: #166534;">An automated notification has been dispatched to the courier. They can now fulfill this order.</div>
                <div style="margin-top: 0.65rem;">
                    <a href="http://localhost:8088/?partnerId=${partnerId}&orderId=${orderId}" target="_blank" class="btn btn-sm btn-primary" style="text-decoration: none; font-weight: 700;">
                        👉 Switch to Delivery Partner Hub (Log in as ${escapeHtml(pName)}) &rarr;
                    </a>
                </div>
            `;
            modalAlert.style.display = 'block';
        }

        renderTrackingStepper('CONFIRMED');

        const assignedCard = document.getElementById('alreadyAssignedCard');
        if (assignedCard) {
            assignedCard.style.display = 'block';
            document.getElementById('assignedPartnerNameDisplay').textContent = pName;
            document.getElementById('assignedPartnerDetailsDisplay').textContent = 'Assigned: Today • Live status managed by courier in Delivery Hub';
            const portalLink = document.getElementById('assignedPartnerPortalLink');
            if (portalLink) {
                portalLink.href = `http://localhost:8088/?partnerId=${partnerId}&orderId=${orderId}`;
                portalLink.textContent = `🚚 Open Delivery Hub (Log in as ${pName}) →`;
            }
        }

        await loadOrders();
        await loadDashboard();
    } catch (err) {
        showAlert(modalAlert, 'Failed to assign delivery partner: ' + err.message, 'error');
    } finally {
        assignBtn.disabled = false;
        assignBtn.textContent = '🔄 Re-assign Delivery Partner';
    }
}

// 9. Handle Return Request Approval & Rejection
async function handleApproveReturn() {
    if (!state.selectedOrder) return;
    const ordId = state.selectedOrder.orderId;
    const amt = Number(state.selectedOrder.ammountPaid || 0).toFixed(2);
    
    const partnerSelect = document.getElementById('returnDeliveryPartnerSelect');
    const partnerId = partnerSelect ? partnerSelect.value : null;
    const remarksInput = document.getElementById('returnPickupRemarksInput');
    const remarks = remarksInput ? remarksInput.value.trim() : 'Return item pickup assigned by merchant';

    if (!confirm(`Accept returned product for Order #${ordId}, assign courier for pickup, and process E-Wallet refund of Rs. ${amt} to customer?`)) {
        return;
    }

    try {
        let isApproved = false;
        const queryParams = new URLSearchParams();
        if (partnerId) queryParams.append('partnerId', partnerId);
        if (remarks) queryParams.append('remarks', remarks);

        // 1. Try via merchant-service API
        try {
            const res = await fetch(`${API_BASE}/orders/${ordId}/approve-return?${queryParams.toString()}`, { method: 'POST' });
            if (res.ok) isApproved = true;
        } catch (e1) {
            console.warn('Merchant approve-return warning:', e1);
        }

        // 2. Direct fallback to order-service
        if (!isApproved) {
            try {
                const res2 = await fetch(`http://localhost:8084/orders/${ordId}/approve-return?${queryParams.toString()}`, { method: 'POST' });
                if (res2.ok) isApproved = true;
            } catch (e2) {}
        }

        // 3. Fallback direct status update to REFUNDED
        if (!isApproved) {
            try {
                const res3 = await fetch(`http://localhost:8084/orders/${ordId}/status?status=REFUNDED`, { method: 'PUT' });
                if (res3.ok) isApproved = true;
            } catch (e3) {}
        }

        if (isApproved) {
            showNotification(`✅ Order #${ordId} return accepted! Courier assigned for pickup, Rs. ${amt} refunded to customer wallet, and stock restored.`, 'success');
            closeOrderModal();
            await loadOrders();
            await loadDashboard();
            if (typeof loadMerchantWallet === 'function') {
                await loadMerchantWallet();
            }
        } else {
            showNotification('Failed to approve return. Please check server connections.', 'error');
        }
    } catch (err) {
        showNotification('Error approving return: ' + err.message, 'error');
    }
}

async function handleRejectReturn() {
    if (!state.selectedOrder) return;
    const ordId = state.selectedOrder.orderId;
    const reason = prompt(`Please enter the reason for rejecting return for Order #${ordId}:`, "Product returned does not match policy / damaged by user");
    if (reason === null) {
        return; // Cancelled by merchant
    }

    try {
        let isRejected = false;
        const queryParams = new URLSearchParams();
        if (reason) queryParams.append('reason', reason);

        // 1. Try via merchant-service API
        try {
            const res = await fetch(`${API_BASE}/orders/${ordId}/reject-return?${queryParams.toString()}`, { method: 'POST' });
            if (res.ok) isRejected = true;
        } catch (e1) {
            console.warn('Merchant reject-return warning:', e1);
        }

        // 2. Direct fallback to order-service
        if (!isRejected) {
            try {
                const res2 = await fetch(`http://localhost:8084/orders/${ordId}/reject-return?${queryParams.toString()}`, { method: 'POST' });
                if (res2.ok) isRejected = true;
            } catch (e2) {}
        }

        // 3. Fallback direct status update to RETURN_REJECTED
        if (!isRejected) {
            try {
                const res3 = await fetch(`http://localhost:8084/orders/${ordId}/status?status=RETURN_REJECTED`, { method: 'PUT' });
                if (res3.ok) isRejected = true;
            } catch (e3) {}
        }

        if (isRejected) {
            showNotification(`Return request for Order #${ordId} was rejected. Order marked as RETURN_REJECTED.`, 'info');
            closeOrderModal();
            await loadOrders();
            await loadDashboard();
        } else {
            showNotification('Failed to reject return.', 'error');
        }
    } catch (err) {
        showNotification('Error rejecting return: ' + err.message, 'error');
    }
}

window.handleApproveReturn = handleApproveReturn;
window.handleRejectReturn = handleRejectReturn;

// Helpers
function normalizeDisplayStatus(status) {
    const s = status.trim().toLowerCase().replace(/-/g, ' ').replace(/_/g, ' ');
    if (s === 'shipping' || s === 'shipped') return 'SHIPPED';
    if (s === 'out for delivery' || s === 'outfordelivery') return 'OUT_FOR_DELIVERY';
    if (s === 'delivered') return 'DELIVERED';
    if (s === 'confirmed') return 'CONFIRMED';
    if (s === 'placed') return 'PLACED';
    if (s === 'cancelled' || s === 'canceled') return 'CANCELLED';
    return status.trim().toUpperCase();
}

function getStatusBadgeClass(status) {
    const s = (status || '').toLowerCase().replace(/_/g, '-');
    if (s.includes('shipped') || s.includes('shipping')) return 'status-shipped';
    if (s.includes('out-for-delivery') || s.includes('delivery')) return 'status-out-for-delivery';
    if (s.includes('delivered')) return 'status-delivered';
    if (s.includes('confirmed')) return 'status-confirmed';
    if (s.includes('cancelled')) return 'status-cancelled';
    return 'status-placed';
}

function showAlert(el, msg, type) {
    if (!el) return;
    el.style.display = 'block';
    el.className = `alert alert-${type === 'error' ? 'danger' : 'success'}`;
    el.textContent = msg;
}

function showNotification(msg, type = 'info') {
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

// Merchant Offer Management Functions
function openOfferModal(productId, productName, currentDiscount, currentTitle) {
    let modal = document.getElementById('merchantOfferModal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'merchantOfferModal';
        modal.style.position = 'fixed';
        modal.style.top = '0';
        modal.style.left = '0';
        modal.style.width = '100%';
        modal.style.height = '100%';
        modal.style.backgroundColor = 'rgba(0,0,0,0.5)';
        modal.style.display = 'flex';
        modal.style.alignItems = 'center';
        modal.style.justifyContent = 'center';
        modal.style.zIndex = '99999';
        document.body.appendChild(modal);
    }

    modal.innerHTML = `
        <div style="background: white; border-radius: 12px; padding: 1.5rem 2rem; max-width: 440px; width: 90%; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.2);">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; border-bottom: 1px solid #e2e8f0; padding-bottom: 0.75rem;">
                <h3 style="margin: 0; color: #1e293b; font-size: 1.2rem;">🏷️ Create / Edit Offer</h3>
                <button type="button" onclick="closeOfferModal()" style="background: none; border: none; font-size: 1.4rem; cursor: pointer; color: #64748b;">&times;</button>
            </div>
            <p style="font-size: 0.88rem; color: #64748b; margin-top: 0;">Product: <strong style="color: #1e293b;">${escapeHtml(productName)}</strong></p>
            
            <div style="margin-bottom: 1rem;">
                <label style="display: block; font-size: 0.82rem; font-weight: 700; color: #475569; margin-bottom: 0.35rem;">Offer Title / Badge Text</label>
                <input type="text" id="modalOfferTitle" class="form-control" value="${escapeHtml(currentTitle || 'Special Offer')}" placeholder="e.g. Weekend Deal, Flash Sale" />
            </div>

            <div style="margin-bottom: 1.25rem;">
                <label style="display: block; font-size: 0.82rem; font-weight: 700; color: #475569; margin-bottom: 0.35rem;">Discount Percentage (%)</label>
                <input type="number" id="modalOfferDiscount" class="form-control" min="1" max="90" step="1" value="${currentDiscount || 10}" />
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 0.75rem;">
                <button type="button" class="btn btn-outline" onclick="closeOfferModal()">Cancel</button>
                <button type="button" class="btn btn-primary" onclick="submitProductOffer(${productId})">Save Offer</button>
            </div>
        </div>
    `;
    modal.style.display = 'flex';
}

function closeOfferModal() {
    const modal = document.getElementById('merchantOfferModal');
    if (modal) modal.style.display = 'none';
}

async function submitProductOffer(productId) {
    const title = document.getElementById('modalOfferTitle')?.value.trim() || 'Special Offer';
    const discount = parseFloat(document.getElementById('modalOfferDiscount')?.value) || 10;

    try {
        const res = await fetch(`${API_BASE}/products/${productId}/offer?offerTitle=${encodeURIComponent(title)}&discountPercent=${discount}`, {
            method: 'POST'
        });
        if (!res.ok) throw new Error('Failed with HTTP ' + res.status);
        showNotification('✅ Offer applied successfully!', 'success');
        closeOfferModal();
        await loadProducts();
    } catch (e) {
        showNotification('Failed to apply offer: ' + e.message, 'error');
    }
}

async function handleRemoveProductOffer(productId) {
    if (!confirm('Are you sure you want to remove the offer from this product?')) return;
    try {
        const res = await fetch(`${API_BASE}/products/${productId}/offer`, {
            method: 'DELETE'
        });
        if (!res.ok) throw new Error('Failed with HTTP ' + res.status);
        showNotification('Offer removed successfully.', 'info');
        await loadProducts();
    } catch (e) {
        showNotification('Failed to remove offer: ' + e.message, 'error');
    }
}

