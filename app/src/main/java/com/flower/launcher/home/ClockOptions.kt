package com.flower.launcher.home

data class ClockFontOption(val label: String, val deviceFontFamily: String?)

val CLOCK_FONTS: List<ClockFontOption> = listOf(
    ClockFontOption("系统默认", null),
    ClockFontOption("无衬线", "sans-serif"),
    ClockFontOption("衬线", "serif"),
    ClockFontOption("等宽", "monospace"),
    ClockFontOption("手写", "cursive"),
    ClockFontOption("休闲", "casual"),
    ClockFontOption("窄体", "sans-serif-condensed"),
    ClockFontOption("窄体细", "sans-serif-condensed-light"),
    ClockFontOption("细体", "sans-serif-light"),
    ClockFontOption("超细", "sans-serif-thin"),
    ClockFontOption("中等", "sans-serif-medium"),
    ClockFontOption("特粗", "sans-serif-black"),
    ClockFontOption("小大写", "sans-serif-smallcaps"),
    ClockFontOption("衬线等宽", "serif-monospace"),
)

data class ClockWeightOption(val label: String, val weight: Int)

val CLOCK_WEIGHTS: List<ClockWeightOption> = listOf(
    ClockWeightOption("特细", 100),
    ClockWeightOption("细", 300),
    ClockWeightOption("常规", 400),
    ClockWeightOption("中等", 500),
    ClockWeightOption("半粗", 600),
    ClockWeightOption("粗", 700),
    ClockWeightOption("特粗", 900),
)
