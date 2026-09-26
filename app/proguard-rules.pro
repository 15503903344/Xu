# Sure-Xu ProGuard Rules（修正版）

# ============================================================
# 1. Xposed / libxposed
# ============================================================
-keep class io.github.libxposed.** { *; }
-dontwarn io.github.libxposed.**
-keep class com.surexu.sesame.hook.ApplicationHook { *; }
-keepclassmembers class com.surexu.sesame.hook.ApplicationHook {
    public <init>(...);
}
-keep class * implements io.github.libxposed.api.XposedModule { *; }
# 传统框架按 xposed_init 反射加载的入口类
-keep class com.surexu.sesame.hook.LegacyEntry { *; }

# ============================================================
# 2. Model 系统
# ============================================================
-keep class com.surexu.sesame.data.Model { *; }
-keep class com.surexu.sesame.data.ModelType { *; }
-keep class com.surexu.sesame.data.ModelGroup { *; }
-keep class com.surexu.sesame.data.ModelFields { *; }
-keep class com.surexu.sesame.data.ModelConfig { *; }
-keep class com.surexu.sesame.data.ModelField { *; }
-keep class com.surexu.sesame.data.modelFieldExt.** { *; }
-keepclassmembers class com.surexu.sesame.data.Model {
    public <init>(...);
}
-keepclassmembers class com.surexu.sesame.data.modelFieldExt.** {
    public <init>(...);
}
-keep class com.surexu.sesame.data.**$* { *; }

# ============================================================
# 3. data.task 包（反射实例化）
# ============================================================
-keep class com.surexu.sesame.data.task.** { *; }

# ============================================================
# 4. 配置类（Jackson 序列化）
# ============================================================
-keep class com.surexu.sesame.data.ConfigV2 { *; }
-keep class com.surexu.sesame.data.ConfigPreload { *; }
-keep class com.surexu.sesame.data.AppConfig { *; }
-keep class com.surexu.sesame.data.TokenConfig { *; }
-keepclassmembers class com.surexu.sesame.data.ConfigV2 {
    public <init>(...);
}
-keepclassmembers class com.surexu.sesame.data.AppConfig {
    public <init>(...);
}
-keepclassmembers class com.surexu.sesame.data.TokenConfig {
    public <init>(...);
}

# ============================================================
# 5. 状态与统计
# ============================================================
-keep class com.surexu.sesame.util.Status { *; }
-keep class com.surexu.sesame.util.Statistics { *; }
-keepclassmembers class com.surexu.sesame.util.Status {
    public static ** INSTANCE;
}
-keepclassmembers class com.surexu.sesame.util.Statistics {
    public static ** INSTANCE;
}

# ============================================================
# 6. RPC
# ============================================================
-keep class com.surexu.sesame.hook.RpcRequest { *; }
-keep class com.surexu.sesame.hook.ServerCommon { *; }
-keep class com.surexu.sesame.hook.BaseHandler { *; }
-keep class com.surexu.sesame.rpc.bridge.* { *; }
-keepclassmembers class com.surexu.sesame.hook.RpcRequest {
    public <init>(...);
}

# ============================================================
# 7. idMap
# ============================================================
-keep class com.surexu.sesame.util.idMap.** { *; }

# ============================================================
# 8. Entity
# ============================================================
-keep class com.surexu.sesame.entity.** { *; }

# ============================================================
# 9. 扩展模块
# ============================================================
-keep class com.surexu.sesame.model.extensions.** { *; }
-keepclassmembers class com.surexu.sesame.model.extensions.ExtensionsHandle {
    public static java.lang.Object handleAlphaRequest(java.lang.String, java.lang.String, java.lang.Object);
}

# ============================================================
# 10. Hook 包（保持原有整包保留，避免反射调用崩溃）
# ============================================================
-keep class com.surexu.sesame.hook.** { *; }

# ============================================================
# 11. 工具类
# ============================================================
-keep class com.surexu.sesame.util.XHelpers { *; }
-keep class com.surexu.sesame.util.compat.** { *; }
-keep class com.surexu.sesame.util.ClassUtil { *; }
-keep class com.surexu.sesame.util.FileUtil { *; }
-keep class com.surexu.sesame.util.Log { *; }
-keep class com.surexu.sesame.util.JsonUtil { *; }
-keep class com.surexu.sesame.util.TimeUtil { *; }
-keep class com.surexu.sesame.util.NotificationUtil { *; }
-keep class com.surexu.sesame.util.PermissionUtil { *; }
-keep class com.surexu.sesame.util.StringUtil { *; }
-keep class com.surexu.sesame.util.ThreadUtil { *; }
-keep class com.surexu.sesame.util.ToastUtil { *; }
-keep class com.surexu.sesame.util.TypeUtil { *; }

# ============================================================
# 12. Model 实现类 / UI
# ============================================================
-keep class com.surexu.sesame.model.** { *; }
-keep class com.surexu.sesame.ui.** { *; }
-keep class com.surexu.sesame.SesameApplication { *; }

# ============================================================
# 13. 通用：保留 Lombok 生成的 getter/setter（R8 可能误删）
# ============================================================
-keepclassmembers class ** {
    public * get*();
    public void set*(...);
}

# ============================================================
# 14. Jackson 注解字段/方法保留（防 R8 重命名 Jackson 注解字段）
# ============================================================
-keepclassmembers class * {
    @com.fasterxml.jackson.annotation.* <fields>;
    @com.fasterxml.jackson.annotation.* <methods>;
}
-dontwarn java.beans.**

# ============================================================
# 15. 第三方库
# ============================================================
-keep class com.fasterxml.jackson.** { *; }
-keep class fi.iki.elonen.** { *; }
-dontwarn fi.iki.elonen.**
-keep class org.nanohttpd.** { *; }
-dontwarn org.nanohttpd.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ============================================================
# 16. AppCompat Tab 组件（如遇 TabAdapter 崩溃再启用）
# ============================================================
# -keep class androidx.appcompat.widget.ScrollingTabContainerView { *; }
# -keep class androidx.appcompat.widget.ScrollingTabContainerView$* { *; }
# -keep class androidx.appcompat.widget.AbsActionBarView { *; }
# -keep class androidx.appcompat.widget.AbsActionBarView$* { *; }

# ============================================================
# 17. Sure-Xu 免 root 兼容层：modern / legacy 双入口与后端
# ============================================================
-keep class com.surexu.sesame.hook.** { *; }
-keepclassmembers class com.surexu.sesame.hook.** {
    public <init>(...);
    public *;
}
# legacy 传统 Xposed API(compileOnly api-82.jar)运行时由框架提供,编译期勿告警
-dontwarn de.robv.android.xposed.**
-keep class de.robv.android.xposed.** { *; }

# ============================================================
# 18. miuix / Compose UI 入口
# ============================================================
-keep class com.surexu.sesame.ui.** { *; }
-keepclassmembers class com.surexu.sesame.ui.** {
    public <init>(...);
}
