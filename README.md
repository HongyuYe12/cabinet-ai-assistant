# AI 橱柜方案助手

一个适合个人项目演示的 Java 初始架构。它以规则引擎组合标准柜体，生成一字型、L 型、U 型厨房的初步柜体清单、预算和复尺提醒。

## 在 VS Code 中运行

1. 用 VS Code 打开此文件夹。
2. 安装推荐的 `Extension Pack for Java` 扩展。
3. 在终端执行：

```powershell
mvn spring-boot:run
```

4. 浏览器访问 `http://localhost:8080`。

也可以从 VS Code 的“运行任务”执行 `Run Cabinet AI Assistant`。

## API 示例

`POST /api/designs/generate`

```json
{
  "layoutType": "L_SHAPED",
  "wallAmm": 3200,
  "wallBmm": 2400,
  "wallCmm": 0,
  "budget": 20000,
  "style": "现代简约",
  "needs": ["dishwasher"]
}
```

## 后续扩展

- 将 `CabinetCatalog` 替换为 SQLite/MySQL 产品库。
- 增加门窗、水电、燃气点位等户型约束。
- 接入效果图模型或在获得授权后接入三维家方案/渲染数据。
- 在审核后导出 BOM、报价单和客户方案页。

## 发布到 GitHub Pages

`docs` 目录是无需 Spring Boot 的静态演示版，已包含本地方案计算逻辑。将项目上传到 GitHub 仓库的 `main` 分支后，Actions 会自动发布；在仓库 Settings → Pages 中将 Source 设为 GitHub Actions。
