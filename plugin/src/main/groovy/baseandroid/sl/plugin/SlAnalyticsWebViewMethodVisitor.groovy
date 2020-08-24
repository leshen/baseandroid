package baseandroid.sl.plugin


import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class SlAnalyticsWebViewMethodVisitor extends MethodVisitor implements Opcodes {

    private SlAnalyticsTransformHelper transformHelper
    private Class webView
    private Class x5WebView
    private boolean isPreviousX5WebView = false
    private static X5WebViewStatus x5WebViewStatus = X5WebViewStatus.NOT_INITIAL
    private static final def TARGET_NAME_DESC = ["loadUrl(Ljava/lang/String;)V", "loadUrl(Ljava/lang/String;Ljava/util/Map;)V",
                                                 "loadData(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                                                 "loadDataWithBaseURL(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",
                                                 "postUrl(Ljava/lang/String;[B)V"]
    private static final def VIEW_DESC = "Landroid/view/View;"
    private static final def OWNER_WHITE_SET = new HashSet(["android/webkit/WebView", "com/tencent/smtt/sdk/WebView"])
    private String className
    private String superName


    SlAnalyticsWebViewMethodVisitor(MethodVisitor mv, SlAnalyticsTransformHelper transformHelper, String className, String superName) {
        super(SlAnalyticsUtil.ASM_VERSION, mv)
        this.transformHelper = transformHelper
        this.className = className
        this.superName = superName
    }

    @Override
    void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (TARGET_NAME_DESC.contains(name + desc)) {
            if (!checkWebViewChild(className)) {
                if (isAssignableWebView(owner)) {
                    opcode = INVOKESTATIC
                    owner = SlAnalyticsHookConfig.SL_ANALYTICS_API
                    desc = reStructureDesc(desc)
                }
            }
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf)
    }

    /**
     * 判断是否是 WebView 的子类，避免 WebView 子类中调用 load* 方法，导致的递归调用
     *
     * @param className 当前被处理的类
     * @return true 是 WebView 的子类，false 非 WebView 子类
     */
    private boolean checkWebViewChild(String className) {
        if (superName == "com/tencent/smtt/sdk/WebViewClient") {
            return false
        }
        isAssignableWebView(className)
    }

    private boolean isAssignableWebView(String owner) {
        try {
            if (OWNER_WHITE_SET.contains(owner)) {
                return true
            }
            Class ownerClass = transformHelper.urlClassLoader.loadClass(owner.replace("/", "."))
            if (x5WebViewStatus == X5WebViewStatus.FOUND && isPreviousX5WebView) {
                if (checkX5WebView(ownerClass, owner)) {
                    return true
                }
            }
            return checkAndroidWebView(ownerClass, owner) || checkX5WebView(ownerClass, owner)
        } catch (Exception e) {
            e.printStackTrace()
        }
        return false
    }

    private boolean checkAndroidWebView(Class ownerClass, String owner) {
        if (webView == null) {
            webView = transformHelper.urlClassLoader.loadClass("android.webkit.WebView")
        }
        if (webView.isAssignableFrom(ownerClass)) {
            OWNER_WHITE_SET << owner
            isPreviousX5WebView = false
            return true
        }
    }

    private boolean checkX5WebView(Class ownerClass, String owner) {
        if (x5WebViewStatus == X5WebViewStatus.NOT_FOUND) {
            return false
        }
        if (x5WebView == null) {
            try {
                x5WebView = transformHelper.urlClassLoader.loadClass("com.tencent.smtt.sdk.WebView")
                x5WebViewStatus = X5WebViewStatus.FOUND
            } catch (ClassNotFoundException ignored) {
                x5WebViewStatus = X5WebViewStatus.NOT_FOUND
                return false
            }
        }
        if (x5WebView.isAssignableFrom(ownerClass)) {
            OWNER_WHITE_SET << owner
            return (isPreviousX5WebView = true)
        }
    }


    private static String reStructureDesc(String desc) {
        return desc.replaceFirst("\\(", "(" + VIEW_DESC)
    }

    private enum X5WebViewStatus {
        NOT_INITIAL, FOUND, NOT_FOUND
    }
}