# Keep OpenAI request/response model field names for Gson serialization
-keepclassmembers class com.playerid.app.ui.ai.** {
    <fields>;
}

# Keep AI module APIs used by reflection or dynamic invocation
-keep class com.playerid.app.ui.ai.** { *; }
