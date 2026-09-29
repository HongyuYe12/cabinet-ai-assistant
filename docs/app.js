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
    const design = generateLocally(data);
    result.innerHTML = `<h2>${design.layoutType}方案</h2><p>风格：${design.style || '未指定'} | 预估总价：<strong>${design.estimatedPrice.toLocaleString()} 元</strong></p><p>可用余量：${design.remainingWallSpaceMm} mm</p><h3>柜体清单</h3><ul>${design.cabinets.map(item => `<li>${item.position}：${item.name}（${item.sku}，${item.price} 元）</li>`).join('')}</ul><h3>需确认事项</h3><ul>${design.warnings.map(warning => `<li>${warning}</li>`).join('')}</ul>`;
  } catch (error) {
    result.textContent = '生成失败，请确认服务已启动。';
  }
});

function generateLocally(data) {
  const cabinets = [];
  const addRun = (length, position) => {
    const standard = [
      ['BASE-SINK-800', '800水槽地柜', 800, 2200],
      ['BASE-DRAWER-600', '600抽屉地柜', 600, 1800],
      ['BASE-HOB-900', '900灶台地柜', 900, 2600]
    ];
    const base = standard.reduce((sum, item) => sum + item[2], 0);
    if (length < base) {
      cabinets.push({sku: `BASE-FILLER-${length}`, name: `${length}补充地柜`, widthMm: length, price: length * 2, position});
      return;
    }
    standard.forEach(([sku, name, widthMm, price]) => cabinets.push({sku, name, widthMm, price, position}));
    let remaining = length - base;
    while (remaining >= 300) {
      const width = Math.min(600, remaining);
      cabinets.push({sku: `BASE-FILLER-${width}`, name: `${width}补充地柜`, widthMm: width, price: width * 2, position});
      remaining -= width;
    }
  };
  addRun(data.wallAmm, 'A墙');
  if (data.layoutType !== 'STRAIGHT') addRun(data.wallBmm, 'B墙');
  if (data.layoutType === 'U_SHAPED') addRun(data.wallCmm, 'C墙');
  const estimatedPrice = cabinets.reduce((sum, item) => sum + item.price, 0);
  const warnings = ['方案为初步估算，尺寸、报价及水电燃气位置均需复尺确认。'];
  if (data.layoutType === 'U_SHAPED') warnings.unshift('U型厨房请现场确认中间通道不小于900mm。');
  if (estimatedPrice > data.budget) warnings.unshift(`当前组合超出预算 ${estimatedPrice - data.budget} 元，可替换材质或减少增配。`);
  return {layoutType: {STRAIGHT: '一字型', L_SHAPED: 'L型', U_SHAPED: 'U型'}[data.layoutType], style: data.style, cabinets, estimatedPrice, remainingWallSpaceMm: 0, warnings};
}
