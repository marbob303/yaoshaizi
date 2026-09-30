# 摇色子 Yaoshaizi v0.10

安卓手机上的骰盅摇骰小游戏：打开就是一只骰盅里的 5 颗骰子，晃一晃手机，骰子跟着翻滚，停稳后显示总点数。

## 玩法

1. 打开 App，5 颗骰子静静躺在骰盅里（中国传统配色：1 点和 4 点是红色大点，其余黑色）。
2. **晃动手机**——骰子获得随机初速度，在盅内弹跳翻滚，约 1.5~2 秒后静止，顶部显示 5 颗骰子的**总点数**。
3. 不想晃手机？点底部 **"摇一摇"** 按钮，效果一样。
4. 手机**微微倾斜**时，骰子会顺着倾斜方向轻轻滚动（重力感应）。
5. 右上角喇叭图标可**静音**（碰撞声为程序实时合成，无音频文件）。

## 工程说明

- 包名：`com.marbob303.yaoshaizi`，版本 `0.10`（versionCode 10）
- 技术栈：Kotlin + Jetpack Compose（Material3），单 Activity，无第三方物理引擎
- 界面全部由 Compose Canvas 绘制（赌桌背景、骰盅、骰子、点数），无图片资源
- 物理：自研轻量 2D 物理——骰盅椭圆边界碰撞反射、骰子间圆形碰撞、摩擦减速；最终点数用 `SecureRandom` 生成
- 音效：`AudioTrack` 实时合成短促噪声 burst 模拟碰撞声

### 源码结构

```
app/src/main/java/com/marbob303/yaoshaizi/
├── MainActivity.kt   # 入口：加速度计监听（倾斜/晃动检测）、物理主循环
├── DiceGame.kt       # 游戏状态 + 轻量 2D 物理引擎（椭圆边界、骰子碰撞、摩擦）
├── DiceCanvas.kt     # 全 Canvas 绘制：赌桌、骰盅、骰子、标题、总点数
└── SoundEngine.kt    # AudioTrack 合成碰撞音效
```

## 编译方法

### 环境要求

- JDK 17（Android Gradle Plugin 8.x 要求）
- Android SDK：`platform-tools`、`platforms;android-34`、`build-tools;34.0.0`
  - 设置环境变量 `ANDROID_SDK_ROOT`（或 `ANDROID_HOME`）指向 SDK 目录

### 编译

本工程自带 Gradle Wrapper，无需本机安装 Gradle：

```bash
cd yaoshaizi
./gradlew assembleDebug
```

编译产物：`app/build/outputs/apk/debug/app-debug.apk`

直接安装到手机（需打开 USB 调试）：

```bash
./gradlew installDebug
```

### 一键环境准备（Linux 参考）

```bash
# 1. JDK 17（示例：Temurin）
export JAVA_HOME=$HOME/workspace/.jdks/jdk-17.0.11+9
export PATH=$JAVA_HOME/bin:$PATH

# 2. Android cmdline-tools（解压到 $HOME/workspace/android-sdk/cmdline-tools/latest）
export ANDROID_SDK_ROOT=$HOME/workspace/android-sdk

# 3. 接受许可并安装所需包
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 4. 编译
cd ~/workspace/yaoshaizi
./gradlew assembleDebug
```
