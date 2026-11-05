package local.lito381.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 編譯後由 tools/as-record 還原為 JVM record；元件順序即 Record 屬性順序。 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface AsRecord {
    String[] components();

    String[] finalFields() default {};
}
