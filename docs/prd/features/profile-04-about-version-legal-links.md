# 子 PRD：关于、版本号与协议链接（我的）

| 字段 | 内容 |
|------|------|
| ID | P-04 |
| Tab | 我的 |
| 优先级 | P1 |
| 依赖 | 无 |
| 预估 | 小 |

## 1. 目标

在「我的」提供**版本信息**与**用户协议 / 隐私政策**入口；链接可指向占位 HTTPS 页，但必须可打开、无崩溃，满足上架前补文案的前置结构。

## 2. 用户故事

作为用户，我想知道当前版本，并能在需要时查看隐私与协议。

## 3. 范围

**In**

- 展示：`版本 x.y.z`（`BuildConfig.VERSION_NAME` 或等价）。
- 两行可点击：`用户协议`、`隐私政策` → `CustomTabsIntent` / 外部浏览器打开 URL（常量配置，占位如 `https://example.com/privacy` 需在发版前替换为真实页）。
- 可选：`重新查看摆放说明`（跳转 C-07 向导，若已实现）。

**Out**

- 应用内 WebView 复杂排版、账号体系。

## 4. 验收标准

1. 无网络时点击：有 Snackbar「网络不可用」或浏览器错误页——**不崩溃**。
2. URL 从单一常量源维护，便于替换。
3. 深色模式下列表可读。

## 5. 合规提醒

占位域名不可用于正式上架；替换真实隐私政策后再走商店审核。

## Cursor 对话（AI 提示）

**单次对话边界**：本 chat 只落地当前子 PRD；不把未列在本文件的大范围重构算作本需求完成。

**首条消息可复制**（推荐用 `@` 附加本文件 `docs/prd/features/profile-04-about-version-legal-links.md`；相关 `*.kt` 由你检索 Tab/类名后 `@` 或写路径）：

> 请按当前仓库子 PRD **P-04**（`profile-04-about-version-legal-links.md`，全文见上下文）实现。严格遵守 `.cursor/rules/jumpmaster-standards.mdc`。以本文 **In / Out** 与文档内全部 **验收标准** 为准，**不实现 Out 中任何项**；不扩大 `docs/prd.md` 与 `README.md` 索引外的范围。完成后逐条对照验收标准与文档中的 **手动测试要点**（及合规类小节若有）；合并说明首行写：`PRD: profile-04-about-version-legal-links.md`。

更细的投喂结构、依赖处理与 Gradle 验证：[`CURSOR-FEED.md`](./CURSOR-FEED.md)。

