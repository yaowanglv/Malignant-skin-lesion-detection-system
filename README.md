# 恶性皮肤病变智能检测系统

基于 **Vue 3 + Spring Boot + Flask/YOLO** 的恶性皮肤病变检测系统，支持图像/视频检测、检测记录与看板、JWT 登录，以及可选的大模型分析与飞书上传。

仓库地址：https://github.com/yaowanglv/Malignant-skin-lesion-detection-system

## 技术栈

| 部分 | 说明 |
|---|---|
| 前端 | Vue 3 + Vite + Element Plus，开发端口 `4910` |
| 后端 | Spring Boot 3（Java 17+），端口 `1907` |
| 检测服务 | Flask + Ultralytics YOLO，端口 `2026` |
| 数据库 | MySQL 8，库名 `skin` |
| 权重 | `detect_service/models/`（YOLO11 / YOLO8n） |

## 目录结构

```
.
├── vue/                 前端
├── springb/             Spring Boot 后端
├── detect_service/      Python 检测服务与业务权重
├── sql/                 MySQL 结构脚本（仅结构，不含业务数据）
├── mcp/                 可选：MySQL / 飞书 / 钉钉 MCP 辅助配置
└── README.md
```

## 环境要求

- Node.js 18+
- JDK 17+、Maven 3.9+
- MySQL 8
- Python 3.10（检测服务与 conda 环境 `v11dmt` 对齐）
- 可选：NVIDIA GPU + CUDA 12.1（与 `torch==2.2.2+cu121` 一致）

## 1. 初始化数据库

在 MySQL 中按序号执行 `sql/`：

```sql
SOURCE sql/00_init.sql;
SOURCE sql/01_user_auth.sql;
SOURCE sql/02_detection.sql;
SOURCE sql/03_system_config.sql;
SOURCE sql/04_dataview.sql;
SOURCE sql/05_views.sql;
```

| 文件 | 内容 |
|---|---|
| `00_init.sql` | 建库 |
| `01_user_auth.sql` | 管理员表 |
| `02_detection.sql` | 图像/视频检测记录、日志 |
| `03_system_config.sql` | 系统配置、模型配置、用户配置 |
| `04_dataview.sql` | 看板统计表 |
| `05_views.sql` | 看板视图 |

脚本只导出结构。账号需要自己插入。

## 2. 配置后端密钥（不要提交真实值）

```bash
cd springb/src/main/resources
cp application-local.yml.example application-local.yml
```

至少填写：

- MySQL 用户名 / 密码
- `jwt.secret`

可选：DeepSeek / GLM / Kimi API Key、飞书 App ID/Secret。也可用环境变量覆盖，见 `application.yml` 注释。

`application-local.yml` 已被 `.gitignore` 忽略。

## 3. 启动检测服务

```bash
cd detect_service
pip install -r requirements.txt
python detect-api-skin.py
```

或双击 `detect_service/start.bat`。

服务地址：`http://localhost:2026`

权重默认在：

- `detect_service/models/YOLO11/best.pt`
- `detect_service/models/YOLO8n/best.pt`

## 4. 启动后端

```bash
cd springb
mvn spring-boot:run
```

服务地址：`http://localhost:1907`

## 5. 启动前端

```bash
cd vue
npm install
npm run dev
```

浏览器打开：http://localhost:4910/login

前端请求后端 `http://localhost:1907`。

## 端口一览

| 服务 | 端口 |
|---|---|
| Vue | 4910 |
| Spring Boot | 1907 |
| Python 检测 | 2026 |

## 可选：MCP

`mcp/` 仅给 AI 编程客户端用，产品运行不需要。说明见 `mcp/README.md`。

## 不会进入 Git 的内容

- `node_modules/`、`springb/target/`
- `application-local.yml`、`.env*`（保留 `*.example`）
- `.idea/`、`.vscode/`、`.agents/`、`.claude/`
- 运行时 `uploads/`、`results/`、日志
- 除 `detect_service/models/` 以外的 `*.pt` / `*.onnx`
