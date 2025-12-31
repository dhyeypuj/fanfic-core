# Add project specific ProGuard rules here.
# Keep Room entities
-keep class com.dhyey.fanfic.storage.entity.** { *; }

# Keep data classes
-keep class com.dhyey.fanfic.model.** { *; }

# Jsoup
-keeppackagenames org.jsoup.nodes
