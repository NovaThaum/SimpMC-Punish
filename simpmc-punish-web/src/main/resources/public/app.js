const state = {
  mode: 'recent',
  lastQuery: ''
};

const els = {
  serverName: document.querySelector('#server-name'),
  databaseLabel: document.querySelector('#database-label'),
  refreshButton: document.querySelector('#refresh-button'),
  searchInput: document.querySelector('#search-input'),
  searchButton: document.querySelector('#search-button'),
  tableTitle: document.querySelector('#table-title'),
  tableStatus: document.querySelector('#table-status'),
  body: document.querySelector('#punishment-body'),
  total: document.querySelector('#stat-total'),
  active: document.querySelector('#stat-active'),
  bans: document.querySelector('#stat-bans'),
  mutes: document.querySelector('#stat-mutes')
};

document.addEventListener('DOMContentLoaded', async () => {
  els.refreshButton.addEventListener('click', refresh);
  els.searchButton.addEventListener('click', search);
  els.searchInput.addEventListener('keydown', event => {
    if (event.key === 'Enter') {
      search();
    }
  });
  await loadConfig();
  await refresh();
});

async function loadConfig() {
  const config = await request('/api/config');
  els.serverName.textContent = config.name || 'SimpMC-Punish 管理面板';
  document.title = els.serverName.textContent;
  els.databaseLabel.textContent = `数据库: ${config.database || '未知'}`;
}

async function refresh() {
  await Promise.all([loadStats(), state.mode === 'search' ? search(false) : loadRecent()]);
}

async function loadStats() {
  const stats = await request('/api/stats');
  els.total.textContent = number(stats.total);
  els.active.textContent = number(stats.active);
  els.bans.textContent = number(stats.bans);
  els.mutes.textContent = number(stats.mutes);
}

async function loadRecent() {
  state.mode = 'recent';
  els.tableTitle.textContent = '最近处罚';
  els.tableStatus.textContent = '正在加载...';
  renderLoading();
  try {
    const data = await request('/api/punishments/recent?limit=30');
    renderRecords(data.punishments || []);
  } catch (error) {
    renderError(error.message);
  }
}

async function search(updateState = true) {
  const query = els.searchInput.value.trim();
  if (!query) {
    await loadRecent();
    return;
  }
  if (updateState) {
    state.mode = 'search';
    state.lastQuery = query;
  }
  els.tableTitle.textContent = `搜索结果: ${state.lastQuery || query}`;
  els.tableStatus.textContent = '正在搜索...';
  renderLoading();
  try {
    const data = await request(`/api/punishments/search?limit=50&query=${encodeURIComponent(state.lastQuery || query)}`);
    renderRecords(data.punishments || []);
  } catch (error) {
    renderError(error.message);
  }
}

async function request(url) {
  const response = await fetch(url, {headers: {'Accept': 'application/json'}});
  const payload = await response.json().catch(() => ({}));
  if (!response.ok || payload.error) {
    throw new Error(payload.message || '请求失败');
  }
  return payload;
}

function renderLoading() {
  els.body.innerHTML = '<tr><td colspan="8" class="empty">正在加载...</td></tr>';
}

function renderError(message) {
  els.tableStatus.textContent = '加载失败';
  els.body.innerHTML = `<tr><td colspan="8" class="empty error">${escapeHtml(message)}</td></tr>`;
}

function renderRecords(records) {
  els.tableStatus.textContent = `${records.length} 条记录`;
  if (!records.length) {
    els.body.innerHTML = '<tr><td colspan="8" class="empty">没有找到处罚记录。</td></tr>';
    return;
  }
  els.body.innerHTML = records.map(record => `
    <tr>
      <td>#${record.id}</td>
      <td>
        <strong>${escapeHtml(record.targetName || '未知玩家')}</strong>
        <small>${escapeHtml(record.targetUuid || '')}</small>
        ${record.targetIp ? `<small>IP: ${escapeHtml(record.targetIp)}</small>` : ''}
      </td>
      <td><span class="type ${escapeHtml(record.type || '')}">${escapeHtml(record.typeName || record.type || '未知')}</span></td>
      <td>${statusBadge(record)}</td>
      <td>${escapeHtml(record.reason || '未填写原因')}</td>
      <td>${escapeHtml(record.staffName || '控制台')}</td>
      <td>${formatTime(record.createdAt)}</td>
      <td>${record.expiresAt ? formatTime(record.expiresAt) : '永久'}</td>
    </tr>
  `).join('');
}

function statusBadge(record) {
  if (record.expired) {
    return '<span class="badge expired">已过期</span>';
  }
  if (record.active) {
    return '<span class="badge active">生效中</span>';
  }
  return '<span class="badge removed">已移除</span>';
}

function formatTime(value) {
  if (!value) {
    return '-';
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

function number(value) {
  return Number(value || 0).toLocaleString('zh-CN');
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}
