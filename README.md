# 语雀wear

一个给 **小米手表 5（基于安卓、非 Wear OS、无 WebView/GMS）** 打造的**第三方非官方**语雀客户端,用 **Jetpack Compose for Wear** 编写。在手表上浏览知识库文档、记录与阅读小记/快记。

> 本项目与语雀（Yuque）官方无关,未获授权或背书。仅供个人学习与便捷访问,**禁止商业用途**。

## 功能

- **三标签主页**:快记 · 小记 · 我的(头像 / 收藏 / 最近 / 搜索 / 全部知识库)
- **快记知识库**:指定一个知识库当快速记录区(走官方 API,稳定),可与「小记」共存或二选一
- **小记**(实验):浏览器 Cookie 直连语雀网页端小记,支持读/建/删/分页;可选自动续期
- **阅读器**:Markdown 渲染、表格转文字、清理 lake/HTML、代码块、图片**就地渲染 + 放大器**、引用文档 `[n]` 跳转
- **编辑器**:分段输入 + 改行 + 快捷插入(适配手表全屏输入法);可编辑已有文档
- **离线缓存**:目录树 + 打开过的文档,断网可读;缓存管理与自动清理
- **个性化**:主题(绿色/Monet/自定义)、背景(无/纯色/光效/波普点,可动/静)、全局缩放、字号、一言(hitokoto)、出血边、表冠震动开关等
- **隐私**:Token / Cookie / 密码用 Android Keystore **AES-GCM 加密**存于本机(无可用 keystore 时回退明文)

## 构建

需要 Android Studio(含 JDK 17、Android SDK 34)。

```bash
./gradlew :app:assembleDebug        # 调试包
./gradlew :app:assembleRelease      # release(R8 混淆,需自配签名)
```

手表通过 ADB 无线调试或 Shizuku 安装 `app/build/outputs/apk/debug/app-debug.apk`。

## 首次使用

打开 App 走引导:同意协议 → 选快记方式(小记/快记/共存)→ 填 Token(官方)或 Cookie(小记)→ 缩放/主题 → 完成。
- **Token**:yuque.com → 头像 → 设置 → Token
- **小记 Cookie / 接口路径**:浏览器 F12 抓取,填到 设置→账号与安全 / 调试

## 技术栈与致谢

- Jetpack Compose for Wear OS · AndroidX Lifecycle / Navigation
- OkHttp · kotlinx.serialization / coroutines · Coil
- 语雀开放 API · hitokoto.cn(一言)
- Kotlin / Android Open Source Project

代码由 **Claude (Anthropic)** 编写,创建者 **1Ghz**。

## 许可

[MIT](LICENSE)。第三方接口(小记/登录)为逆向、非官方,可能随时失效,使用风险自负。
