# Shizuku user service is instantiated reflectively in a remote process.
-keep class com.lamamics.spacecalculator.shizuku.** { *; }
-keep class com.lamamics.spacecalculator.IUserService { *; }
-keep class com.lamamics.spacecalculator.IUserService$Stub { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keep,includedescriptorclasses class com.lamamics.spacecalculator.model.**$$serializer { *; }
-keepclassmembers class com.lamamics.spacecalculator.model.** {
    *** Companion;
}
