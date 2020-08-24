package baseandroid.sl.plugin

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes


class SlAnalyticsSDKHookConfig {

    HashMap<String, HashMap<String, ArrayList<SlAnalyticsMethodCell>>> methodCells = new HashMap<>()

    void disableIMEI(String methodName) {
        def imei = new SlAnalyticsMethodCell('getIMEI', '(Landroid/content/Context;)Ljava/lang/String;', 'createGetIMEI')
        def deviceID = new SlAnalyticsMethodCell('getDeviceID', '(Landroid/content/Context;I)Ljava/lang/String;', 'createGetDeviceID')
        def imeiMethods = [imei, deviceID]
        def imeiMethodCells = new HashMap<String, ArrayList<SlAnalyticsMethodCell>>()
        imeiMethodCells.put("baseandroid/sl/sdk/analytics/util/SlDataUtils", imeiMethods)
        methodCells.put(methodName, imeiMethodCells)
    }

    void disableAndroidID(String methodName) {
        def androidID = new SlAnalyticsMethodCell('getAndroidID', '(Landroid/content/Context;)Ljava/lang/String;', 'createGetAndroidID')
        def androidIDMethods = [androidID]
        def androidIdMethodCells = new HashMap<String, ArrayList<SlAnalyticsMethodCell>>()
        androidIdMethodCells.put('baseandroid/sl/sdk/analytics/util/SlDataUtils', androidIDMethods)
        methodCells.put(methodName, androidIdMethodCells)
    }

    void disableLog(String methodName) {
        def info = new SlAnalyticsMethodCell('info', '(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V', "createSlLogInfo")
        def printStackTrace = new SlAnalyticsMethodCell('printStackTrace', '(Ljava/lang/Exception;)V', "createPrintStackTrack")
        def sALogMethods = [info, printStackTrace]
        def sALogMethodCells = new HashMap<String, ArrayList<SlAnalyticsMethodCell>>()
        sALogMethodCells.put('baseandroid/sl/sdk/analytics/SlLog', sALogMethods)
        methodCells.put(methodName, sALogMethodCells)
    }

    void disableJsInterface(String methodName) {
        def showUpWebView = new SlAnalyticsMethodCell("showUpWebView", '(Landroid/webkit/WebView;Lorg/json/JSONObject;ZZ)V', "createShowUpWebViewFour")
        def showUpX5WebView = new SlAnalyticsMethodCell("showUpX5WebView", '(Ljava/lang/Object;Lorg/json/JSONObject;ZZ)V', "createShowUpX5WebViewFour")
        def showUpX5WebView2 = new SlAnalyticsMethodCell("showUpX5WebView", '(Ljava/lang/Object;Z)V', "createShowUpX5WebViewTwo")
        def slDataAPIMethods = [showUpWebView, showUpX5WebView, showUpX5WebView2]
        def slDataAPIMethodCells = new HashMap<String, ArrayList<SlAnalyticsMethodCell>>()
        slDataAPIMethodCells.put('baseandroid/sl/sdk/analytics/SlDataAPI', slDataAPIMethods)
        methodCells.put(methodName, slDataAPIMethodCells)
    }

    void disableMacAddress(String methodName) {
        def macAddress = new SlAnalyticsMethodCell('getMacAddress', '(Landroid/content/Context;)Ljava/lang/String;', 'createGetMacAddress')
        def macMethods = [macAddress]
        def macMethodCells = new HashMap<String, ArrayList<SlAnalyticsMethodCell>>()
        macMethodCells.put("baseandroid/sl/sdk/analytics/util/SlDataUtils", macMethods)
        methodCells.put(methodName, macMethodCells)
    }

    void disableCarrier(String methodName) {
        def carrier = new SlAnalyticsMethodCell('getCarrier', '(Landroid/content/Context;)Ljava/lang/String;', 'createGetCarrier')
        def macMethods = [carrier]
        def macMethodCells = new HashMap<String, ArrayList<SlAnalyticsMethodCell>>()
        macMethodCells.put("baseandroid/sl/sdk/analytics/util/SlDataUtils", macMethods)
        methodCells.put(methodName, macMethodCells)
    }

    //todo 扩展

    void createGetIMEI(ClassVisitor classVisitor, SlAnalyticsMethodCell methodCell) {
        def mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, methodCell.name, methodCell.desc, null, null)
        mv.visitCode()
        mv.visitLdcInsn("")
        mv.visitInsn(Opcodes.ARETURN)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }

    void createGetAndroidID(ClassVisitor classVisitor, SlAnalyticsMethodCell methodCell) {
        def mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, methodCell.name, methodCell.desc, null, null)
        mv.visitCode()
        mv.visitLdcInsn("")
        mv.visitInsn(Opcodes.ARETURN)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }

    void createSlLogInfo(ClassVisitor classVisitor, SlAnalyticsMethodCell methodCell) {
        def mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, methodCell.name, methodCell.desc, null, null)
        mv.visitCode()
        mv.visitInsn(Opcodes.RETURN)
        mv.visitMaxs(0, 3)
        mv.visitEnd()
    }

    void createPrintStackTrack(ClassVisitor classVisitor, SlAnalyticsMethodCell methodCell) {
        def mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, methodCell.name, methodCell.desc, null, null)
        mv.visitCode()
        mv.visitInsn(Opcodes.RETURN)
        mv.visitMaxs(0, 1)
        mv.visitEnd()
    }

    void createShowUpWebViewFour(ClassVisitor classVisitor, SlAnalyticsMethodCell methodCell) {
        def mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC, methodCell.name, methodCell.desc, null, null)
        mv.visitCode()
        mv.visitInsn(Opcodes.RETURN)
        mv.visitMaxs(0, 5)
        mv.visitEnd()
    }

    void createShowUpX5WebViewFour(ClassVisitor classVisitor, SlAnalyticsMethodCell methodCell) {
        def mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC, methodCell.name, methodCell.desc, null, null)
        mv.visitCode()
        mv.visitInsn(Opcodes.RETURN)
        mv.visitMaxs(0, 5)
        mv.visitEnd()
    }

    void createShowUpX5WebViewTwo(ClassVisitor classVisitor, SlAnalyticsMethodCell methodCell) {
        def mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC, methodCell.name, methodCell.desc, null, null)
        mv.visitCode()
        mv.visitInsn(Opcodes.RETURN)
        mv.visitMaxs(0, 3)
        mv.visitEnd()
    }

    void createGetMacAddress(ClassVisitor classVisitor, SlAnalyticsMethodCell methodCell) {
        def mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, methodCell.name, methodCell.desc, null, null)
        mv.visitCode()
        mv.visitLdcInsn("")
        mv.visitInsn(Opcodes.ARETURN)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }

    void createGetCarrier(ClassVisitor classVisitor, SlAnalyticsMethodCell methodCell) {
        def mv = classVisitor.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, methodCell.name, methodCell.desc, null, null)
        mv.visitCode()
        mv.visitLdcInsn("")
        mv.visitInsn(Opcodes.ARETURN)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }

    void createGetDeviceID(ClassVisitor classVisitor, SlAnalyticsMethodCell methodCell) {
        def mv = classVisitor.visitMethod(Opcodes.ACC_PRIVATE + Opcodes.ACC_STATIC, methodCell.name, methodCell.desc, null, null)
        mv.visitCode()
        mv.visitLdcInsn("")
        mv.visitInsn(Opcodes.ARETURN)
        mv.visitMaxs(1, 1)
        mv.visitEnd()
    }

    //todo 扩展

}
