-keep,allowobfuscation @interface tools.shenle.slbaseandroid.Keep
-keep @tools.shenle.slbaseandroid.Keep class * {
*;
}

-keepclasseswithmembers class * {
@tools.shenle.slbaseandroid.Keep <fields>;
}

-keepclasseswithmembers class * {
@tools.shenle.slbaseandroid.Keep <init>(...);
}

-keepclasseswithmembers class * {
@tools.shenle.slbaseandroid.Keep <methods>;
}