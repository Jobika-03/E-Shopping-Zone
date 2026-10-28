/**
 * Order Service Client Application (Port 8084)
 * Multi-Order Management, Batch Cancellation & Deletion, Live Visual Stepper
 */

const API_BASE = '/orders';

const state = {
    customerId: 1,
    orders: [],
    selectedOrderIds: new Set(),
    activeFilter: 'ALL'
};

const TRACKING_STEPS = [
    { key: 'PLACED', label: 'Ordered', icon: '1' },
    { key: 'CONFIRMED', label: 'Confirmed', icon: '2' },
    { key: 'SHIPPED', label: 'Shipped', icon: '3' },
    { key: 'OUT_FOR_DELIVERY', label: 'Out for Delivery', icon: '4' },
    { key: 'DELIVERED', label: 'Delivered', icon: '5' }
];

document.addEventListener('DOMContentLoaded', () => {
    initCustomerSelector();
    initFilters();
    loadOrders();
});

function initCustomerSelector() {
    const input = document.getElementById('customerIdInput');
    if (input) {
        input.addEventListener('change', (e) => {
            const val = parseInt(e.target.value, 10);
            if (!isNaN(val) && val >= 0) {
                state.customerId = val;
                state.selectedOrderIds.clear();
                updateBatchToolbar();
                loadOrders();
            }
        });
    }
}

function initFilters() {
    const filterBtns = document.querySelectorAll('.filter-btn');
    filterBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            filterBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            state.activeFilter = btn.dataset.filter;
            renderOrders();
        });
    });
}

async function loadOrders() {
    try {
        let url = `${API_BASE}/customer/${state.customerId}`;
        if (state.customerId === 0) {
            url = API_BASE;
        }

        const res = await fetch(url);
        if (!res.ok) throw new Error('Failed to load orders');
        state.orders = await res.json();
        updateKpis();
        renderOrders();
    } catch (err) {
        console.error('Error loading orders:', err);
    }
}

function updateKpis() {
    const total = state.orders.length;
    const active = state.orders.filter(o => ['PLACED', 'PLACED_COD', 'CONFIRMED', 'SHIPPED', 'OUT_FOR_DELIVERY'].includes(o.orderStatus?.toUpperCase())).length;
    const delivered = state.orders.filter(o => o.orderStatus?.toUpperCase() === 'DELIVERED').length;
    const cancelled = state.orders.filter(o => o.orderStatus?.toUpperCase() === 'CANCELLED').length;

    document.getElementById('kpiTotalOrders').textContent = total;
    document.getElementById('kpiActiveOrders').textContent = active;
    document.getElementById('kpiDeliveredOrders').textContent = delivered;
    document.getElementById('kpiCancelledOrders').textContent = cancelled;
}

function renderOrders() {
    const container = document.getElementById('ordersListContainer');
    const emptyState = document.getElementById('emptyOrdersState');
    if (!container) return;

    let list = state.orders || [];

    if (state.activeFilter === 'ACTIVE') {
        list = list.filter(o => ['PLACED', 'PLACED_COD', 'CONFIRMED', 'SHIPPED', 'OUT_FOR_DELIVERY'].includes(o.orderStatus?.toUpperCase()));
    } else if (state.activeFilter === 'DELIVERED') {
        list = list.filter(o => o.orderStatus?.toUpperCase() === 'DELIVERED');
    } else if (state.activeFilter === 'CANCELLED') {
        list = list.filter(o => o.orderStatus?.toUpperCase() === 'CANCELLED');
    }

    if (list.length === 0) {
        container.innerHTML = '';
        if (emptyState) emptyState.style.display = 'block';
        return;
    }

    if (emptyState) emptyState.style.display = 'none';

    container.innerHTML = list.map(ord => {
        const isSelected = state.selectedOrderIds.has(ord.orderId);
        const status = ord.orderStatus || 'PLACED';
        const badgeClass = getStatusBadgeClass(status);
        const isCancelled = status.toUpperCase() === 'CANCELLED';
        const isDelivered = status.toUpperCase() === 'DELIVERED';
        const canCancel = !isCancelled && !isDelivered;

        const prodName = ord.product?.productName || 'Catalog Product Item';
        const sellerName = ord.sellerName || 'Merchant Seller';
        const total = Number(ord.ammountPaid || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 });
        const date = ord.orderDate || 'Recent';

        return `
            <div class="order-card ${isSelected ? 'selected' : ''}" id="order-card-${ord.orderId}">
                <div class="order-card-header">
                    <div class="order-card-title">
                        <input type="checkbox" class="order-checkbox" 
                               ${isSelected ? 'checked' : ''} 
                               onchange="toggleOrderSelection(${ord.orderId}, this.checked)">
                        <div>
                            <span style="font-size: 1.15rem; font-weight: 800; color: #0f172a;">Order #${ord.orderId}</span>
                            <span style="color: #64748b; font-size: 0.88rem; margin-left: 0.5rem;">Placed on ${escapeHtml(date)}</span>
                        </div>
                    </div>
                    <div>
                        <span class="status-badge ${badgeClass}">${escapeHtml(status)}</span>
                    </div>
                </div>

                <div class="order-meta-grid">
                    <div class="meta-item">
                        <label>Product</label>
                        <span>${escapeHtml(prodName)}</span>
                    </div>
                    <div class="meta-item">
                        <label>Seller</label>
                        <span style="color: #ea580c;">${escapeHtml(sellerName)}</span>
                    </div>
                    <div class="meta-item">
                        <label>Quantity</label>
                        <span>${ord.quantity || 1} Item(s)</span>
                    </div>
                    <div class="meta-item">
                        <label>Total Amount</label>
                        <span style="color: #2563eb;">Rs. ${total}</span>
                    </div>
                    <div class="meta-item">
                        <label>Payment Mode</label>
                        <span>${escapeHtml(ord.modeOfPayment || 'WALLET')}</span>
                    </div>
                    <div class="meta-item">
                        <label>Customer</label>
                        <span>Customer #${ord.customerId}</span>
                    </div>
                </div>

                <!-- Visual Stepper -->
                ${renderStepperHtml(status)}

                <!-- Card Actions -->
                <div class="order-card-footer">
                    <div style="font-size: 0.85rem; color: #64748b;">
                        ${isCancelled ? '<span style="color: #dc2626; font-weight: 600;">⚠️ This order was cancelled. Any E-Wallet payments have been refunded.</span>' : ''}
                        ${isDelivered ? '<span style="color: #059669; font-weight: 600;">✅ Package successfully delivered.</span>' : ''}
                    </div>

                    <div style="display: flex; gap: 0.5rem;">
                        ${canCancel ? `
                            <button type="button" class="btn btn-outline" style="color: #dc2626; border-color: #fecaca;"
                                    onclick="handleCancelSingleOrder(${ord.orderId})">
                                ❌ Cancel Order
                            </button>
                        ` : ''}
                        
                        <button type="button" class="btn btn-outline" style="color: #475569; border-color: #e2e8f0;"
                                onclick="handleDeleteSingleOrder(${ord.orderId})">
                            🗑️ Delete Order
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function renderStepperHtml(statusStr) {
    const raw = (statusStr || 'PLACED').toUpperCase().replace('-', '_');
    if (raw === 'CANCELLED') {
        return `
            <div style="background: #fef2f2; color: #991b1b; padding: 0.75rem 1rem; border-radius: 6px; font-weight: 700; font-size: 0.88rem; margin: 0.75rem 0;">
                ❌ Order Status: CANCELLED
            </div>
        `;
    }

    let activeIdx = 0;
    if (raw === 'CONFIRMED') activeIdx = 1;
    else if (raw === 'SHIPPED' || raw === 'SHIPPING') activeIdx = 2;
    else if (raw === 'OUT_FOR_DELIVERY' || raw.includes('DELIVERY')) activeIdx = 3;
    else if (raw === 'DELIVERED') activeIdx = 4;

    const stepsHtml = TRACKING_STEPS.map((step, idx) => {
        const isCompleted = idx < activeIdx || (idx === 4 && activeIdx === 4);
        const isActive = idx === activeIdx && activeIdx !== 4;

        let dotClass = '';
        let content = step.icon;
        let labelClass = '';

        if (isCompleted) {
            dotClass = 'completed';
            content = '✓';
            labelClass = 'completed';
        } else if (isActive) {
            dotClass = 'active';
            labelClass = 'active';
        }

        return `
            <div class="tracker-step">
                <div class="tracker-dot ${dotClass}">
                    ${content}
                </div>
                <div class="tracker-label ${labelClass}">
                    ${escapeHtml(step.label)}
                </div>
            </div>
        `;
    }).join('');

    return `
        <div class="tracker-container">
            ${stepsHtml}
        </div>
    `;
}

// Single Order Cancel
async function handleCancelSingleOrder(orderId) {
    const order = state.orders.find(o => o.orderId === orderId);
    const mode = order?.modeOfPayment || 'WALLET';
    const amount = Number(order?.ammountPaid || 0).toFixed(2);

    let confirmMsg = `Are you sure you want to cancel Order #${orderId}?`;
    if (mode.toUpperCase().includes('WALLET')) {
        confirmMsg += `\n\n💰 An immediate refund of Rs. ${amount} will be deposited back to your E-Wallet!`;
    }

    if (!confirm(confirmMsg)) return;

    try {
        const res = await fetch(`${API_BASE}/${orderId}/cancel`, { method: 'POST' });
        if (!res.ok) throw new Error('Cancellation failed (HTTP ' + res.status + ')');

        showPageAlert(`Order #${orderId} was cancelled successfully. ${mode.toUpperCase().includes('WALLET') ? `Rs. ${amount} was refunded to E-Wallet.` : ''}`, 'success');
        await loadOrders();
    } catch (err) {
        showPageAlert('Failed to cancel order: ' + err.message, 'danger');
    }
}

// Single Order Delete
async function handleDeleteSingleOrder(orderId) {
    if (!confirm(`Are you sure you want to permanently delete Order #${orderId} from order history?`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/${orderId}/delete`, { method: 'POST' });
        if (!res.ok) throw new Error('Deletion failed (HTTP ' + res.status + ')');

        state.selectedOrderIds.delete(orderId);
        updateBatchToolbar();
        showPageAlert(`Order #${orderId} was deleted successfully.`, 'info');
        await loadOrders();
    } catch (err) {
        showPageAlert('Failed to delete order: ' + err.message, 'danger');
    }
}

// Multi-Order Selection
function toggleOrderSelection(orderId, checked) {
    if (checked) {
        state.selectedOrderIds.add(orderId);
    } else {
        state.selectedOrderIds.delete(orderId);
    }
    updateBatchToolbar();
    const card = document.getElementById(`order-card-${orderId}`);
    if (card) {
        card.classList.toggle('selected', checked);
    }
}

function selectAllVisibleOrders(checked) {
    state.orders.forEach(o => {
        if (checked) {
            state.selectedOrderIds.add(o.orderId);
        } else {
            state.selectedOrderIds.delete(o.orderId);
        }
    });
    updateBatchToolbar();
    renderOrders();
}

function clearAllSelections() {
    state.selectedOrderIds.clear();
    updateBatchToolbar();
    renderOrders();
}

function updateBatchToolbar() {
    const toolbar = document.getElementById('batchToolbar');
    const countSpan = document.getElementById('selectedOrdersCount');
    const selectAllCb = document.getElementById('selectAllCheckbox');
    if (!toolbar || !countSpan) return;

    const count = state.selectedOrderIds.size;
    countSpan.textContent = count;

    if (count > 0) {
        toolbar.style.display = 'flex';
    } else {
        toolbar.style.display = 'none';
    }

    if (selectAllCb) {
        selectAllCb.checked = count > 0 && count === state.orders.length;
    }
}

// Batch Cancel
async function handleCancelSelectedOrders() {
    const ids = Array.from(state.selectedOrderIds);
    if (ids.length === 0) return;

    if (!confirm(`Are you sure you want to cancel the ${ids.length} selected orders (#${ids.join(', #')})?\nAny wallet payments will be immediately refunded.`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/cancel-multiple`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(ids)
        });

        if (!res.ok) throw new Error('Batch cancel failed');
        const cancelledIds = await res.json();

        clearAllSelections();
        showPageAlert(`Successfully cancelled ${cancelledIds.length} orders: #${cancelledIds.join(', #')}. E-Wallet refunds processed.`, 'success');
        await loadOrders();
    } catch (err) {
        showPageAlert('Batch cancellation failed: ' + err.message, 'danger');
    }
}

// Batch Delete
async function handleDeleteSelectedOrders() {
    const ids = Array.from(state.selectedOrderIds);
    if (ids.length === 0) return;

    if (!confirm(`Are you sure you want to permanently delete the ${ids.length} selected orders (#${ids.join(', #')})?`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/delete-multiple`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(ids)
        });

        if (!res.ok) throw new Error('Batch delete failed');
        const deletedIds = await res.json();

        clearAllSelections();
        showPageAlert(`Successfully deleted ${deletedIds.length} orders: #${deletedIds.join(', #')}.`, 'info');
        await loadOrders();
    } catch (err) {
        showPageAlert('Batch deletion failed: ' + err.message, 'danger');
    }
}

function showPageAlert(msg, type = 'success') {
    const el = document.getElementById('pageAlert');
    if (!el) return;
    el.style.display = 'block';
    el.className = `alert alert-${type}`;
    el.textContent = msg;
    window.scrollTo({ top: 0, behavior: 'smooth' });
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

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
