// BiteExpress Client-Side Application Logic
let menuItems = [];
let cart = []; // Array of { foodId, item, quantity, notes }
let appliedCoupon = null;
let currentCategory = 'All';

// Global functions for inline onclick handlers
window.addToCart = function(foodId) {
    const item = menuItems.find(f => f.id === foodId);
    if (!item) return;

    const existing = cart.find(c => c.foodId === foodId);
    if (existing) {
        existing.quantity++;
    } else {
        cart.push({ foodId: item.id, item: item, quantity: 1, notes: '' });
    }

    updateCartUI();
    openCart();
};

window.updateQty = function(foodId, delta) {
    const index = cart.findIndex(c => c.foodId === foodId);
    if (index !== -1) {
        cart[index].quantity += delta;
        if (cart[index].quantity <= 0) {
            cart.splice(index, 1);
        }
    }
    updateCartUI();
};

window.updateOrderStatus = async function(orderId, newStatus) {
    try {
        const res = await fetch(`/api/orders/${orderId}`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        });

        if (res.ok) {
            fetchKitchenOrders();
            fetchOrders();
        }
    } catch (err) {
        console.error(err);
    }
};

function openCart() {
    const cartDrawer = document.getElementById('cart-drawer');
    const drawerOverlay = document.getElementById('cart-drawer-overlay');
    if (cartDrawer && drawerOverlay) {
        cartDrawer.classList.add('open');
        drawerOverlay.classList.add('active');
    }
}

function closeCart() {
    const cartDrawer = document.getElementById('cart-drawer');
    const drawerOverlay = document.getElementById('cart-drawer-overlay');
    if (cartDrawer && drawerOverlay) {
        cartDrawer.classList.remove('open');
        drawerOverlay.classList.remove('active');
    }
}

function updateCartUI() {
    const cartCountEl = document.getElementById('cart-count');
    const cartItemsContainer = document.getElementById('cart-items-container');

    const totalItems = cart.reduce((sum, i) => sum + i.quantity, 0);
    if (cartCountEl) cartCountEl.textContent = totalItems;

    if (!cartItemsContainer) return;

    if (cart.length === 0) {
        cartItemsContainer.innerHTML = `
            <div class="empty-cart-msg">
                <i class="fa-solid fa-basket-shopping" style="font-size: 2.5rem; margin-bottom: 1rem; color: var(--text-muted);"></i>
                <p>Your basket is currently empty.</p>
            </div>`;
    } else {
        cartItemsContainer.innerHTML = cart.map(c => `
            <div class="cart-item">
                <div class="cart-item-details">
                    <div class="cart-item-title">${c.item.name}</div>
                    <div class="cart-item-price">$${(c.item.price * c.quantity).toFixed(2)}</div>
                </div>
                <div class="qty-controls">
                    <button class="qty-btn" onclick="window.updateQty('${c.foodId}', -1)">-</button>
                    <span>${c.quantity}</span>
                    <button class="qty-btn" onclick="window.updateQty('${c.foodId}', 1)">+</button>
                </div>
            </div>
        `).join('');
    }

    calculateBill();
}

function calculateBill() {
    const billSubtotal = document.getElementById('bill-subtotal');
    const billDiscountRow = document.getElementById('bill-discount-row');
    const billDiscount = document.getElementById('bill-discount');
    const billDelivery = document.getElementById('bill-delivery');
    const billTax = document.getElementById('bill-tax');
    const billTotal = document.getElementById('bill-total');

    if (!billSubtotal) return;

    const subtotal = cart.reduce((sum, c) => sum + (c.item.price * c.quantity), 0);
    let discount = 0;

    // Backend Coupon.toJson() sends discountPercentage/minOrderValue
    const minOrder = appliedCoupon ? (appliedCoupon.minOrderValue ?? appliedCoupon.minOrder ?? 0) : 0;
    const discountPct = appliedCoupon ? (appliedCoupon.discountPercentage ?? appliedCoupon.discountPercent ?? 0) : 0;

    if (appliedCoupon && subtotal >= minOrder) {
        if (discountPct > 0) {
            discount = (subtotal * (discountPct / 100.0));
            if (appliedCoupon.maxDiscount > 0 && discount > appliedCoupon.maxDiscount) {
                discount = appliedCoupon.maxDiscount;
            }
        }
    }

    const delivery = subtotal > 50.0 || (appliedCoupon && appliedCoupon.code === 'FREESHIP') ? 0.0 : (subtotal > 0 ? 3.99 : 0.0);
    const tax = Math.max(0, Math.round((subtotal - discount) * 0.08 * 100.0) / 100.0);
    const total = Math.max(0, Math.round((subtotal - discount + delivery + tax) * 100.0) / 100.0);

    billSubtotal.textContent = `$${subtotal.toFixed(2)}`;
    if (discount > 0) {
        billDiscountRow.style.display = 'flex';
        billDiscount.textContent = `-$${discount.toFixed(2)}`;
    } else {
        billDiscountRow.style.display = 'none';
    }
    billDelivery.textContent = delivery === 0 ? 'FREE' : `$${delivery.toFixed(2)}`;
    billTax.textContent = `$${tax.toFixed(2)}`;
    billTotal.textContent = `$${total.toFixed(2)}`;
}

// Fetch Menu API
async function fetchMenu() {
    const menuGrid = document.getElementById('menu-grid');
    try {
        const response = await fetch('/api/menu');
        if (!response.ok) throw new Error('Failed to fetch menu');
        menuItems = await response.json();
        renderMenu();
    } catch (err) {
        console.error(err);
        if (menuGrid) {
            menuGrid.innerHTML = `<div class="error-msg"><i class="fa-solid fa-triangle-exclamation"></i> Error connecting to backend API (${err.message}).</div>`;
        }
    }
}

// Render Menu Cards
function renderMenu() {
    const menuGrid = document.getElementById('menu-grid');
    const searchInput = document.getElementById('menu-search');
    if (!menuGrid) return;

    const searchTerm = searchInput ? searchInput.value.toLowerCase().trim() : '';

    const filtered = menuItems.filter(item => {
        const matchesCategory = currentCategory === 'All' || item.category.toLowerCase() === currentCategory.toLowerCase();
        const matchesSearch = item.name.toLowerCase().includes(searchTerm) || 
                              item.description.toLowerCase().includes(searchTerm) ||
                              (item.tags && item.tags.toLowerCase().includes(searchTerm));
        return matchesCategory && matchesSearch;
    });

    if (filtered.length === 0) {
        menuGrid.innerHTML = `<div class="empty-msg"><i class="fa-solid fa-utensils"></i> No delicious items match your search.</div>`;
        return;
    }

    menuGrid.innerHTML = filtered.map(item => `
        <div class="food-card">
            <div class="food-img-wrapper">
                <img src="${item.imageUrl || '/images/burger.jpg'}" alt="${item.name}" onerror="this.src='/images/burger.jpg'">
                ${item.tags ? `<span class="badge-tag">${item.tags.split(',')[0]}</span>` : ''}
                <span class="rating-tag"><i class="fa-solid fa-star"></i> ${item.rating}</span>
            </div>
            <div class="food-info">
                <div class="food-header">
                    <h3 class="food-title">${item.name}</h3>
                    <span class="food-price">$${item.price.toFixed(2)}</span>
                </div>
                <p class="food-desc">${item.description}</p>
                <div class="food-meta">
                    <span><i class="fa-solid fa-clock"></i> ${item.prepTime}</span>
                    <span><i class="fa-solid fa-fire"></i> ${item.category}</span>
                </div>
                <button class="btn-add-cart" onclick="window.addToCart('${item.id}')">
                    <i class="fa-solid fa-plus"></i> Add to Order
                </button>
            </div>
        </div>
    `).join('');
}

// Fetch & Display Customer Orders
async function fetchOrders() {
    const ordersList = document.getElementById('orders-list');
    try {
        const res = await fetch('/api/orders');
        const orders = await res.json();
        
        const badge = document.getElementById('active-orders-badge');
        if (badge) {
            badge.textContent = orders.filter(o => o.status !== 'DELIVERED' && o.status !== 'CANCELLED').length;
        }

        if (!ordersList) return;

        if (orders.length === 0) {
            ordersList.innerHTML = `<div class="empty-msg"><i class="fa-solid fa-box-open"></i> No orders found.</div>`;
            return;
        }

        ordersList.innerHTML = orders.map(order => `
            <div class="order-card">
                <div class="order-header-row">
                    <div>
                        <h3>Order ${order.orderId}</h3>
                        <span style="font-size: 0.85rem; color: var(--text-muted);">${new Date(order.timestamp).toLocaleString()}</span>
                    </div>
                    <span class="order-status-badge status-${order.status}">${order.status.replace(/_/g, ' ')}</span>
                </div>
                <div style="font-size: 0.95rem; color: var(--text-muted);">
                    <strong>Deliver To:</strong> ${order.customerName} (${order.phone}) &bull; ${order.deliveryAddress}
                </div>
                <div class="order-items-summary">
                    ${order.items ? order.items.map(i => `<div>&bull; ${i.quantity}x ${i.name} ($${i.price})</div>`).join('') : ''}
                </div>
                <div style="font-size: 1.1rem; font-weight: 800; color: var(--accent-orange); text-align: right;">
                    Total Paid: $${order.total.toFixed(2)}
                </div>
            </div>
        `).join('');
    } catch (err) {
        console.error(err);
    }
}

// Fetch & Display Kitchen Portal Orders
async function fetchKitchenOrders() {
    const kitchenGrid = document.getElementById('kitchen-orders-grid');
    try {
        const res = await fetch('/api/orders');
        const orders = await res.json();

        if (!kitchenGrid) return;

        if (orders.length === 0) {
            kitchenGrid.innerHTML = `<div class="empty-msg">No kitchen orders available.</div>`;
            return;
        }

        kitchenGrid.innerHTML = orders.map(order => `
            <div class="order-card">
                <div class="order-header-row">
                    <div>
                        <h3>${order.orderId} — ${order.customerName}</h3>
                        <span style="font-size:0.8rem; color: var(--text-muted);">${order.paymentMethod} &bull; ${order.items ? order.items.length : 0} Items</span>
                    </div>
                    <select class="status-selector" onchange="window.updateOrderStatus('${order.orderId}', this.value)">
                        <option value="RECEIVED" ${order.status === 'RECEIVED' ? 'selected' : ''}>RECEIVED</option>
                        <option value="PREPARING" ${order.status === 'PREPARING' ? 'selected' : ''}>PREPARING</option>
                        <option value="OUT_FOR_DELIVERY" ${order.status === 'OUT_FOR_DELIVERY' ? 'selected' : ''}>OUT FOR DELIVERY</option>
                        <option value="DELIVERED" ${order.status === 'DELIVERED' ? 'selected' : ''}>DELIVERED</option>
                        <option value="CANCELLED" ${order.status === 'CANCELLED' ? 'selected' : ''}>CANCELLED</option>
                    </select>
                </div>
                <div class="order-items-summary">
                    ${order.items ? order.items.map(i => `<div>&bull; ${i.quantity}x ${i.name} ${i.notes ? `(${i.notes})` : ''}</div>`).join('') : ''}
                </div>
            </div>
        `).join('');
    } catch (err) {
        console.error(err);
    }
}

// Fetch & Display Analytics
async function fetchAnalytics() {
    try {
        const res = await fetch('/api/analytics');
        const data = await res.json();

        const revEl = document.getElementById('metric-revenue');
        const totEl = document.getElementById('metric-total-orders');
        const actEl = document.getElementById('metric-active-orders');
        const avgEl = document.getElementById('metric-avg-value');

        if (revEl) revEl.textContent = `$${data.totalRevenue.toFixed(2)}`;
        if (totEl) totEl.textContent = data.totalOrders;
        if (actEl) actEl.textContent = data.activeOrders;
        if (avgEl) avgEl.textContent = `$${data.avgOrderValue.toFixed(2)}`;
    } catch (err) {
        console.error(err);
    }
}

// Initialize Event Listeners
document.addEventListener('DOMContentLoaded', () => {
    fetchMenu();

    // Navigation setup
    const navButtons = document.querySelectorAll('.nav-btn');
    const viewPanels = document.querySelectorAll('.view-panel');

    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetViewId = btn.getAttribute('data-target');
            
            navButtons.forEach(b => b.classList.remove('active'));
            viewPanels.forEach(p => p.classList.remove('active'));

            btn.classList.add('active');
            const targetPanel = document.getElementById(targetViewId);
            if (targetPanel) targetPanel.classList.add('active');

            if (targetViewId === 'orders-view') fetchOrders();
            if (targetViewId === 'kitchen-view') fetchKitchenOrders();
            if (targetViewId === 'analytics-view') fetchAnalytics();
        });
    });

    // Cart toggle listeners
    const cartToggleBtn = document.getElementById('cart-toggle-btn');
    const closeCartBtn = document.getElementById('close-cart-btn');
    const drawerOverlay = document.getElementById('cart-drawer-overlay');

    if (cartToggleBtn) cartToggleBtn.addEventListener('click', openCart);
    if (closeCartBtn) closeCartBtn.addEventListener('click', closeCart);
    if (drawerOverlay) drawerOverlay.addEventListener('click', closeCart);

    // Category filter pills
    const categoryPills = document.getElementById('category-pills');
    if (categoryPills) {
        categoryPills.addEventListener('click', (e) => {
            if (e.target.classList.contains('pill')) {
                document.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));
                e.target.classList.add('active');
                currentCategory = e.target.getAttribute('data-category');
                renderMenu();
            }
        });
    }

    // Search input listener
    const searchInput = document.getElementById('menu-search');
    if (searchInput) {
        searchInput.addEventListener('input', renderMenu);
    }

    // Apply promo listener
    const applyPromoBtn = document.getElementById('apply-promo-btn');
    const promoCodeInput = document.getElementById('promo-code-input');
    const promoFeedback = document.getElementById('promo-feedback');

    if (applyPromoBtn) {
        applyPromoBtn.addEventListener('click', async () => {
            const code = promoCodeInput.value.toUpperCase().trim();
            if (!code) return;

            try {
                const response = await fetch('/api/coupons/validate', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ code: code })
                });

                if (response.ok) {
                    appliedCoupon = await response.json();
                    promoFeedback.innerHTML = `<span class="green-text"><i class="fa-solid fa-check-circle"></i> Applied: ${appliedCoupon.description}</span>`;
                    calculateBill();
                } else {
                    appliedCoupon = null;
                    promoFeedback.innerHTML = `<span style="color:#EF4444;"><i class="fa-solid fa-circle-xmark"></i> Invalid or expired coupon.</span>`;
                    calculateBill();
                }
            } catch (err) {
                console.error(err);
            }
        });
    }

    // Checkout form listener
    const checkoutForm = document.getElementById('checkout-form');
    if (checkoutForm) {
        checkoutForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (cart.length === 0) {
                alert('Your basket is empty. Please add food items before placing an order.');
                return;
            }

            const orderData = {
                customerName: document.getElementById('cust-name').value,
                phone: document.getElementById('cust-phone').value,
                deliveryAddress: document.getElementById('cust-address').value,
                paymentMethod: document.getElementById('cust-payment').value,
                promoCode: appliedCoupon ? appliedCoupon.code : '',
                items: cart.map(c => ({
                    foodId: c.foodId,
                    name: c.item.name,
                    price: String(c.item.price),
                    quantity: String(c.quantity),
                    notes: c.notes || ''
                }))
            };

            try {
                const response = await fetch('/api/orders', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(orderData)
                });

                if (response.ok) {
                    const createdOrder = await response.json();
                    cart = [];
                    appliedCoupon = null;
                    if (promoCodeInput) promoCodeInput.value = '';
                    if (promoFeedback) promoFeedback.innerHTML = '';
                    updateCartUI();
                    closeCart();

                    alert(`🎉 Order ${createdOrder.orderId} Placed Successfully!`);
                    const navOrders = document.querySelector('[data-target="orders-view"]');
                    if (navOrders) navOrders.click();
                } else {
                    alert('Failed to place order. Please try again.');
                }
            } catch (err) {
                console.error(err);
                alert('Server error connecting to BiteExpress backend.');
            }
        });
    }

    // Auto-refresh active order counts every 5 seconds
    setInterval(() => {
        fetch('/api/orders')
            .then(res => res.json())
            .then(orders => {
                const badge = document.getElementById('active-orders-badge');
                if (badge) {
                    badge.textContent = orders.filter(o => o.status !== 'DELIVERED' && o.status !== 'CANCELLED').length;
                }
            }).catch(() => {});
    }, 5000);
});
