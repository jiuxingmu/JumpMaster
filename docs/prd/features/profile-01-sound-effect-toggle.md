# 子 PRD：训练音效开关（我的）

| 字段 | 内容 |
|------|------|
| ID | P-01 |
| Tab | 我的 |
| 优先级 | P1 |
| 依赖 | 无（与 `SoundEffectPlayer` 集成） |
| 预估 | 小 |

## 1. 目标

用户可在「我的」关闭或开启**计数音效**，设置持久化，并在训练中立即生效。

## 2. 用户故事

作为用户，我在公共场合希望关掉提示音，在家再打开。

## 3. 范围

**In**

- UI：`Switch` 或带标题的设置行「训练音效」+ 简短说明。
- 持久化：DataStore 或 SharedPreferences（与 P-02、P-03 统一存储方案）。
- `SoundEffectPlayer`（或调用方）在播放前读取开关；关则不播放、不抛异常。
- 默认值：`开`（与当前行为一致）。

**Out**

- 系统媒体音量控制、自定义音效文件。

## 4. 验收标准

1. 关 → 训练中计数无音效；开 → 恢复有音效（与现有触发点一致）。
2. 杀进程重启后状态保持。
3. 不在 Composable 内直接写磁盘；经 ViewModel 或 `remember` + use case 注入。

## 5. 手动测试要点

- 训练中切换 Tab 改设置再回来（若支持热更）或下一会话生效——**实现时选定一种并在 PR 说明**。

## Cursor 对话（AI 提示）

**单次对话边界**：本 chat 只落地当前子 PRD；不把未列在本文件的大范围重构算作本需求完成。

**首条消息可复制**（推荐用 `@` 附加本文件 `docs/prd/features/profile-01-sound-effect-toggle.md`；相关 `*.kt` 由你检索 Tab/类名后 `@` 或写路径）：

> 请按当前仓库子 PRD **P-01**（`profile-01-sound-effect-toggle.md`，全文见上下文）实现。严格遵守 `.cursor/rules/jumpmaster-standards.mdc`。以本文 **In / Out** 与文档内全部 **验收标准** 为准，**不实现 Out 中任何项**；不扩大 `docs/prd.md` 与 `README.md` 索引外的范围。完成后逐条对照验收标准与文档中的 **手动测试要点**（及合规类小节若有）；合并说明首行写：`PRD: profile-01-sound-effect-toggle.md`。

更细的投喂结构、依赖处理与 Gradle 验证：[`CURSOR-FEED.md`](./CURSOR-FEED.md)。

