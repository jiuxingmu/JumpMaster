
# Role
你是一名资深 Android 开发工程师，精通 Kotlin, Jetpack Compose, 以及移动端 AI 算法闭环实现。

# Task
协助我从零开发一个名为 "RopeMate" 的跳绳计数器 APP。

# Tech Stack
- Language: Kotlin
- UI Framework: Jetpack Compose
- Architecture: MVVM + Clean Architecture
- Local Database: Room
- AI/Vision: Google MediaPipe (Pose Landmarker)
- Camera: CameraX
- Dependency Injection: Hilt
- Async: Coroutines & Flow

# Project Structure Requirement
1. `data`: 除了 Room 数据库，还需包含 SensorProvider 用于获取加速度计数据。
2. `domain`: 包含计数器核心接口及跳绳逻辑模型。
3. `ui`: 使用 Compose 构建，包含主计页面、历史记录页面。
4. `util`: 音效播放器 (SoundPool) 和 语音播报 (TTS) 工具类。

# Step 1: Initialization
请先帮我生成项目的 `libs.versions.toml` 配置文件，包含 CameraX, MediaPipe, Room, Hilt, 以及 Compose 的最新稳定版方案。然后帮我初始化项目的基础目录结构和 Hilt 的 Application 类。

方案 A：针对“摄像头 AI 计数”模块
“现在请帮我实现摄像头计数功能。

集成 CameraX 并将每一帧传递给 MediaPipe Pose Landmarker。
实现一个算法类 CameraJumpDetector。逻辑：监控 Hip（髋部）关键点的 Y 轴坐标。
使用简单滤波算法（如移动平均）处理坐标抖动。
实现状态机：当 Y 坐标持续下降超过阈值 A 进入 JUMPING 状态，随后上升回到阈值 B 时，判定为计数一次。
实时将计数结果通过 Flow 传递给 ViewModel。”
方案 B：针对“手机传感器（装口袋）”模块
“请帮我实现基于手机重力传感器的计数功能。

创建 SensorJumpDetector，通过 SensorManager 监听 LINEAR_ACCELERATION。
计算合加速度：sqrt(x^2 + y^2 + z^2)。
实现波峰检测算法：设定一个加速度阈值（例如 15.0 m/s²）和一个最小时间间隔（例如 250ms，防止连击误报）。
当检测到波峰时，调用辅助音效 SoundPool 发出‘嘀’的一声。
请确保在 UI 切换到后台时，依然可以通过 Foreground Service 保持传感器监听（如果用户选择了该模式）。”