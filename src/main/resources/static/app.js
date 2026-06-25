/**
 * Product Feed – Infinite Scroll with Cursor Pagination
 * 
 * Calls GET /product/generate-feed?cursor=&limit=20&filter=NONE
 * and appends products as the user scrolls to the bottom.
 */

(function () {
    'use strict';

    // ── Config ──────────────────────────────────────────
    const API_BASE = 'http://localhost:8080/product/generate-feed';
    const PAGE_SIZE = 20;

    // ── DOM refs ────────────────────────────────────────
    const grid = document.getElementById('product-grid');
    const loadingIndicator = document.getElementById('loading-indicator');
    const scrollSentinel = document.getElementById('scroll-sentinel');
    const endMessage = document.getElementById('end-message');
    const errorMessage = document.getElementById('error-message');
    const errorText = document.getElementById('error-text');
    const retryBtn = document.getElementById('retry-btn');
    const productCount = document.getElementById('product-count');
    const categoryFilter = document.getElementById('category-filter');

    // ── State ───────────────────────────────────────────
    let currentCursor = '';
    let currentFilter = '';
    let isLoading = false;
    let isEndOfFeed = false;
    let totalLoaded = 0;

    // ── IntersectionObserver (triggers load at bottom) ──
    const observer = new IntersectionObserver(
        (entries) => {
            if (entries[0].isIntersecting && !isLoading && !isEndOfFeed) {
                loadMore();
            }
        },
        { rootMargin: '300px' }   // start loading 300px before the sentinel is visible
    );
    observer.observe(scrollSentinel);

    // ── Filter change ───────────────────────────────────
    categoryFilter.addEventListener('change', () => {
        currentFilter = categoryFilter.value;
        resetFeed();
        loadMore();
    });

    // ── Retry button ────────────────────────────────────
    retryBtn.addEventListener('click', () => {
        hideError();
        loadMore();
    });

    // ── Core: fetch next page ───────────────────────────
    async function loadMore() {
        if (isLoading || isEndOfFeed) return;

        isLoading = true;
        showLoading();
        hideError();

        const params = new URLSearchParams({
            cursor: currentCursor,
            limit: PAGE_SIZE,
            filter: currentFilter
        });

        try {
            const res = await fetch(`${API_BASE}?${params}`);

            if (!res.ok) {
                throw new Error(`Server responded with ${res.status}`);
            }

            const data = await res.json();
            const products = data.productList || [];

            if (products.length === 0 && totalLoaded === 0) {
                // first page came back empty
                showEnd();
            } else {
                renderProducts(products);
            }

            // Advance cursor or mark end
            if (data.encodedCursor) {
                currentCursor = data.encodedCursor;
                console.log(currentCursor);

            } else {
                isEndOfFeed = true;
                showEnd();
            }
        } catch (err) {
            console.error('Feed fetch error:', err);
            showError(err.message || 'Failed to load products');
        } finally {
            isLoading = false;
            hideLoading();
        }
    }

    // ── Rendering ───────────────────────────────────────
    function renderProducts(products) {
        products.forEach((product, i) => {
            const card = document.createElement('div');
            card.className = 'product-card';
            card.style.animationDelay = `${i * 50}ms`;

            const price = formatPrice(product.price);
            const date = formatDate(product.createdAt);
            const time = formatTime(product.createdAt);

            const updatedDate = formatDate(product.updatedAt);

            card.innerHTML = `
                <div class="card-header">
                    <span class="card-title">${escapeHtml(product.name)}</span>
                    <span class="card-id">#${product.id}</span>
                </div>
                <span class="category-badge category-${product.category}">
                    ${product.category}
                </span>
                <div class="card-body">
                    <span class="card-price"><span class="currency">₹</span>${price}</span>
                    <span class="card-date" title="Created">📅 ${date}</span>
                    <span class="card-time">${time}</span>
                    <span class="card-date" title="Updated">🔄 ${updatedDate}</span>
                </div>
            `;

            grid.appendChild(card);
        });

        totalLoaded += products.length;
        updateCount();
    }

    // ── Reset (on filter change) ────────────────────────
    function resetFeed() {
        currentCursor = '';
        isEndOfFeed = false;
        totalLoaded = 0;
        grid.innerHTML = '';
        hideEnd();
        hideError();
        updateCount();
    }

    // ── UI Helpers ──────────────────────────────────────
    function showLoading() { loadingIndicator.style.display = ''; }
    function hideLoading() { loadingIndicator.style.display = 'none'; }

    function showEnd() { endMessage.style.display = ''; }
    function hideEnd() { endMessage.style.display = 'none'; }

    function showError(msg) {
        errorText.textContent = msg;
        errorMessage.style.display = '';
    }
    function hideError() { errorMessage.style.display = 'none'; }

    function updateCount() {
        productCount.textContent = `${totalLoaded} product${totalLoaded !== 1 ? 's' : ''} loaded`;
    }

    // ── Formatting ──────────────────────────────────────
    function formatPrice(value) {
        return Number(value).toLocaleString('en-IN', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        });
    }

    function formatDate(iso) {
        if (!iso) return '';
        const d = new Date(iso);
        return d.toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        });
    }

    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    function formatTime(iso) {
        if (!iso) return '';
        const d = new Date(iso);
        return d.toLocaleTimeString('en-IN', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: true
        });
    }

    // ── Initial load ────────────────────────────────────
    loadMore();

})();
