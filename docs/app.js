const form = document.querySelector('#design-form');
const aiResult = document.querySelector('#ai-result');
const cadResult = document.querySelector('#cad-result');
let latestAiPayload = null;
const apiBase = (window.CABINET_API_BASE || '').replace(/\/$/, '');

document.querySelector('#ai-button').addEventListener('click', async () => {
  const data = Object.fromEntries(new FormData(form));
  data.budget = Number(data.budget);
  data.needs = applianceNeeds(data);
  aiResult.hidden = false;
  aiResult.textContent = '正在请求 AI 建议...';
  try {
    const body = new FormData();
    body.append('budget', String(data.budget));
    body.append('style', data.style || '');
    body.append('needs', data.needs.join(';'));
    body.append('cadFile', form.elements.cadFile.files[0]);
    const response = await fetch(`${apiBase}/api/ai/design`, { method: 'POST', body });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(payload.message || (response.status === 413 ? 'DXF 文件超过 50MB 上传限制' : 'AI 请求失败'));
    latestAiPayload = payload;
    aiResult.innerHTML = `<h2>AI设计建议</h2><p>${payload.recommendation || ''}</p><p>${payload.layoutAdvice || ''}</p><h3>注意事项</h3><ul>${(payload.warnings || []).map(item => `<li>${item}</li>`).join('')}</ul><button type="button" id="cad-button">确认建议并生成最终CAD</button>`;
    document.querySelector('#cad-button').addEventListener('click', generateFinalCad);
  } catch (error) { aiResult.textContent = error.message; }
});

async function generateFinalCad() {
  const file = form.elements.cadFile.files[0];
  if (!file) { cadResult.hidden = false; cadResult.textContent = '请先上传DXF图纸。'; return; }
  cadResult.hidden = false;
  cadResult.textContent = '正在根据已确认的 AI 建议生成 CAD...';
  try {
    const body = new FormData();
    body.append('file', file);
    body.append('cabinetDepthMm', '600');
    body.append('plan', JSON.stringify(latestAiPayload || {}));
    const response = await fetch(`${apiBase}/api/cad/design`, { method: 'POST', body });
    if (!response.ok) throw new Error(await response.text());
    const url = URL.createObjectURL(await response.blob());
    const link = document.createElement('a');
    link.href = url; link.download = 'designed-kitchen.dxf'; link.click(); URL.revokeObjectURL(url);
    cadResult.textContent = '最终 CAD 已生成并下载：designed-kitchen.dxf';
  } catch (error) { cadResult.textContent = `CAD生成失败：${error.message}`; }
}

function applianceNeeds(data) {
  return [['洗碗机', data.dishwasher], ['烤箱', data.oven], ['蒸箱', data.steamOven], ['冰箱', data.refrigerator], ['油烟机', data.hood]]
    .filter(([, size]) => size && size.trim()).map(([name, size]) => `${name}尺寸：${size}`);
}

document.querySelectorAll('[data-appliance]').forEach((box) => box.addEventListener('change', () => {
  const input = document.querySelector(`[data-for="${box.dataset.appliance}"]`);
  input.hidden = !box.checked;
  if (!box.checked) input.value = '';
}));

document.querySelectorAll('.nav-button').forEach((button) => button.addEventListener('click', () => {
  document.querySelectorAll('.nav-button').forEach((item) => item.classList.toggle('active', item === button));
  document.querySelectorAll('.workspace').forEach((workspace) => workspace.classList.toggle('active', workspace.id === button.dataset.target));
}));

bindTool('floorplan-form', 'floorplan-result', '/api/vision/floorplan', renderFloorplan);
bindTool('cad-structure-form', 'cad-structure-result', '/api/vision/cad-structure', renderCadStructure);
bindTool('match-form', 'match-result', '/api/vision/match', renderMatch);

function bindTool(formId, resultId, path, renderer) {
  const toolForm = document.querySelector(`#${formId}`);
  const result = document.querySelector(`#${resultId}`);
  toolForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    result.hidden = false;
    result.innerHTML = '<p class="loading">正在分析，请稍候...</p>';
    const submit = toolForm.querySelector('button');
    submit.disabled = true;
    try {
      const response = await fetch(`${apiBase}${path}`, { method: 'POST', body: new FormData(toolForm) });
      const payload = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(payload.message || '分析请求失败');
      renderer(result, payload);
    } catch (error) {
      result.innerHTML = `<p class="error">${escapeHtml(error.message)}</p>`;
    } finally { submit.disabled = false; }
  });
}

function renderFloorplan(target, data) {
  target.innerHTML = `${metrics([['墙体覆盖', `${data.wallCoveragePercent}%`], ['房间区域', data.roomRegionCount], ['门窗候选', data.doorWindowCandidateCount], ['分析尺寸', `${data.imageWidth}×${data.imageHeight}`]])}
    <img src="${data.annotatedImage}" alt="户型图墙体、门窗候选和房间区域分析结果">
    <p class="method-note">${escapeHtml(data.method)}<br>${escapeHtml((data.warnings || []).join('；'))}</p>`;
}

function renderCadStructure(target, data) {
  const bounds = data.bounds || {};
  target.innerHTML = `${metrics([['图元总数', data.entityCount], ['已识别图元', data.recognizedEntityCount], ['图层数量', Object.keys(data.layers || {}).length], ['图纸范围', bounds.width ? `${bounds.width} × ${bounds.height}` : '未识别']])}
    <h3>图元类型</h3>${keyValueTable(data.entities)}<h3>图层统计</h3>${keyValueTable(data.layers)}
    <p class="method-note">${escapeHtml(data.unitsNote || '')}</p>`;
}

function renderMatch(target, data) {
  target.innerHTML = `${metrics([['视觉相似度', `${data.similarityPercent}%`], ['变化面积', `${data.changedAreaPercent}%`], ['水平偏移', `${data.alignment.offsetX}px`], ['垂直偏移', `${data.alignment.offsetY}px`]])}
    <img src="${data.differenceHeatmap}" alt="效果图与施工结果差异热力图">
    <p class="method-note">${escapeHtml(data.method)}<br>${escapeHtml((data.warnings || []).join('；'))}</p>`;
}

function metrics(items) {
  return `<div class="metrics">${items.map(([label, value]) => `<div class="metric"><strong>${escapeHtml(value)}</strong><span>${escapeHtml(label)}</span></div>`).join('')}</div>`;
}

function keyValueTable(value = {}) {
  const rows = Object.entries(value);
  if (!rows.length) return '<p class="method-note">没有解析到数据。</p>';
  return `<table class="data-table"><thead><tr><th>名称</th><th>数量</th></tr></thead><tbody>${rows.map(([key, count]) => `<tr><td>${escapeHtml(key)}</td><td>${escapeHtml(count)}</td></tr>`).join('')}</tbody></table>`;
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[char]));
}
