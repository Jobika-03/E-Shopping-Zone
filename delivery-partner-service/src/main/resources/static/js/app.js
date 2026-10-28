// ShopNexa Delivery Partner Portal - Simplified & Robust App Logic
let currentPartner = null;
let allPartners = [];
let partnerDeliveries = [];
let systemDeliveries = [];
let viewMode = 'MINE'; // 'MINE' or 'ALL_COURIERS'
let activeFilter = 'ALL';
let selectedOrderId = null;
let pendingStatus = null;
let partnerNotifications = [];
let dismissedBannerOrderId = null;
let pollInterval = null;
let knownOrderIds = new Set();

let targetOrderId = null;

document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

async function initApp() {
    await loadAllPartners();

    // 1. Check for URL Parameters (Deep Link from Merchant)
    const urlParams = new URLSearchParams(window.location.search);
    const paramPartnerId = urlParams.get('partnerId');
    const paramEmail = urlParams.get('email');
    const paramOrderId = urlParams.get('orderId');

    if (paramOrderId) {
        targetOrderId = parseInt(paramOrderId);
    }

    if (paramPartnerId) {
        const found = allPartners.find(p => p.partnerId === parseInt(paramPartnerId));
        if (found) {
            loginAsPartner(found);
            return;
        }
    } else if (paramEmail) {
        const found = allPartners.find(p => p.email && p.email.toLowerCase() === paramEmail.toLowerCase());
        if (found) {
            loginAsPartner(found);
            return;
        }
    }

    // 2. Check LocalStorage for previously active session
    const stored = localStorage.getItem('shopnexa_courier');
    if (stored) {
        try {
            currentPartner = JSON.parse(stored);
            renderDashboard();
            return;
        } catch (e) {
            localStorage.removeItem('shopnexa_courier');
        }
    }

    // 3. Fallback: Show Courier Sign In Screen
    renderAuthScreen();
}

async function loadAllPartners() {
    try {
        const res = await fetch('/delivery/partners');
        if (res.ok) {
            allPartners = await res.json();
        }
    } catch (e) {
        console.warn("Could not fetch delivery partners:", e);
    }
}

// -------------------------------------------------------------
// Authentication Flow (Simple & Immediate)
// -------------------------------------------------------------
function quickLogin(email, password) {
    const emailInput = document.getElementById('loginEmail');
    const pwdInput = document.getElementById('loginPassword');
    if (emailInput) emailInput.value = email;
    if (pwdInput) pwdInput.value = password;
    executeLogin(email, password);
}

async function handlePartnerLogin(e) {
    if (e) e.preventDefault();
    const email = document.getElementById('loginEmail')?.value.trim();
    const password = document.getElementById('loginPassword')?.value;
    if (!email || !password) return;
    executeLogin(email, password);
}

async function executeLogin(email, password) {
    const btn = document.getElementById('loginBtn');
    const alertBox = document.getElementById('authAlert');

    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Authenticating...';
    }
    if (alertBox) alertBox.style.display = 'none';

    try {
        const res = await fetch('/delivery/partners/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: email, password: password })
        });

        const data = await res.json();

        if (res.ok && (data.success || data.partnerId)) {
            const partner = {
                partnerId: data.partnerId,
                partnerName: data.partnerName,
                email: data.email,
                phone: data.phone,
                vehicleType: data.vehicleType,
                serviceZone: data.serviceZone || 'All Zones'
            };
            loginAsPartner(partner);
        } else {
            if (alertBox) {
                alertBox.className = 'alert alert-danger';
                alertBox.textContent = data.message || 'Invalid courier credentials. Please try again.';
                alertBox.style.display = 'block';
            }
        }
    } catch (err) {
        // Fallback: check if local partner list has matching email
        const localFound = allPartners.find(p => p.email && p.email.toLowerCase() === email.toLowerCase());
        if (localFound) {
            loginAsPartner(localFound);
        } else if (alertBox) {
            alertBox.className = 'alert alert-danger';
            alertBox.textContent = 'Could not sign in. Please verify your credentials.';
            alertBox.style.display = 'block';
        }
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = 'Sign In as Delivery Partner \u2192';
        }
    }
}

function loginAsPartner(partner) {
    currentPartner = {
        partnerId: partner.partnerId,
        partnerName: partner.partnerName,
        email: partner.email,
        phone: partner.phone,
        vehicleType: partner.vehicleType,
        serviceZone: partner.serviceZone || 'All Zones',
        status: (partner.status || 'AVAILABLE').toUpperCase()
    };
    localStorage.setItem('shopnexa_courier', JSON.stringify(currentPartner));
    renderDashboard();
}

function handleCourierLogout() {
    stopLiveSync();
    localStorage.removeItem('shopnexa_courier');
    currentPartner = null;
    partnerDeliveries = [];
    systemDeliveries = [];
    partnerNotifications = [];
    knownOrderIds.clear();
    dismissedBannerOrderId = null;

    const badge = document.getElementById('notifBadgeCount');
    if (badge) {
        badge.textContent = '0';
        badge.classList.add('empty');
    }
    const notifDropdown = document.getElementById('notifDropdown');
    if (notifDropdown) notifDropdown.classList.remove('active');

    renderAuthScreen();
}

function switchAuthTab(tab) {
    const btnSign = document.getElementById('tabBtnSignIn');
    const btnReg = document.getElementById('tabBtnRegister');
    const paneSign = document.getElementById('signInPane');
    const paneReg = document.getElementById('registerPane');

    if (tab === 'signin') {
        btnSign?.classList.add('active');
        btnReg?.classList.remove('active');
        if (paneSign) paneSign.style.display = 'block';
        if (paneReg) paneReg.style.display = 'none';
    } else {
        btnReg?.classList.add('active');
        btnSign?.classList.remove('active');
        if (paneReg) paneReg.style.display = 'block';
        if (paneSign) paneSign.style.display = 'none';
    }
}

async function handlePartnerRegister(e) {
    if (e) e.preventDefault();
    const name = document.getElementById('regName').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const phone = document.getElementById('regPhone').value.trim();
    const vehicle = document.getElementById('regVehicle').value;
    const zone = document.getElementById('regZone').value.trim();
    const password = document.getElementById('regPassword').value;

    try {
        const res = await fetch('/delivery/partners/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                partnerName: name,
                email: email,
                phone: phone,
                vehicleType: vehicle,
                serviceZone: zone,
                password: password,
                status: 'AVAILABLE'
            })
        });

        if (res.ok) {
            const registered = await res.json();
            loginAsPartner(registered);
        } else {
            alert('Registration failed. Please check your details.');
        }
    } catch (err) {
        alert('Server error registering partner.');
    }
}

// -------------------------------------------------------------
// Dashboard Rendering & Active Courier Switcher
// -------------------------------------------------------------
function renderAuthScreen() {
    stopLiveSync();
    document.getElementById('authSection').style.display = 'flex';
    document.getElementById('dashboardSection').style.display = 'none';
    document.getElementById('navLinksAuth').style.display = 'none';
    document.getElementById('navLinksGuest').style.display = 'flex';
}

function renderDashboard() {
    document.getElementById('authSection').style.display = 'none';
    document.getElementById('dashboardSection').style.display = 'block';
    document.getElementById('navLinksAuth').style.display = 'flex';
    document.getElementById('navLinksGuest').style.display = 'none';

    document.getElementById('headerPartnerName').textContent = currentPartner.partnerName || 'Courier';
    document.getElementById('headerPartnerZone').textContent = currentPartner.serviceZone || currentPartner.vehicleType || 'Courier Partner';

    // Sync duty status select
    const dutySel = document.getElementById('partnerDutyStatusSelect');
    if (dutySel) {
        const curStatus = (currentPartner.status || 'AVAILABLE').toUpperCase();
        dutySel.value = curStatus;
        dutySel.style.color = curStatus === 'AVAILABLE' ? '#16a34a' : (curStatus === 'BUSY' ? '#ca8a04' : '#64748b');
    }

    // Populate quick switcher in sub-navbar
    const sel = document.getElementById('partnerQuickSelect');
    if (sel && allPartners.length > 0) {
        sel.innerHTML = allPartners.map(p => `
            <option value="${p.partnerId}" ${p.partnerId === currentPartner.partnerId ? 'selected' : ''}>
                ${p.partnerName} (${p.vehicleType || 'Courier'})
            </option>
        `).join('');
    }

    loadDeliveries(false);
    loadNotifications(false);
    startLiveSync();
}

async function handlePartnerSwitch(partnerId) {
    const partner = allPartners.find(p => p.partnerId === parseInt(partnerId));
    if (!partner) return;

    dismissedBannerOrderId = null;
    loginAsPartner(partner);
    showDashboardAlert(`Switched active courier to ${partner.partnerName}`, 'info');
}

function setViewMode(mode) {
    viewMode = mode;
    const btnMine = document.getElementById('btnModeMine');
    const btnAll = document.getElementById('btnModeAll');

    if (mode === 'MINE') {
        btnMine?.classList.add('active');
        btnAll?.classList.remove('active');
    } else {
        btnAll?.classList.add('active');
        btnMine?.classList.remove('active');
    }

    renderDeliveries();
}

// -------------------------------------------------------------
// Load Deliveries & Live Assignment Banner (High Visibility)
// -------------------------------------------------------------
async function loadDeliveries(isSilent = false) {
    if (!currentPartner) return;

    try {
        const res = await fetch(`/delivery/partner/${currentPartner.partnerId}/deliveries`);
        if (res.ok) {
            const data = await res.json();

            // Detect brand new assigned order
            if (knownOrderIds.size > 0) {
                const newOrders = data.filter(d => !knownOrderIds.has(d.orderId));
                if (newOrders.length > 0) {
                    const first = newOrders[0];
                    playChime();
                    showDashboardAlert(`\uD83D\uDD14 New Order Assigned! Order #${first.orderId} (${first.productName}) assigned to you!`, 'success');
                    dismissedBannerOrderId = null; // Reopen banner for new order
                    loadNotifications(false);
                }
            }

            partnerDeliveries = data;
            knownOrderIds = new Set(partnerDeliveries.map(d => d.orderId));
            updateKPIs();
        }

        // Also fetch network deliveries
        try {
            const resSys = await fetch('/delivery/deliveries');
            if (resSys.ok) {
                systemDeliveries = await resSys.json();
                const countSysEl = document.getElementById('countSystemDeliveries');
                if (countSysEl) countSysEl.textContent = systemDeliveries.length;
            }
        } catch (ignored) {}

        const countMineEl = document.getElementById('countMineDeliveries');
        if (countMineEl) countMineEl.textContent = partnerDeliveries.length;

        renderBanner();
        renderDeliveries();

        // If deep-linked with ?orderId=XX, focus and auto-open the status modal for this order
        if (targetOrderId) {
            const foundOrder = partnerDeliveries.find(d => d.orderId === targetOrderId) || 
                               systemDeliveries.find(d => d.orderId === targetOrderId);
            if (foundOrder) {
                const currentStatus = (foundOrder.status || 'SHIPPED').toUpperCase();
                const nextSt = (currentStatus === 'ASSIGNED' || currentStatus === 'PLACED') ? 'SHIPPED' : 
                               (currentStatus === 'SHIPPED' ? 'OUT_FOR_DELIVERY' : 'DELIVERED');
                // Scroll row into view if present
                const rowEl = document.getElementById(`order-row-${targetOrderId}`);
                if (rowEl) {
                    rowEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    rowEl.style.outline = '3px solid #3b82f6';
                }
                openUpdateModal(targetOrderId, nextSt);
                targetOrderId = null; // Only auto-trigger once on initial load
            }
        }
    } catch (err) {
        if (!isSilent) console.error("Error loading deliveries:", err);
    }
}

// Render the high-visibility notification banner right at the top
function renderBanner() {
    const banner = document.getElementById('newAssignmentBanner');
    if (!banner || !currentPartner) return;

    // Find the latest order assigned to this courier that needs fulfillment action
    const assignedOrder = partnerDeliveries.find(d => {
        const st = (d.status || '').toUpperCase();
        return st === 'ASSIGNED' || st === 'PLACED' || st === 'CONFIRMED';
    });

    if (!assignedOrder || assignedOrder.orderId === dismissedBannerOrderId) {
        banner.style.display = 'none';
        return;
    }

    const orderId = assignedOrder.orderId;
    const prodName = assignedOrder.productName || 'Catalog Item';
    const custName = assignedOrder.customerName || 'Valued Customer';
    const address = assignedOrder.shippingAddress || 'Address on file';
    const total = Number(assignedOrder.totalAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });

    document.getElementById('bannerOrderHeading').textContent = `Order #${orderId} - ${prodName}`;
    document.getElementById('bannerCustName').textContent = custName;
    document.getElementById('bannerCustAddress').textContent = address;
    document.getElementById('bannerAmount').textContent = `Rs. ${total}`;

    // Direct 1-click status update action buttons right in the banner!
    const actionsContainer = document.getElementById('bannerActionButtons');
    if (actionsContainer) {
        actionsContainer.innerHTML = `
            <button type="button" class="btn-fast-ship" onclick="quickUpdateStatus(${orderId}, 'SHIPPED')">
                \uD83D\uDE9A Pick Up & Ship
            </button>
            <button type="button" class="btn-fast-out" onclick="quickUpdateStatus(${orderId}, 'OUT_FOR_DELIVERY')">
                \uD83D\uDEF5 Out for Delivery
            </button>
            <button type="button" class="btn btn-sm btn-outline" onclick="openUpdateModal(${orderId}, 'SHIPPED')">
                \uD83D\uDD0D Details
            </button>
        `;
    }

    banner.style.display = 'flex';
}

function dismissAssignmentBanner() {
    const banner = document.getElementById('newAssignmentBanner');
    if (banner) banner.style.display = 'none';
    const firstAssigned = partnerDeliveries.find(d => {
        const st = (d.status || '').toUpperCase();
        return st === 'ASSIGNED' || st === 'PLACED' || st === 'CONFIRMED';
    });
    if (firstAssigned) {
        dismissedBannerOrderId = firstAssigned.orderId;
    }
}

// -------------------------------------------------------------
// 1-Click Fast Status Update (Simple & Direct)
// -------------------------------------------------------------
async function quickUpdateStatus(orderId, newStatus, estimatedTime = '') {
    showDashboardAlert(`Updating Order #${orderId} to ${newStatus}...`, 'info');

    try {
        let isSuccess = false;

        // Default estimated time for OUT_FOR_DELIVERY if not provided
        if (newStatus === 'OUT_FOR_DELIVERY' && !estimatedTime) {
            estimatedTime = 'Within 30-45 mins';
        }

        const timeParam = estimatedTime ? `&estimatedTime=${encodeURIComponent(estimatedTime)}` : '';

        // 1. Call delivery-partner-service (Port 8088)
        try {
            const res = await fetch(`/delivery/orders/${orderId}/status?status=${encodeURIComponent(newStatus)}&remarks=${encodeURIComponent('Updated via Courier Dashboard')}${timeParam}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ status: newStatus, remarks: 'Updated via Courier Dashboard', estimatedTime: estimatedTime })
            });
            if (res.ok) isSuccess = true;
        } catch (e1) {
            console.warn('Delivery service status call warning:', e1);
        }

        // 2. Direct Sync to order-service (Port 8084)
        try {
            await fetch(`http://localhost:8084/orders/${orderId}/status?status=${encodeURIComponent(newStatus)}${timeParam}`, {
                method: 'PUT'
            });
            isSuccess = true;
        } catch (e2) {
            try {
                await fetch(`http://localhost:8084/orders/${orderId}/status?status=${encodeURIComponent(newStatus)}${timeParam}`, {
                    method: 'POST'
                });
                isSuccess = true;
            } catch (e3) {}
        }

        if (isSuccess) {
            closeModal();
            showDashboardAlert(`\u2705 Order #${orderId} status successfully updated to ${newStatus}! Customer notification dispatched.`, 'success');
            await loadDeliveries(false);
            await loadNotifications(false);
        } else {
            showDashboardAlert(`Failed to update status for Order #${orderId}`, 'danger');
        }
    } catch (err) {
        showDashboardAlert(`Error updating status: ${err.message}`, 'danger');
    }
}

// -------------------------------------------------------------
// Render Deliveries Table with 1-Click Action Buttons
// -------------------------------------------------------------
function setDeliveryFilter(filter, btn) {
    activeFilter = filter;
    document.querySelectorAll('.categories-bar .filter-btn').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
    renderDeliveries();
}

function renderDeliveries() {
    const tbody = document.getElementById('deliveriesTableBody');
    if (!tbody) return;

    const sourceList = (viewMode === 'ALL_COURIERS') ? systemDeliveries : partnerDeliveries;

    let filtered = sourceList;
    if (activeFilter !== 'ALL') {
        filtered = sourceList.filter(d => {
            const st = (d.status || '').toUpperCase();
            if (activeFilter === 'ASSIGNED') return st === 'ASSIGNED' || st === 'PLACED' || st === 'CONFIRMED';
            return st === activeFilter;
        });
    }

    if (filtered.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; padding: 3rem 1.5rem; color: #718096;">
                    <div style="font-size: 2.2rem; margin-bottom: 0.5rem;">\uD83D\uDCE6</div>
                    <div style="font-weight: 700; color: #2d3748; font-size: 1.05rem;">No customer shipments in this stage</div>
                    <div style="font-size: 0.88rem; margin-top: 0.25rem;">
                        ${viewMode === 'MINE' 
                            ? `When merchants assign orders to ${currentPartner ? currentPartner.partnerName : 'this courier'}, they will appear here!`
                            : 'No orders found across network couriers in this milestone.'}
                    </div>
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filtered.map(d => {
        const st = (d.status || 'ASSIGNED').toUpperCase();
        let badgeClass = 'badge-assigned';
        let badgeLabel = 'Assigned';

        if (st === 'SHIPPED') { badgeClass = 'badge-shipped'; badgeLabel = 'In Transit'; }
        else if (st === 'OUT_FOR_DELIVERY') { badgeClass = 'badge-out'; badgeLabel = 'Out for Delivery'; }
        else if (st === 'DELIVERED') { badgeClass = 'badge-delivered'; badgeLabel = 'Delivered'; }
        else if (st === 'CANCELLED') { badgeClass = 'badge-cancelled'; badgeLabel = 'Cancelled'; }

        // Direct 1-click Fast Action Buttons right in the table row
        let actionButtonsHtml = '';
        if (st === 'ASSIGNED' || st === 'PLACED' || st === 'CONFIRMED') {
            actionButtonsHtml = `
                <button type="button" class="btn-fast-ship" onclick="quickUpdateStatus(${d.orderId}, 'SHIPPED')">
                    \uD83D\uDE9A Mark Shipped
                </button>
            `;
        } else if (st === 'SHIPPED') {
            actionButtonsHtml = `
                <button type="button" class="btn-fast-out" onclick="openUpdateModal(${d.orderId}, 'OUT_FOR_DELIVERY')">
                    \uD83D\uDEF5 Out for Delivery
                </button>
            `;
        } else if (st === 'OUT_FOR_DELIVERY') {
            actionButtonsHtml = `
                <button type="button" class="btn btn-sm btn-success" style="font-weight: 700; font-size: 0.8rem; padding: 0.35rem 0.75rem;" onclick="promptDeliverModal(${d.orderId})">
                    \u2705 Mark Delivered
                </button>
                <button type="button" class="btn btn-sm" style="background: #fff1f2; color: #b91c1c; border: 1px solid #fecdd3; font-weight: 700; font-size: 0.8rem; padding: 0.35rem 0.6rem;" onclick="openFailModal(${d.orderId})">
                    ✕ Failed
                </button>
            `;
        } else if (st === 'DELIVERED') {
            actionButtonsHtml = `
                <span style="color: #15803d; font-weight: 800; font-size: 0.88rem; display: inline-flex; align-items: center; gap: 4px;">
                    \u2705 Completed
                </span>
            `;
        }

        const dateStr = d.assignedAt ? new Date(d.assignedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : 'Today';

        const isAnotherCourier = currentPartner && d.partnerId && d.partnerId !== currentPartner.partnerId;
        const courierTag = (viewMode === 'ALL_COURIERS' || isAnotherCourier) ? `
            <div style="font-size: 0.73rem; color: #2563eb; font-weight: 700; margin-top: 2px;">
                Courier: ${escapeHtml(d.partnerName || ('Partner #' + d.partnerId))}
            </div>
        ` : '';

        const isReturnPickup = d.assignmentType === 'RETURN_PICKUP';
        const returnBadge = isReturnPickup ? `
            <span style="background: #fef3c7; color: #b45309; border: 1px solid #fde68a; font-size: 0.72rem; font-weight: 700; padding: 2px 6px; border-radius: 4px; display: inline-block; margin-top: 3px;">
                🔄 Return Pickup
            </span>
        ` : '';

        const mapBtnHtml = d.returnLocationMapUrl ? `
            <a href="${escapeHtml(d.returnLocationMapUrl)}" target="_blank" class="btn btn-sm" style="background: #e0f2fe; color: #0369a1; border: 1px solid #bae6fd; font-size: 0.75rem; text-decoration: none; padding: 2px 8px; border-radius: 4px; display: inline-flex; align-items: center; gap: 4px; margin-top: 4px;">
                📍 Customer Map Link &rarr;
            </a>
        ` : '';

        return `
            <tr id="order-row-${d.orderId}" style="${isReturnPickup ? 'background: #fffbeb;' : ''}">
                <td>
                    <div style="font-weight: 800; color: #2b6cb0; font-size: 0.95rem;">#${d.orderId}</div>
                    <div style="font-size: 0.78rem; color: #718096;">${dateStr}</div>
                    <div style="font-size: 0.75rem; color: #dd6b20; font-weight: 600; margin-top: 2px;">Merchant: ${escapeHtml(d.merchantName || 'Seller')}</div>
                    ${courierTag}
                    ${returnBadge}
                </td>
                <td>
                    <div style="font-weight: 700; color: #1a202c;">${escapeHtml(d.productName || 'Catalog Product')}</div>
                    <div style="font-size: 0.82rem; color: #718096;">Qty: <strong>${d.quantity || 1}</strong></div>
                    ${isReturnPickup && d.returnReason ? `<div style="font-size: 0.75rem; color: #b45309; font-weight: 600; margin-top: 3px;">Reason: ${escapeHtml(d.returnReason)}</div>` : ''}
                </td>
                <td>
                    <div style="font-weight: 700; color: #2d3748;">${escapeHtml(d.customerName || 'Customer')}</div>
                    <div style="font-size: 0.84rem; color: #4a5568; max-width: 250px;">${escapeHtml(d.shippingAddress || 'Address on file')}</div>
                    ${d.customerPhone ? `<a href="tel:${d.customerPhone}" style="font-size: 0.8rem; color: #3182ce; font-weight: 600; text-decoration: none;">\uD83D\uDCDE ${escapeHtml(d.customerPhone)}</a>` : ''}
                    <div>${mapBtnHtml}</div>
                </td>
                <td>
                    <div style="font-weight: 800; color: #2b6cb0; font-size: 1rem;">Rs. ${(d.totalAmount || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 })}</div>
                    <div style="font-size: 0.76rem; color: #718096; font-weight: 600;">${escapeHtml(d.paymentMode || 'WALLET')}</div>
                </td>
                <td>
                    <span class="status-badge ${badgeClass}">${badgeLabel}</span>
                </td>
                <td style="text-align: right;">
                    <div style="display: flex; gap: 0.45rem; justify-content: flex-end; align-items: center;">
                        ${actionButtonsHtml}
                        <button type="button" class="btn btn-sm btn-outline" title="Inspect Order Details" onclick="openUpdateModal(${d.orderId}, '${d.status || 'SHIPPED'}')">
                            \uD83D\uDD0D
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// -------------------------------------------------------------
// Delivery Modal Helpers & Window Exports
// -------------------------------------------------------------
function promptDeliverModal(orderId) {
    openUpdateModal(orderId, 'DELIVERED');
}
window.promptDeliverModal = promptDeliverModal;

function openUpdateModal(orderId, defaultNextStatus) {
    selectedOrderId = orderId;
    pendingStatus = defaultNextStatus;

    const sourceList = (viewMode === 'ALL_COURIERS') ? systemDeliveries : partnerDeliveries;
    let assignment = sourceList.find(d => d.orderId === orderId);
    if (!assignment && partnerDeliveries.length > 0) {
        assignment = partnerDeliveries.find(d => d.orderId === orderId);
    }
    if (!assignment && systemDeliveries.length > 0) {
        assignment = systemDeliveries.find(d => d.orderId === orderId);
    }
    if (!assignment) return;

    document.getElementById('modalOrderTitle').textContent = `Order #${orderId} - ${assignment.productName || 'Package'}`;
    document.getElementById('modalCustomerSubtitle').textContent = `Customer: ${assignment.customerName || 'Customer'} \u2022 Total: Rs. ${(assignment.totalAmount || 0).toFixed(2)}`;
    document.getElementById('modalDestName').textContent = assignment.customerName || 'Customer';
    document.getElementById('modalDestAddress').textContent = assignment.shippingAddress || 'Destination address on file';
    document.getElementById('modalDestPhone').textContent = `\uD83D\uDCDE ${assignment.customerPhone || 'Not provided'}`;

    document.getElementById('modalRemarks').value = assignment.remarks || '';

    setNextStatus(defaultNextStatus);
    document.getElementById('statusModal').style.display = 'flex';
}

function setNextStatus(status) {
    pendingStatus = status;

    const btnShipped = document.getElementById('btnSetShipped');
    const btnOut = document.getElementById('btnSetOut');
    const btnDelivered = document.getElementById('btnSetDelivered');

    [btnShipped, btnOut, btnDelivered].forEach(b => {
        if (b) b.className = 'btn btn-sm btn-outline';
    });

    if (status === 'SHIPPED' && btnShipped) {
        btnShipped.className = 'btn btn-sm btn-warning';
    } else if (status === 'OUT_FOR_DELIVERY' && btnOut) {
        btnOut.className = 'btn btn-sm btn-primary';
    } else if (status === 'DELIVERED' && btnDelivered) {
        btnDelivered.className = 'btn btn-sm btn-success';
    }

    const timeBox = document.getElementById('deliveryTimeBox');
    if (timeBox) {
        timeBox.style.display = (status === 'OUT_FOR_DELIVERY') ? 'block' : 'none';
        if (status === 'OUT_FOR_DELIVERY') {
            const timeInput = document.getElementById('modalEstimatedTime');
            if (timeInput && !timeInput.value) {
                timeInput.value = 'Within 30-45 mins';
            }
        }
    }

    updateModalStepper(status);
}

function setEstimatedTimePreset(val) {
    const input = document.getElementById('modalEstimatedTime');
    if (input) {
        input.value = val;
    }
}
window.setEstimatedTimePreset = setEstimatedTimePreset;

function updateModalStepper(status) {
    const st = (status || 'ASSIGNED').toUpperCase();

    const s1 = document.getElementById('step-assigned');
    const s2 = document.getElementById('step-shipped');
    const s3 = document.getElementById('step-out');
    const s4 = document.getElementById('step-delivered');

    [s1, s2, s3, s4].forEach(s => {
        if (s) {
            s.querySelector('.step-circle').className = 'step-circle';
            s.querySelector('.step-title').className = 'step-title';
        }
    });

    const setCircle = (item, cls) => {
        if (!item) return;
        item.querySelector('.step-circle').classList.add(cls);
        item.querySelector('.step-title').classList.add(cls);
    };

    if (st === 'ASSIGNED' || st === 'PLACED' || st === 'CONFIRMED') {
        setCircle(s1, 'active');
    } else if (st === 'SHIPPED') {
        setCircle(s1, 'completed');
        setCircle(s2, 'active');
    } else if (st === 'OUT_FOR_DELIVERY') {
        setCircle(s1, 'completed');
        setCircle(s2, 'completed');
        setCircle(s3, 'active');
    } else if (st === 'DELIVERED') {
        setCircle(s1, 'completed');
        setCircle(s2, 'completed');
        setCircle(s3, 'completed');
        setCircle(s4, 'completed');
    }
}

async function submitStatusUpdate() {
    if (!selectedOrderId || !pendingStatus) return;
    const remarks = document.getElementById('modalRemarks')?.value || '';
    const estimatedTime = (pendingStatus === 'OUT_FOR_DELIVERY') 
        ? (document.getElementById('modalEstimatedTime')?.value || 'Within 30-45 mins')
        : '';
    
    const btn = document.getElementById('btnSaveStatus');
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Saving...';
    }

    try {
        await quickUpdateStatus(selectedOrderId, pendingStatus, estimatedTime);
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.textContent = '\uD83D\uDCBE Save Status & Update Tracking';
        }
    }
}

function closeModal() {
    document.getElementById('statusModal').style.display = 'none';
    selectedOrderId = null;
    pendingStatus = null;
    document.getElementById('modalRemarks').value = '';
}

// -------------------------------------------------------------
// Notification Drawer & Counter
// -------------------------------------------------------------
async function loadNotifications(manual = false) {
    if (!currentPartner) return;

    try {
        const res = await fetch(`/delivery/partner/${currentPartner.partnerId}/notifications`);
        if (res.ok) {
            partnerNotifications = await res.json();
            renderNotifications();
            if (manual) showDashboardAlert("Notifications refreshed!", "info");
        }
    } catch (err) {
        console.warn("Could not load notifications:", err);
    }
}

function renderNotifications() {
    const listContainer = document.getElementById('notifListContainer');
    const badge = document.getElementById('notifBadgeCount');
    const totalCountEl = document.getElementById('notifTotalCount');

    if (!listContainer) return;

    const count = partnerNotifications.length;
    if (totalCountEl) totalCountEl.textContent = count;

    const readIds = new Set(JSON.parse(localStorage.getItem('shopnexa_read_notifs') || '[]'));
    const unread = partnerNotifications.filter(n => !readIds.has(n.id || n.orderId));

    if (badge) {
        if (unread.length > 0) {
            badge.textContent = unread.length;
            badge.classList.remove('empty');
        } else {
            badge.textContent = '0';
            badge.classList.add('empty');
        }
    }

    if (count === 0) {
        listContainer.innerHTML = `
            <div style="text-align: center; padding: 2rem 1rem; color: #94a3b8; font-size: 0.85rem;">
                <div style="font-size: 1.8rem; margin-bottom: 0.4rem;">\uD83D\uDCEB</div>
                No notifications yet.<br/>When merchants assign orders, they will appear here!
            </div>
        `;
        return;
    }

    listContainer.innerHTML = partnerNotifications.map(n => {
        const isUnread = !readIds.has(n.id || n.orderId);
        const orderId = n.orderId || (n.subject ? (n.subject.match(/#(\d+)/) || [])[1] : null);
        const dateStr = n.timestamp ? new Date(n.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Just now';

        return `
            <div class="notif-card ${isUnread ? 'unread' : ''}" onclick="handleNotifClick('${n.id || n.orderId}', ${orderId})">
                <div class="notif-card-head">
                    <span class="notif-card-title">${escapeHtml(n.subject || '\uD83D\uDCE6 Order Assignment')}</span>
                    <span class="notif-card-time">${dateStr}</span>
                </div>
                <div class="notif-card-desc">${escapeHtml(n.snippet || 'New shipment package assigned for fulfillment.')}</div>
                ${orderId ? `<div style="margin-top: 6px; font-size: 0.76rem; color: #2563eb; font-weight: 700;">\uD83D\uDE9A Open Order #${orderId} &rarr;</div>` : ''}
            </div>
        `;
    }).join('');
}

function toggleNotificationDrawer() {
    const dropdown = document.getElementById('notifDropdown');
    if (!dropdown) return;

    dropdown.classList.toggle('active');

    if (dropdown.classList.contains('active')) {
        const ids = partnerNotifications.map(n => n.id || n.orderId);
        localStorage.setItem('shopnexa_read_notifs', JSON.stringify(ids));
        const badge = document.getElementById('notifBadgeCount');
        if (badge) {
            badge.textContent = '0';
            badge.classList.add('empty');
        }
        document.querySelectorAll('.notif-card.unread').forEach(c => c.classList.remove('unread'));
    }
}

function handleNotifClick(notifId, orderId) {
    const dropdown = document.getElementById('notifDropdown');
    if (dropdown) dropdown.classList.remove('active');

    if (orderId) {
        const found = partnerDeliveries.find(d => d.orderId === parseInt(orderId));
        if (found) {
            openUpdateModal(found.orderId, found.status || 'SHIPPED');
        } else {
            showDashboardAlert(`Loading Order #${orderId}...`, 'info');
            loadDeliveries(false);
        }
    }
}

// -------------------------------------------------------------
// Live Auto-Sync & Utilities
// -------------------------------------------------------------
function startLiveSync() {
    stopLiveSync();
    pollInterval = setInterval(() => {
        loadDeliveries(true);
        loadNotifications(false);
    }, 6000);
}

function stopLiveSync() {
    if (pollInterval) {
        clearInterval(pollInterval);
        pollInterval = null;
    }
}

function playChime() {
    try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = 'sine';
        osc.frequency.setValueAtTime(587.33, ctx.currentTime);
        osc.frequency.setValueAtTime(880, ctx.currentTime + 0.12);
        gain.gain.setValueAtTime(0.15, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4);
        osc.start();
        osc.stop(ctx.currentTime + 0.4);
    } catch (e) {}
}

function updateKPIs() {
    let assigned = 0, shipped = 0, out = 0, delivered = 0;

    partnerDeliveries.forEach(d => {
        const st = (d.status || '').toUpperCase();
        if (st === 'ASSIGNED' || st === 'PLACED' || st === 'CONFIRMED') assigned++;
        else if (st === 'SHIPPED') shipped++;
        else if (st === 'OUT_FOR_DELIVERY') out++;
        else if (st === 'DELIVERED') delivered++;
    });

    const setEl = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.textContent = val;
    };

    setEl('kpiAssigned', assigned);
    setEl('kpiShipped', shipped);
    setEl('kpiOutForDelivery', out);
    setEl('kpiDelivered', delivered);

    // Earnings calculation: Rs. 50 flat per delivery + 2% payout incentive
    const completedDelvs = partnerDeliveries.filter(d => (d.status || '').toUpperCase() === 'DELIVERED');
    let totalEarnings = completedDelvs.length * 50.0;
    completedDelvs.forEach(d => {
        if (d.totalAmount) totalEarnings += (d.totalAmount * 0.02);
    });
    setEl('kpiEarnings', 'Rs. ' + totalEarnings.toFixed(2));

    setEl('countAll', partnerDeliveries.length);
    setEl('countAssigned', assigned);
    setEl('countShipped', shipped);
    setEl('countOut', out);
    setEl('countDelivered', delivered);
}

// Duty status toggle (AVAILABLE, BUSY, OFFLINE)
async function changeCourierDutyStatus(newStatus) {
    if (!currentPartner) return;
    try {
        const res = await fetch(`/delivery/partners/${currentPartner.partnerId}/status?status=${encodeURIComponent(newStatus)}`, {
            method: 'PUT'
        });
        currentPartner.status = newStatus;
        localStorage.setItem('shopnexa_courier', JSON.stringify(currentPartner));

        const dutySel = document.getElementById('partnerDutyStatusSelect');
        if (dutySel) {
            dutySel.value = newStatus;
            dutySel.style.color = newStatus === 'AVAILABLE' ? '#16a34a' : (newStatus === 'BUSY' ? '#ca8a04' : '#64748b');
        }

        const match = allPartners.find(p => p.partnerId === currentPartner.partnerId);
        if (match) match.status = newStatus;

        showDashboardAlert(`Duty status updated: ${newStatus === 'AVAILABLE' ? '🟢 Available' : (newStatus === 'BUSY' ? '🟡 Busy (On Delivery)' : '⚪ Offline (Off-Duty)')}`, 'success');
    } catch (e) {
        showDashboardAlert('Error updating duty status.', 'danger');
    }
}

// Delivery Failure Handling
function openFailModal(orderId) {
    document.getElementById('failOrderId').value = orderId;
    document.getElementById('failDeliveryModal').style.display = 'flex';
}

function closeFailModal() {
    document.getElementById('failDeliveryModal').style.display = 'none';
}

async function submitDeliveryFailure() {
    const orderId = document.getElementById('failOrderId').value;
    const reason = document.getElementById('failReasonSelect').value;
    const reschedule = document.getElementById('failRescheduleDate').value;
    const fullReason = `${reason} (${reschedule})`;

    try {
        const res = await fetch(`/delivery/orders/${orderId}/fail?reason=${encodeURIComponent(fullReason)}`, {
            method: 'POST'
        });
        if (res.ok) {
            closeFailModal();
            showDashboardAlert(`⚠️ Order #${orderId} marked as Delivery Failed. Reason logged.`, 'info');
            await loadDeliveries(false);
        } else {
            showDashboardAlert('Failed to update delivery status.', 'danger');
        }
    } catch (err) {
        showDashboardAlert('Server error updating failure reason.', 'danger');
    }
}

function showDashboardAlert(msg, type = 'info') {
    const alertBox = document.getElementById('dashboardAlert');
    if (!alertBox) return;
    alertBox.className = `alert alert-${type}`;
    alertBox.textContent = msg;
    alertBox.style.display = 'block';
    setTimeout(() => { alertBox.style.display = 'none'; }, 6000);
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
