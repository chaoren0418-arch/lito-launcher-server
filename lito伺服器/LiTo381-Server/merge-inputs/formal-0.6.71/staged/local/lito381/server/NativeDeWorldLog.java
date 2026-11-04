// DECOMPILED from base-0.6.71.jar, TODO review
package local.lito381.server;

interface NativeDeWorldLog {
    public void emit(String var1, String var2, Object[] var3);

    default public void warn(String string) {
        this.emit("warn", string, new Object[0]);
    }

    default public void warn(String string, Object object) {
        this.emit("warn", string, new Object[]{object});
    }

    default public void warn(String string, Object object, Object object2) {
        this.emit("warn", string, new Object[]{object, object2});
    }

    default public void warn(String string, Object object, Object object2, Object object3) {
        this.emit("warn", string, new Object[]{object, object2, object3});
    }

    default public void warn(String string, Object object, Object object2, Object object3, Object object4) {
        this.emit("warn", string, new Object[]{object, object2, object3, object4});
    }

    default public void warn(String string, Object object, Object object2, Object object3, Object object4, Object object5) {
        this.emit("warn", string, new Object[]{object, object2, object3, object4, object5});
    }

    default public void warn(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6) {
        this.emit("warn", string, new Object[]{object, object2, object3, object4, object5, object6});
    }

    default public void warn(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7) {
        this.emit("warn", string, new Object[]{object, object2, object3, object4, object5, object6, object7});
    }

    default public void warn(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7, Object object8) {
        this.emit("warn", string, new Object[]{object, object2, object3, object4, object5, object6, object7, object8});
    }

    default public void warn(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7, Object object8, Object object9) {
        this.emit("warn", string, new Object[]{object, object2, object3, object4, object5, object6, object7, object8, object9});
    }

    default public void warn(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7, Object object8, Object object9, Object object10) {
        this.emit("warn", string, new Object[]{object, object2, object3, object4, object5, object6, object7, object8, object9, object10});
    }

    default public void warn(String string, Throwable throwable) {
        this.emit("warn", string, new Object[]{throwable});
    }

    default public void warn(String string, Object ... objectArray) {
        this.emit("warn", string, objectArray);
    }

    default public void error(String string) {
        this.emit("error", string, new Object[0]);
    }

    default public void error(String string, Object object) {
        this.emit("error", string, new Object[]{object});
    }

    default public void error(String string, Object object, Object object2) {
        this.emit("error", string, new Object[]{object, object2});
    }

    default public void error(String string, Object object, Object object2, Object object3) {
        this.emit("error", string, new Object[]{object, object2, object3});
    }

    default public void error(String string, Object object, Object object2, Object object3, Object object4) {
        this.emit("error", string, new Object[]{object, object2, object3, object4});
    }

    default public void error(String string, Object object, Object object2, Object object3, Object object4, Object object5) {
        this.emit("error", string, new Object[]{object, object2, object3, object4, object5});
    }

    default public void error(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6) {
        this.emit("error", string, new Object[]{object, object2, object3, object4, object5, object6});
    }

    default public void error(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7) {
        this.emit("error", string, new Object[]{object, object2, object3, object4, object5, object6, object7});
    }

    default public void error(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7, Object object8) {
        this.emit("error", string, new Object[]{object, object2, object3, object4, object5, object6, object7, object8});
    }

    default public void error(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7, Object object8, Object object9) {
        this.emit("error", string, new Object[]{object, object2, object3, object4, object5, object6, object7, object8, object9});
    }

    default public void error(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7, Object object8, Object object9, Object object10) {
        this.emit("error", string, new Object[]{object, object2, object3, object4, object5, object6, object7, object8, object9, object10});
    }

    default public void error(String string, Throwable throwable) {
        this.emit("error", string, new Object[]{throwable});
    }

    default public void error(String string, Object ... objectArray) {
        this.emit("error", string, objectArray);
    }

    default public void info(String string) {
        this.emit("info", string, new Object[0]);
    }

    default public void info(String string, Object object) {
        this.emit("info", string, new Object[]{object});
    }

    default public void info(String string, Object object, Object object2) {
        this.emit("info", string, new Object[]{object, object2});
    }

    default public void info(String string, Object object, Object object2, Object object3) {
        this.emit("info", string, new Object[]{object, object2, object3});
    }

    default public void info(String string, Object object, Object object2, Object object3, Object object4) {
        this.emit("info", string, new Object[]{object, object2, object3, object4});
    }

    default public void info(String string, Object object, Object object2, Object object3, Object object4, Object object5) {
        this.emit("info", string, new Object[]{object, object2, object3, object4, object5});
    }

    default public void info(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6) {
        this.emit("info", string, new Object[]{object, object2, object3, object4, object5, object6});
    }

    default public void info(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7) {
        this.emit("info", string, new Object[]{object, object2, object3, object4, object5, object6, object7});
    }

    default public void info(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7, Object object8) {
        this.emit("info", string, new Object[]{object, object2, object3, object4, object5, object6, object7, object8});
    }

    default public void info(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7, Object object8, Object object9) {
        this.emit("info", string, new Object[]{object, object2, object3, object4, object5, object6, object7, object8, object9});
    }

    default public void info(String string, Object object, Object object2, Object object3, Object object4, Object object5, Object object6, Object object7, Object object8, Object object9, Object object10) {
        this.emit("info", string, new Object[]{object, object2, object3, object4, object5, object6, object7, object8, object9, object10});
    }

    default public void info(String string, Throwable throwable) {
        this.emit("info", string, new Object[]{throwable});
    }

    default public void info(String string, Object ... objectArray) {
        this.emit("info", string, objectArray);
    }
    // 主控 #74／#153 核准偏離：補足正式 A02 呼叫的介面方法。
    default void debug(String message, Object a, Object b) {
        emit("debug", message, new Object[]{a, b});
    }
}

