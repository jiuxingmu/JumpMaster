# 子 PRD：结束训练写入本地会话（开始）

| 字段 | 内容 |
|------|------|
| ID | C-02 |
| Tab | 开始 |
| 优先级 | P0 |
| 依赖 | C-01（结束摘要钩子已存在） |
| 预估 | 小 |

## 1. 目标

用户点击结束并确认后，将**一条会话记录**写入本地（Room 已有实体则复用），使主流程与「历史」数据源打通。

## 2. 用户故事

作为用户，我结束训练后希望数据被保存，以便稍后在历史里查看。

## 3. 范围

**In**

- 在 C-01 的「确认结束」路径上，插入一次**异步写入**（`Dispatchers.IO` + Repository），字段至少包含：`id`（自增）、`startedAt`、`endedAt` 或 `durationMs`、`jumpCount`（与摘要一致）、`createdAt`（若表结构已有则对齐）。
- 写入成功：摘要上简短反馈「已保存」或等价 Snackbar（失败则 Snackbar「保存失败，请重试」+ 可选重试按钮）。
- 不在本需求实现历史 UI；验收通过 **DAO 查询 / 单元测试 / Log** 任一可重复方式验证。

**Out**

- 历史列表 UI（H-01）、结束页 ±1（C-05）、备注字段。

## 4. 验收标准

1. 同一会话结束只产生**一条**记录（防双击重复插入）。
2. 写入失败不崩溃；用户可再次尝试结束或得到明确错误提示。
3. `jumpCount` 与 `durationMs`（或起止时间推导）与 C-01 摘要一致。
4. 主线程无不恰当阻塞。

## 5. 手动测试要点

- 连续完成两次训练 → DB 两条记录，数据互不覆盖。
- 飞行模式 / 无网（若仅本地）不影响保存。

## 6. 实现提示

仅通过 `JumpRepository`（或现有封装）访问 DB；ViewModel 不直接持 Dao。与主 PRD Room 约束一致。

## Cursor 对话（AI 提示）

**单次对话边界**：本 chat 只落地当前子 PRD；不把未列在本文件的大范围重构算作本需求完成。

**首条消息可复制**（推荐用 `@` 附加本文件 `docs/prd/features/counter-02-persist-session-on-end.md`；相关 `*.kt` 由你检索 Tab/类名后 `@` 或写路径）：

> 请按当前仓库子 PRD **C-02**（`counter-02-persist-session-on-end.md`，全文见上下文）实现。严格遵守 `.cursor/rules/jumpmaster-standards.mdc`。以本文 **In / Out** 与文档内全部 **验收标准** 为准，**不实现 Out 中任何项**；不扩大 `docs/prd.md` 与 `README.md` 索引外的范围。完成后逐条对照验收标准与文档中的 **手动测试要点**（及合规类小节若有）；合并说明首行写：`PRD: counter-02-persist-session-on-end.md`。

更细的投喂结构、依赖处理与 Gradle 验证：[`CURSOR-FEED.md`](./CURSOR-FEED.md)。

