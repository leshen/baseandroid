package tools.shenle.slbaseandroid

/**
 * 在proguard规则中keep住这个注解, 并且允许混淆.然后在代码，方法中加上这个注解
 *
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
 */

@Retention(AnnotationRetention.BINARY)
@Target(*[AnnotationTarget.TYPE, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD])
annotation class Keep