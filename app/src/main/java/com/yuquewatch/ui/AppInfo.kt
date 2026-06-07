package com.yuquewatch.ui

import com.yuquewatch.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Version, build metadata, changelog and credits shown on the 关于 / 版本日志 screens. */
object AppInfo {

    const val APP_NAME = "语雀wear"
    val version: String = BuildConfig.VERSION_NAME

    val buildTime: String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(BuildConfig.BUILD_TIME_MS))

    data class Change(val version: String, val date: String, val items: List<String>)

    /** Newest first. Add a new entry every release. */
    val changelog: List<Change> = listOf(
        Change("Beta1.0", "2026-06-07", listOf(
            "设置重排为：个性化/阅读与编辑器/账号与安全/缓存/调试/关于",
            "快记知识库 + 三标签(快记/小记/我的)、本地收藏、最近、搜索",
            "编辑器(分段/改行/快捷)、编辑已有文档、复制分享、代码块",
            "离线缓存(目录树+文档)、缓存管理与自动清理",
            "图标集中化(AppIcons)、修复返回标签丢失",
            "凭据本地加密、R8 混淆，准备开源",
        )),
        Change("Beta0.9", "2026-06-07", listOf(
            "阅读器修复：清理 <font> 等 HTML、HTML 表格转文字",
            "文档详情改用 Markdown 正文，表格更正常",
            "删除文档改为二次确认",
            "新增独立「阅读」设置页；出血边可自定义",
            "图片渲染：可设为不显示/点按显示/直接显示",
            "数据源选项卡加图标",
            "小记新建改用真实 update 接口（实验）",
            "小记删除改用 batchDelete 接口",
            "图片与引用插入正文对应位置，清理图片链接，引用改为 [n] 角标",
            "新增图片放大器（点图全屏，双指缩放/拖动）",
            "修复多级引用返回错乱（每文档独立加载）",
            "小记图片现在会加载显示",
            "原始响应改为可记录的「记录」，并加清理缓存",
            "版本记录并入「关于」，操作员改称主理人",
            "版本号统一为 Beta0.x",
        )),
        Change("Beta0.8", "2026-06-06", listOf(
            "修复滑动返回透出下层界面（每屏自绘不透明背景）",
            "所有界面上下留出出血边，防圆屏裁切",
            "背景细化：点阵密度/强度可调，默认更密更小",
            "关键字体加粗；问候语加粗",
            "主页知识库/小记加图标；新建加号增大",
            "主页设置入口去掉图标只留文字",
            "一言加载时显示加载圈",
            "小记支持「加载更多」分页",
        )),
        Change("Beta0.7", "2026-06-06", listOf(
            "修复背景不显示（主题透明 + AppBackground 自绘底色）",
            "背景可见度增强，动态模式更明显",
            "设置分层为子页面，更易操作",
            "加载内容时显示加载动画",
            "问候语字号可自定义",
            "菜单引入图标",
        )),
        Change("Beta0.6", "2026-06-06", listOf(
            "更换为官方语雀图标；启动闪屏提速",
            "修复小记内容解析（content.abstract）",
            "新增初次使用引导 + 使用协议",
            "背景系统：无/纯色/光效/波普点，可静/动",
            "全局界面缩放 + 阅读字号调节",
            "主页一言（hitokoto），可选类型",
            "设置返回自动保存",
            "调试：语雀API连通测试 + 离线阅读器自检两个演示页",
        )),
        Change("Beta0.5", "2026-06-06", listOf(
            "更名「语雀wear」、新图标、新包名、版本号体系",
            "主题系统：Monet 取色 / 自定义颜色 / 纯黑背景可选",
            "新增表冠震动开关",
            "修复小记「无内容」：正确解析 content.abstract",
            "调试开关：调用语雀 API 时 Toast 剩余次数",
            "优化加载逻辑、缓存+节流，缓解 429 限流",
            "新建小记按钮移回顶部；新增版本日志/关于页",
        )),
        Change("Beta0.4", "2026-06-06", listOf(
            "修复手表闪退（补 WearHapticFeedbackConstants 桩类）",
            "浏览全部知识库、按目录文件夹分组",
            "文档内表格转为可读文字",
            "正文中引用的语雀文档可跳转",
            "小记原始响应调试页、Cookie 自动续期（实验）",
        )),
        Change("Beta0.3", "2026-06-06", listOf(
            "官方/小记双数据源并存可切换，设置分离",
            "主页时段问候 + 用户名",
        )),
        Change("Beta0.2", "2026-06-06", listOf(
            "浏览全部知识库与文档；非文档类型去格式只留文字",
        )),
        Change("Beta0.1", "2026-06-06", listOf(
            "首版：Wear Compose 界面，官方 Token + 小记两种数据源",
        )),
    )

    val credits: List<String> = listOf(
        "Jetpack Compose for Wear OS",
        "OkHttp（Square）",
        "kotlinx.serialization / coroutines",
        "AndroidX Lifecycle / Activity Compose",
        "语雀开放 API",
        "Kotlin / Android Open Source Project",
    )
}
