/* ============================================================
   FlashSale 前端逻辑 - frontend/app.js
   纯 Vanilla JS，无框架依赖，由 Nginx 作为静态资源直接提供
   动态请求（/api/*）由 Nginx 转发到后端集群
   ============================================================ */

const API_BASE = '/api';  // Nginx 代理后端，统一前缀

/* ===== 状态 ===== */
let token = localStorage.getItem('fs_token') || '';
let username = localStorage.getItem('fs_user') || '';

/* ===== 初始化 ===== */
document.addEventListener('DOMContentLoaded', () => {
  updateNavUser();
  loadProducts();
  loadSeckillEvents();
  startCountdown();
  if (token) loadOrders();
});

/* ===== HTTP 工具 ===== */
async function request(method, path, body, opts = {}) {
  const t0 = performance.now();
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const res = await fetch(API_BASE + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
    ...opts,
  });

  const elapsed = Math.round(performance.now() - t0);
  // 从响应头获取 Nginx 回写的后端节点信息（X-Upstream-Addr）
  const upstream = res.headers.get('X-Upstream-Addr') || '-';
  const cacheHit  = res.headers.get('X-Cache-Hit') || '-';
  updateInfoBar(upstream, elapsed, cacheHit);

  const data = await res.json().catch(() => ({}));
  return { ok: res.ok, status: res.status, data };
}

function get(path)       { return request('GET',    path); }
function post(path, body){ return request('POST',   path, body); }

/* ===== 导航栏用户状态 ===== */
function updateNavUser() {
  const btnLogin  = document.getElementById('btnShowLogin');
  const btnLogout = document.getElementById('btnLogout');
  const navUser   = document.getElementById('navUsername');
  if (token && username) {
    btnLogin.classList.add('hidden');
    btnLogout.classList.remove('hidden');
    navUser.textContent = '👤 ' + username;
    navUser.classList.remove('hidden');
  } else {
    btnLogin.classList.remove('hidden');
    btnLogout.classList.add('hidden');
    navUser.classList.add('hidden');
  }
}

/* ===== 底部信息栏（展示负载均衡节点）===== */
function updateInfoBar(upstream, ms, cacheHit) {
  document.getElementById('upstreamNode').textContent   = upstream;
  document.getElementById('responseTime').textContent   = ms;
  document.getElementById('cacheHit').textContent       = cacheHit;
}

/* ===== 商品列表 ===== */
async function loadProducts() {
  const grid = document.getElementById('productGrid');
  const { ok, data } = await get('/product/list');
  if (!ok || !data.data) { grid.innerHTML = '<p class="empty-tip">加载失败，请刷新</p>'; return; }

  const products = data.data;
  const EMOJIS = ['📱','💻','⌚','🎧','📷','🎮','🖥️','🖨️'];
  grid.innerHTML = products.length ? products.map((p, i) => `
    <div class="product-card" onclick="showProduct(${p.id})">
      <div class="product-img">${EMOJIS[i % EMOJIS.length]}</div>
      <div class="product-body">
        <div class="product-name">${esc(p.name)}</div>
        <div class="product-price">¥${p.price}</div>
        <div class="product-stock">库存 ${p.stock ?? '充足'}</div>
        <div class="product-actions">
          <button class="btn btn-primary" style="flex:1" onclick="event.stopPropagation();showProduct(${p.id})">查看详情</button>
        </div>
      </div>
    </div>`).join('') : '<p class="empty-tip">暂无商品</p>';
}

/* ===== 商品详情（验证 Redis 缓存）===== */
async function showProduct(id) {
  document.getElementById('buyContent').innerHTML = '<p style="text-align:center;padding:20px">加载中...</p>';
  showModal('buyModal');
  const { ok, data } = await get(`/product/${id}`);
  if (!ok) { document.getElementById('buyContent').innerHTML = '<p class="error-msg">加载失败</p>'; return; }
  const p = data.data;
  document.getElementById('buyContent').innerHTML = `
    <h3>${esc(p.name)}</h3>
    <p style="color:var(--text-muted);margin:8px 0 16px">${esc(p.description || '暂无描述')}</p>
    <div style="font-size:1.8rem;font-weight:800;color:var(--primary);margin-bottom:12px">¥${p.price}</div>
    <p style="color:var(--text-muted);font-size:.85rem;margin-bottom:16px">库存：${p.stock ?? '-'} · 分类：${esc(p.category || '-')}</p>
    <button class="btn btn-primary w-full" onclick="hideModal('buyModal');showToast('请通过秒杀活动参与抢购','success')">加入抢购</button>`;
}

/* ===== 秒杀活动 ===== */
async function loadSeckillEvents() {
  const area = document.getElementById('seckillArea');
  const { ok, data } = await get('/seckill/events').catch(() => ({ ok: false }));
  if (!ok || !data?.data?.length) {
    area.innerHTML = '<p class="empty-tip">暂无进行中的秒杀活动</p>'; return;
  }
  area.innerHTML = data.data.map(e => {
    const sold = (e.seckillStock ?? 0) - (e.remainStock ?? e.seckillStock ?? 0);
    const pct  = e.seckillStock ? Math.round(sold / e.seckillStock * 100) : 0;
    return `
    <div class="seckill-card">
      <div class="seckill-name">${esc(e.productName || '秒杀商品')}</div>
      <div class="seckill-prices">
        <span class="price-sale">¥${e.seckillPrice}</span>
        <span class="price-orig">¥${e.originalPrice ?? ''}</span>
      </div>
      <div class="progress-bar"><div class="progress-fill" style="width:${pct}%"></div></div>
      <div class="progress-label">已抢 ${pct}%</div>
      <button class="btn btn-primary w-full" style="margin-top:12px" onclick="doSeckill(${e.id})">立即抢购</button>
    </div>`;
  }).join('');
}

async function doSeckill(eventId) {
  if (!token) { showToast('请先登录', 'error'); showModal('loginModal'); return; }
  const { ok, data } = await post(`/seckill/${eventId}`);
  showToast(data.message || (ok ? '抢购成功！' : '抢购失败'), ok ? 'success' : 'error');
  if (ok) { loadOrders(); loadSeckillEvents(); }
}

/* ===== 订单 ===== */
async function loadOrders() {
  if (!token) return;
  const area = document.getElementById('orderArea');
  const { ok, data } = await get('/order/list').catch(() => ({ ok: false }));
  if (!ok || !data?.data?.length) {
    area.innerHTML = '<p class="empty-tip">暂无订单记录</p>'; return;
  }
  const STATUS = { 0:'待支付', 1:'已支付', 2:'已取消', 3:'已退款', 4:'已过期' };
  const BADGE  = { 0:'badge-pending', 1:'badge-paid', 2:'badge-cancelled', 3:'badge-cancelled', 4:'badge-cancelled' };
  area.innerHTML = `<div class="order-list">${data.data.map(o => `
    <div class="order-item">
      <div>
        <div style="font-weight:600">${esc(o.productName || '订单 #' + o.id)}</div>
        <div class="order-meta">${o.createTime || ''} · 订单号：${o.orderNo || o.id}</div>
      </div>
      <div style="text-align:right">
        <div style="font-weight:700;color:var(--primary)">¥${o.totalAmount}</div>
        <span class="badge ${BADGE[o.status] || ''}">${STATUS[o.status] ?? o.status}</span>
      </div>
    </div>`).join('')}</div>`;
}

/* ===== 登录 ===== */
async function login() {
  const u = document.getElementById('loginUsername').value.trim();
  const p = document.getElementById('loginPassword').value;
  const errEl = document.getElementById('loginError');
  errEl.classList.add('hidden');
  if (!u || !p) { showErr(errEl, '请填写用户名和密码'); return; }

  const { ok, data } = await post('/user/login', { username: u, password: p });
  if (ok && data.data?.token) {
    token = data.data.token;
    username = u;
    localStorage.setItem('fs_token', token);
    localStorage.setItem('fs_user', username);
    hideModal('loginModal');
    updateNavUser();
    loadOrders();
    showToast('登录成功，欢迎回来 ' + u, 'success');
  } else {
    showErr(errEl, data.message || '登录失败');
  }
}

/* ===== 注册 ===== */
async function register() {
  const body = {
    username: document.getElementById('regUsername').value.trim(),
    password: document.getElementById('regPassword').value,
    nickname: document.getElementById('regNickname').value.trim(),
    phone:    document.getElementById('regPhone').value.trim(),
  };
  const errEl = document.getElementById('regError');
  const okEl  = document.getElementById('regSuccess');
  errEl.classList.add('hidden'); okEl.classList.add('hidden');
  if (!body.username || !body.password) { showErr(errEl, '用户名和密码不能为空'); return; }

  const { ok, data } = await post('/user/register', body);
  if (ok) {
    okEl.textContent = '注册成功！请登录'; okEl.classList.remove('hidden');
    setTimeout(() => switchModal('registerModal', 'loginModal'), 1200);
  } else {
    showErr(errEl, data.message || '注册失败');
  }
}

/* ===== 退出登录 ===== */
function logout() {
  token = ''; username = '';
  localStorage.removeItem('fs_token');
  localStorage.removeItem('fs_user');
  updateNavUser();
  document.getElementById('orderArea').innerHTML = '<p class="empty-tip">请先登录查看订单</p>';
  showToast('已退出登录');
}

/* ===== 倒计时 ===== */
function startCountdown() {
  const el = document.getElementById('countdown');
  // 简单演示：距下一个整点的倒计时
  function tick() {
    const now  = new Date();
    const next = new Date(now);
    next.setHours(next.getHours() + 1, 0, 0, 0);
    const d = next - now;
    const h = String(Math.floor(d / 3600000)).padStart(2, '0');
    const m = String(Math.floor((d % 3600000) / 60000)).padStart(2, '0');
    const s = String(Math.floor((d % 60000) / 1000)).padStart(2, '0');
    el.textContent = `${h}:${m}:${s}`;
  }
  tick();
  setInterval(tick, 1000);
}

/* ===== Modal 工具 ===== */
function showModal(id) { document.getElementById(id).classList.remove('hidden'); }
function hideModal(id) { document.getElementById(id).classList.add('hidden'); }
function closeModal(e, id) { if (e.target.classList.contains('modal-overlay')) hideModal(id); }
function switchModal(hide, show) { hideModal(hide); showModal(show); }

/* ===== Toast ===== */
let toastTimer;
function showToast(msg, type = '') {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.className = 'toast' + (type ? ' ' + type : '');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.add('hidden'), 3000);
}

/* ===== 工具 ===== */
function showErr(el, msg) { el.textContent = msg; el.classList.remove('hidden'); }
function esc(s) {
  return String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
