const form = document.querySelector('#design-form');
const result = document.querySelector('#result');
const aiResult = document.querySelector('#ai-result');
const cadResult = document.querySelector('#cad-result');
let latestAiPayload = null;

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(form));
  data.budget = Number(data.budget);
  data.layoutType = 'STRAIGHT'; data.wallAmm = 3200; data.wallBmm = 0; data.wallCmm = 0;
  data.needs = applianceNeeds(data);
  result.hidden = false;
  result.textContent = '正在生成方案...';
  try {
    const response = await fetch('/api/designs/generate', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data) });
    if (!response.ok) throw new Error('请求失败');
    const design = await response.json();
    result.innerHTML = `<h2>${design.layoutType}方案</h2><p>风格：${design.style || '未指定'} | 预估总价：<strong>${design.estimatedPrice.toLocaleString()} 元</strong></p><p>可用余量：${design.remainingWallSpaceMm} mm</p><h3>柜体清单</h3><ul>${design.cabinets.map(item => `<li>${item.position}：${item.name}（${item.sku}，${item.price} 元）</li>`).join('')}</ul><h3>需确认事项</h3><ul>${design.warnings.map(warning => `<li>${warning}</li>`).join('')}</ul>`;
  } catch (error) {
    result.textContent = '生成失败，请确认服务已启动。';
  }
});

document.querySelector('#ai-button').addEventListener('click', async () => {
  const data = Object.fromEntries(new FormData(form));
  data.budget = Number(data.budget);
  data.layoutType = 'STRAIGHT'; data.wallAmm = 3200; data.wallBmm = 0; data.wallCmm = 0;
  data.needs = applianceNeeds(data);
  aiResult.hidden = false;
  aiResult.textContent = '正在请求 AI 建议...';
  try {
    const response = await fetch('/api/ai/design', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(data) });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.message || 'AI 请求失败');
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
    const response = await fetch('/api/cad/design', { method: 'POST', body });
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
