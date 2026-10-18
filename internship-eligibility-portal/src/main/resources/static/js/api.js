const API_BASE = '/api';

function getToken() { return localStorage.getItem('token'); }
function getRole() { return localStorage.getItem('role'); }
function getEmail() { return localStorage.getItem('email'); }

/** HTML-escape anything user-controlled before putting it into innerHTML. */
function esc(v) {
  if (v === null || v === undefined) return '';
  return String(v).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function requireAuth(expectedRole) {
  if (!getToken()) { window.location.href = '/index.html'; return; }
  if (expectedRole && getRole() !== expectedRole) {
    alert('This page is for ' + expectedRole + ' accounts. You are logged in as ' + getRole() + '.');
    window.location.href = '/index.html';
  }
}

function logout() {
  localStorage.clear();
  window.location.href = '/index.html';
}

async function request(path, opts) {
  const headers = Object.assign({}, opts.headers || {});
  const token = getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;

  const res = await fetch(API_BASE + path, { method: opts.method || 'GET', headers, body: opts.body || null });
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch (e) { data = text; }

  if (!res.ok) {
    const msg = data && data.message ? data.message : null;
    if ((res.status === 401 || res.status === 403) && !msg && token) {
      localStorage.clear();
      window.location.href = '/index.html';
      throw new Error('Session expired - please log in again');
    }
    throw new Error(msg || ('Request failed (' + res.status + ')'));
  }
  return data;
}

function api(path, method = 'GET', body = null) {
  return request(path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : null
  });
}

function apiForm(path, formData) {
  return request(path, { method: 'POST', body: formData });
}

/** Opens a protected file (document / resume) in a new tab. */
async function openFile(path) {
  const res = await fetch(API_BASE + path, { headers: { 'Authorization': 'Bearer ' + getToken() } });
  if (!res.ok) { showAlert('alert', 'Could not open the file'); return; }
  const blob = await res.blob();
  window.open(URL.createObjectURL(blob), '_blank');
}

function showAlert(elementId, message, type = 'danger') {
  const el = document.getElementById(elementId);
  if (!el) { alert(message); return; }
  el.className = 'alert alert-' + type;
  el.textContent = message;
  el.classList.remove('d-none');
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function setupTabs(onShow) {
  document.querySelectorAll('#tabs button').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('#tabs button').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      document.querySelectorAll('.tab-pane').forEach(p => p.classList.add('d-none'));
      document.getElementById('tab-' + btn.dataset.tab).classList.remove('d-none');
      if (onShow) onShow(btn.dataset.tab);
    });
  });
}

function pill(text) { return '<span class="status-pill">' + esc(String(text).replaceAll('_', ' ')) + '</span>'; }
function fmtDate(v) { return v ? new Date(v).toLocaleString() : '—'; }
function num(id) { const v = document.getElementById(id).value; return v === '' ? null : Number(v); }
