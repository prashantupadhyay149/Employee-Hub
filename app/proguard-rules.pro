# Proguard rules for EmployeeManagementApp
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* *;
}
-dontwarn org.osmdroid.**
