package local.lito381.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 由建置轉換器補回 JVM final 欄位旗標。 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface AsmFinal {
    String[] value();
}
