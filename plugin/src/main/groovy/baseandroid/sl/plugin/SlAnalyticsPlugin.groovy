package baseandroid.sl.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.reflect.Instantiator
import org.gradle.invocation.DefaultGradle

class SlAnalyticsPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        Instantiator ins = ((DefaultGradle) project.getGradle()).getServices().get(Instantiator)
        def args = [ins] as Object[]
        SlAnalyticsExtension extension = project.extensions.create("slAnalytics", SlAnalyticsExtension, args)

        boolean disableSlAnalyticsPlugin = false
        boolean disableSlAnalyticsMultiThreadBuild = false
        boolean disableSlAnalyticsIncrementalBuild = false
        boolean isHookOnMethodEnter = false
        Properties properties = new Properties()
        if (project.rootProject.file('gradle.properties').exists()) {
            properties.load(project.rootProject.file('gradle.properties').newDataInputStream())
            disableSlAnalyticsPlugin = Boolean.parseBoolean(properties.getProperty("slAnalytics.disablePlugin", "false")) ||
                    Boolean.parseBoolean(properties.getProperty("disableSlAnalyticsPlugin", "false"))
            disableSlAnalyticsMultiThreadBuild = Boolean.parseBoolean(properties.getProperty("slAnalytics.disableMultiThreadBuild", "false"))
            disableSlAnalyticsIncrementalBuild = Boolean.parseBoolean(properties.getProperty("slAnalytics.disableIncrementalBuild", "false"))
            isHookOnMethodEnter = Boolean.parseBoolean(properties.getProperty("slAnalytics.isHookOnMethodEnter", "false"))
        }
        if (!disableSlAnalyticsPlugin) {
            AppExtension appExtension = project.extensions.findByType(AppExtension.class)
            SlAnalyticsTransformHelper transformHelper = new SlAnalyticsTransformHelper(extension, appExtension)
            transformHelper.disableSlAnalyticsIncremental = disableSlAnalyticsIncrementalBuild
            transformHelper.disableSlAnalyticsMultiThread = disableSlAnalyticsMultiThreadBuild
            transformHelper.isHookOnMethodEnter = isHookOnMethodEnter
            appExtension.registerTransform(new SlAnalyticsTransform(transformHelper))
        } else {
            Logger.error("------------您已关闭了全埋点插件--------------")
        }
    }

}