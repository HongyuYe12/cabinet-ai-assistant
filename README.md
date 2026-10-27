# Cabinet Vision AI

Java 21 + Spring Boot 橱柜设计与视觉分析个人项目。网页包括两个工作区：

- 大模型橱柜设计：上传 DXF 墙体图纸，结合预算、风格和电器尺寸生成建议与最终 DXF。
- 视觉分析实验室：户型图区域分割、DXF 结构解析、效果图与施工结果匹配。

## 本地运行

```powershell
powershell -ExecutionPolicy Bypass -File .\run-with-ai.ps1
```

访问 `http://localhost:8081`。API Key 通过启动脚本输入，不写入代码。

## 视觉分析接口

- `POST /api/vision/floorplan`：上传 PNG/JPG 户型图，返回墙体候选、门窗细线候选、封闭房间区域指标与标注图。
- `POST /api/vision/cad-structure`：上传 ASCII DXF，返回图层、图元类型、数量和坐标范围。
- `POST /api/vision/match`：上传效果图与施工图，返回平移对齐、相似度、变化面积和差异热力图。

图像模块当前采用本地 Java 启发式算法，适合 MVP 验证与流程演示，不冒充训练完成的语义分割模型。后续可在不改变网页调用方式的前提下替换为 ONNX/OpenCV 推理服务。

## 云端部署

- `docs/` 由 GitHub Actions 发布到 GitHub Pages。
- Spring Boot 后端通过 `Dockerfile` 和 `render.yaml` 部署到 Render。
- `AI_API_KEY` 仅保存在 Render 环境变量中。

## 编译验证

```powershell
mvn -q -DskipTests package
```
