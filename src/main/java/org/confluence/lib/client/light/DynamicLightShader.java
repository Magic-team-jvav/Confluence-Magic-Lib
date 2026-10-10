package org.confluence.lib.client.light;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

// 修改取光坐标，保留资源包的模型变换、材质、雾和颜色计算。
public final class DynamicLightShader {
    private static final Pattern MAIN = Pattern.compile("void\\s+main\\s*\\(\\s*(?:void)?\\s*\\)\\s*\\{");
    private static final Pattern UV = Pattern.compile("\\bUV2\\b");
    private static final String FUNCTIONS = readFunctions();

    private DynamicLightShader() {}

    public static String patch(String source) {
        if (source.contains("CmlLightData") || !source.matches("(?s).*in\\s+ivec2\\s+UV2\\s*;.*")
                || !source.matches("(?s).*in\\s+vec3\\s+Position\\s*;.*")
                || !source.matches("(?s).*uniform\\s+mat4\\s+ModelViewMat\\s*;.*")) return source;
        var main = MAIN.matcher(source);
        if (!main.find()) return source;
        String body = source.substring(main.end());
        if (!UV.matcher(body).find()) return source;
        String position = source.matches("(?s).*uniform\\s+vec3\\s+ChunkOffset\\s*;.*") ? "Position + ChunkOffset" : "Position";
        return source.substring(0, main.start()) + FUNCTIONS + "\n" + main.group()
                + "\n    ivec2 cmlUV = cmlLightUV(UV2, (ModelViewMat * vec4(" + position + ", 1.0)).xyz);\n"
                + UV.matcher(body).replaceAll("cmlUV");
    }

    private static String readFunctions() {
        try (var stream = DynamicLightShader.class.getResourceAsStream("/assets/confluence_magic_lib/shaders/include/dynamic_light.glsl")) {
            if (stream == null) throw new IOException("Missing dynamic light shader");
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
