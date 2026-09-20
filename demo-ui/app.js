const state = { trace: [], lastResult: null };
const $ = (selector) => document.querySelector(selector);

function setSceneState(scene, status) {
    const element = $(`#${scene}-state`);
    element.textContent = status === 'done' ? 'DONE' : status === 'running' ? 'RUNNING' : status === 'error' ? 'ERROR' : 'READY';
    element.className = `scene-state ${status}`;
}

function addTrace(title, detail, payload, status = 'done') {
    state.trace.push({ title, detail, payload, status });
    renderTrace();
}

function renderTrace() {
    const timeline = $('#timeline');
    $('#trace-counter').textContent = `${state.trace.length} event${state.trace.length === 1 ? '' : 's'}`;
    timeline.innerHTML = state.trace.map((event, index) => `
        <div class="trace-event ${event.status}" style="animation-delay:${index * 55}ms">
            <strong>${escapeHtml(event.title)}</strong>
            <small>${escapeHtml(event.detail)} · ${new Date().toLocaleTimeString()}</small>
            ${event.payload ? `<code>${escapeHtml(event.payload)}</code>` : ''}
        </div>`).join('');
}

function escapeHtml(value) {
    return String(value).replace(/[&<>'"]/g, (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[character]));
}

function showResult(value) {
    state.lastResult = value;
    $('#result-area').hidden = false;
    $('#result-content').textContent = JSON.stringify(value, null, 2);
}

function clearTrace() {
    state.trace = [];
    state.lastResult = null;
    $('#trace-counter').textContent = '0 events';
    $('#timeline').innerHTML = '<div class="empty-trace"><span class="empty-line"></span><strong>Waiting for an operation</strong><p>Run one of the scenes to reveal the request path.</p></div>';
    $('#result-area').hidden = true;
    ['import', 'search', 'order'].forEach((scene) => setSceneState(scene, ''));
}

async function request(path, options = {}) {
    let response;
    let lastError;
    for (let attempt = 0; attempt < 3; attempt++) {
        try {
            response = await fetch(`/api${path}`, options);
            if (response.status !== 502 && response.status !== 503) break;
            lastError = new Error(`HTTP ${response.status}`);
        } catch (error) {
            lastError = error;
        }
        await new Promise((resolve) => setTimeout(resolve, 700));
    }
    if (!response) throw lastError || new Error('API unavailable');
    const text = await response.text();
    let body;
    try { body = text ? JSON.parse(text) : {}; } catch { body = { raw: text }; }
    if (!response.ok) throw new Error(body.message || body.error || `HTTP ${response.status}`);
    return body;
}

async function importCsv() {
    setSceneState('import', 'running');
    const file = $('#csv-file').files[0];
    if (!file) { setSceneState('import', 'error'); addTrace('Import blocked', 'Select a CSV file first', null, 'error'); return; }
    const formData = new FormData();
    formData.append('file', file);
    try {
        addTrace('Browser → ImpEx API', 'POST /impex/import', `${file.name} · multipart/form-data`, 'pending');
        const result = await request('/impex/import', { method: 'POST', body: formData });
        addTrace('Commerce → H2', 'Upsert Product by code', `${result.importedProducts} products written locally`);
        addTrace('ImpEx response → Browser', 'Import completed', JSON.stringify(result));
        showResult(result); setSceneState('import', 'done');
    } catch (error) { addTrace('Import failed', error.message, null, 'error'); setSceneState('import', 'error'); }
}

async function searchProducts() {
    setSceneState('search', 'running');
    const query = $('#search-input').value.trim();
    if (!query) { setSceneState('search', 'error'); addTrace('Search blocked', 'Enter a query first', null, 'error'); return; }
    try {
        addTrace('Browser → OCC API', 'GET /occ/v2/products/search', `query=${query}`, 'pending');
        const products = await request(`/occ/v2/products/search?query=${encodeURIComponent(query)}`);
        addTrace('Commerce → H2', 'FlexibleSearch-style catalog query', `${products.length} local match${products.length === 1 ? '' : 'es'}`);
        products.forEach((product) => {
            addTrace('Commerce → ERP', 'GET price + GET stock', `${product.code} · price ${product.price} · ATP ${product.availableToPromise}`);
        });
        addTrace('Enriched response → Browser', 'OCC result ready', `${products.length} product${products.length === 1 ? '' : 's'} with live ERP data`);
        showResult(products); setSceneState('search', 'done');
    } catch (error) { addTrace('Search failed', error.message, null, 'error'); setSceneState('search', 'error'); }
}

async function createOrder() {
    setSceneState('order', 'running');
    const productCode = $('#order-product').value.trim();
    const quantity = Number($('#order-quantity').value);
    if (!productCode || quantity < 1) { setSceneState('order', 'error'); addTrace('Checkout blocked', 'Product and positive quantity are required', null, 'error'); return; }
    try {
        addTrace('Browser → OCC API', 'POST /occ/v2/orders', `${productCode} · quantity ${quantity}`, 'pending');
        addTrace('Commerce → H2', 'Create local order', 'status = CREATED');
        const order = await request('/occ/v2/orders', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ productCode, quantity }) });
        addTrace('Commerce → ERP', 'POST /erp/orders', `commerceOrderId=${order.id}`);
        addTrace('ERP → Commerce', 'Order confirmed', `${order.erpOrderId} · status ${order.status}`);
        addTrace('Updated order → Browser', `GET /occ/v2/orders/${order.id}`, 'state persisted locally');
        showResult(order); setSceneState('order', 'done');
    } catch (error) { addTrace('Checkout failed', error.message, null, 'error'); setSceneState('order', 'error'); }
}

$('#csv-file').addEventListener('change', (event) => { $('#file-name').textContent = event.target.files[0]?.name || 'products.csv'; });
$('#import-button').addEventListener('click', importCsv);
$('#search-button').addEventListener('click', searchProducts);
$('#order-button').addEventListener('click', createOrder);
$('#reset-button').addEventListener('click', clearTrace);
$('#copy-result').addEventListener('click', async () => { if (state.lastResult) await navigator.clipboard.writeText(JSON.stringify(state.lastResult, null, 2)); });
