package baseandroid.sl.plugin

class ClassNameAnalytics {

    public String className
    boolean isShouldModify = false
    boolean isSlDataAPI = false
    boolean isSlDataUtils = false
    boolean isSlLog = false
    def methodCells = new ArrayList<SlAnalyticsMethodCell>()

    ClassNameAnalytics(String className) {
        this.className = className
        isSlDataAPI = (className == 'baseandroid.sl.sdk.analytics.SlDataAPI')
        isSlDataUtils = (className == 'baseandroid.sl.sdk.analytics.util.SlDataUtils')
        isSlLog = (className == 'baseandroid.sl.sdk.analytics.SlLog')
    }

    boolean isSDKFile() {
        return isSlLog || isSlDataAPI || isSlDataUtils
    }

    boolean isLeanback() {
        return className.startsWith("android.support.v17.leanback") || className.startsWith("androidx.leanback")
    }

    boolean isAndroidGenerated() {
        return className.contains('R$') ||
                className.contains('R2$') ||
                className.contains('R.class') ||
                className.contains('R2.class') ||
                className.contains('BuildConfig.class')
    }

}