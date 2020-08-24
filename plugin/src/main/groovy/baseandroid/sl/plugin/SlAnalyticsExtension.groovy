package baseandroid.sl.plugin

import org.gradle.api.Action
import org.gradle.internal.reflect.Instantiator

class SlAnalyticsExtension {
    public boolean debug = false
    public boolean disableJar = false
    public boolean useInclude = false
    public boolean lambdaEnabled = true
    public boolean autoHandleWebView = true

    public ArrayList<String> exclude = []
    public ArrayList<String> include = []

    public SlAnalyticsSDKExtension sdk

    SlAnalyticsExtension(Instantiator ins) {
        sdk = ins.newInstance(SlAnalyticsSDKExtension)
    }

    void sdk(Action<? super SlAnalyticsSDKExtension> action) {
        action.execute(sdk)
    }

    @Override
    String toString() {
        StringBuilder excludeBuilder = new StringBuilder()
        int length = exclude.size()
        for (int i = 0; i < length; i++) {
            excludeBuilder.append("'").append(exclude.get(i)).append("'")
            if (i != length - 1) {
                excludeBuilder.append(",")
            }
        }

        StringBuilder includeBuilder = new StringBuilder()
        length = include.size()
        for (int i = 0; i < length; i++) {
            includeBuilder.append("'").append(include.get(i)).append("'")
            if (i != length - 1) {
                includeBuilder.append(",")
            }
        }
        return "\tdebug=" + debug + "\n" +
                "\tdisableJar=" + disableJar + "\n" +
                "\tuseInclude=" + useInclude + "\n" +
                "\tautoHandleWebView=" + autoHandleWebView + "\n" +
                "\tlambdaEnabled=" + lambdaEnabled + "\n" +
                "\texclude=[" + excludeBuilder.toString() + "]" + "\n" +
                "\tinclude=[" + includeBuilder.toString() + "]" + "\n" +
                "\tsdk {\n" + sdk + "\n" +
                "\t}"
    }
}