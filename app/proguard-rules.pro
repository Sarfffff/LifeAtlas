-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# 高德地图 SDK 依赖运行时类名和反射能力，Release 混淆时保留。
-keep class com.amap.** { *; }
-keep class com.autonavi.** { *; }
-dontwarn com.amap.**
-dontwarn com.autonavi.**

# Room schema、实体和 DAO 已由编译期生成代码使用，保留数据层模型便于迁移和崩溃排查。
-keep class com.xiaoyin.lifeatlas.data.entity.** { *; }
-keep class com.xiaoyin.lifeatlas.data.dao.** { *; }
-keep class com.xiaoyin.lifeatlas.core.database.** { *; }

# 备份导入导出是 V1.0 核心能力，保留序列化模型和生成 serializer。
-keep class com.xiaoyin.lifeatlas.data.export.** { *; }
-keep class **$$serializer { *; }
-keepclassmembers class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}
