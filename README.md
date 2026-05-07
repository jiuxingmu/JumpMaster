# JumpMaster

基于 Android + CameraX + MediaPipe Pose Landmarker 的跳绳计数 Demo。  
应用通过识别人体髋部关键点（Hip Y）变化，结合平滑与状态机实现实时计数。

## 技术栈

- Kotlin
- Jetpack Compose
- MVVM + Flow
- CameraX
- MediaPipe Tasks Vision (Pose Landmarker)
- Hilt
- Room（已接入依赖）

## 项目结构

- `app/src/main/java/com/jumpmaster/app/data`：数据层与 MediaPipe 工厂
- `app/src/main/java/com/jumpmaster/app/domain`：核心检测算法（含 `CameraJumpDetector`）
- `app/src/main/java/com/jumpmaster/app/ui`：Compose UI 与 `MainViewModel`
- `app/src/test`：核心算法单元测试
- `docs/prd.md`：需求与方案说明

## 环境要求

- Android Studio（建议最新稳定版）
- JDK 21
- Android SDK 35
- 真机（推荐）或支持 CameraX 的模拟器

## 快速开始

1. 克隆仓库并打开项目：
   - `git clone <your-repo-url>`
   - `cd JumpMaster`
2. 使用 Android Studio 同步 Gradle。
3. 连接 Android 设备并授予相机权限。
4. 运行 `app` 模块。

## 构建与测试

- 构建 Debug 包：
  - `./gradlew :app:assembleDebug`
- 运行核心计数单测：
  - `./gradlew :app:testDebugUnitTest --tests com.jumpmaster.app.domain.camera.CameraJumpDetectorTest`

## 计数算法说明（简版）

`CameraJumpDetector` 的处理流程：

1. 对原始 Hip Y 做边界收敛与滑动平均滤波。
2. 动态维护基线（站立参考高度）与噪声阈值。
3. 用两态状态机计数：
   - `IDLE`：等待进入有效跳跃振幅；
   - `IN_JUMP`：等待回落到基线附近并满足最小振幅与最小间隔后计数。
4. 超时保护：长时间未闭环会自动重置，避免锁死。

## 调试建议

- 观察日志 tag：`JumpMasterPose`
- 重点字段：
  - `hipRaw`：原始髋部值
  - `hip`：滤波后值
  - `base`：当前基线
  - `diff`：与基线差值
  - `Δ`：动态跳跃阈值
  - `counted`：本帧是否触发计数

## Roadmap

- 传感器计数模式（口袋模式）
- 历史记录持久化与可视化
- 参数调优面板（阈值/滤波窗口实时调整）
- 前后台稳定性增强
