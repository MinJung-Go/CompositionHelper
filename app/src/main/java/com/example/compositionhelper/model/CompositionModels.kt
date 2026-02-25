package com.example.compositionhelper.model

// 构图类型枚举
enum class CompositionType(val displayName: String, val icon: String) {
    RULE_OF_THIRDS("三分法", "grid_3x3"),
    CENTER("中心构图", "center_focus_strong"),
    DIAGONAL("对角线", "line_diagonal"),
    FRAME("框架构图", "crop_square"),
    LEADING_LINES("引导线", "show_chart"),
    S_CURVE("S形曲线", "wave"),
    GOLDEN_SPIRAL("黄金螺旋", "all_inclusive"),
    GOLDEN_TRIANGLE("黄金三角", "change_history"),
    SYMMETRY("对称构图", "sync"),
    NEGATIVE_SPACE("负空间", "crop_square"),
    PATTERN_REPEAT("模式重复", "grid_on"),
    TUNNEL("隧道式", "exit_to_app"),
    SPLIT("分割构图", "view_week"),
    PERSPECTIVE("透视焦点", "visibility"),
    INVISIBLE_LINE("隐形线", "arrow_forward"),
    FILL_FRAME("充满画面", "panorama"),
    LOW_ANGLE("低角度", "arrow_upward"),
    HIGH_ANGLE("高角度", "arrow_downward"),
    DEPTH_LAYER("深度层次", "layers")
}

// 构图分类
enum class CompositionCategory(val displayName: String) {
    CLASSIC("经典"),
    MODERN("现代"),
    PERSPECTIVE("视角")
}

// 获取构图类型属于哪个分类
fun CompositionType.getCategory(): CompositionCategory {
    return when (this) {
        CompositionType.RULE_OF_THIRDS, CompositionType.CENTER, CompositionType.DIAGONAL,
        CompositionType.FRAME, CompositionType.LEADING_LINES, CompositionType.S_CURVE,
        CompositionType.GOLDEN_SPIRAL -> CompositionCategory.CLASSIC

        CompositionType.GOLDEN_TRIANGLE, CompositionType.SYMMETRY, CompositionType.NEGATIVE_SPACE,
        CompositionType.PATTERN_REPEAT, CompositionType.TUNNEL, CompositionType.SPLIT,
        CompositionType.PERSPECTIVE -> CompositionCategory.MODERN

        CompositionType.INVISIBLE_LINE, CompositionType.FILL_FRAME, CompositionType.LOW_ANGLE,
        CompositionType.HIGH_ANGLE, CompositionType.DEPTH_LAYER -> CompositionCategory.PERSPECTIVE
    }
}
