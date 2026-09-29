const form = document.querySelector('#design-form');
const result = document.querySelector('#result');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(form));
  ['wallAmm', 'wallBmm', 'wallCmm', 'budget'].forEach((key) => data[key] = Number(data[key]));
  data.needs = [];
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
