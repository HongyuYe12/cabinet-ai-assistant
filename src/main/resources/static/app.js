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
