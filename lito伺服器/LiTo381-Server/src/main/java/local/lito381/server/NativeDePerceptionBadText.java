// DECOMPILED (CFR 0.152) from reference/base-0.6.71.jar — TODO: replace with original source (T002)
/*
 * Decompiled with CFR 0.152.
 */
package local.lito381.server;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.StringTokenizer;
import local.lito381.server.NativeDePerceptionControls;
import local.lito381.server.NativeDePerceptionResource;

public class NativeDePerceptionBadText {
    private static void loadList(String filePath, ArrayList<String> targetList, boolean useTokenizer) {
        NativeDePerceptionResource.File file = new NativeDePerceptionResource.File(filePath);
        if (!file.exists()) {
            NativeDePerceptionControls.log().warn("\u627e\u4e0d\u5230\u904e\u6ffe\u6a94\u6848: " + filePath);
            return;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader((InputStream)new NativeDePerceptionResource.Stream(file), StandardCharsets.UTF_8));){
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                StringTokenizer st;
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                if ((line = line.trim()).isEmpty() || line.startsWith("#")) continue;
                String word = line;
                if (useTokenizer && (st = new StringTokenizer(line, ";")).hasMoreTokens()) {
                    word = st.nextToken().trim();
                }
                if (targetList.contains(word)) continue;
                targetList.add(word);
            }
            NativeDePerceptionControls.log().info("\u5df2\u8f09\u5165: " + file.getName());
        }
        catch (Exception e) {
            NativeDePerceptionControls.log().error("\u8b80\u53d6\u904e\u6ffe\u6a94\u6848\u767c\u751f\u932f\u8aa4: " + filePath, e);
        }
    }

    public static void load(String string, ArrayList arrayList, boolean bl) {
        NativeDePerceptionBadText.loadList(string, arrayList, bl);
    }
}
