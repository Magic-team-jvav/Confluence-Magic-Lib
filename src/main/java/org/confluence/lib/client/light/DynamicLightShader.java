package org.confluence.lib.client.light;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

// 原版地形逐像素取光，其他材质沿用顶点路径，保留模型变换、雾和颜色计算。
public final class DynamicLightShader {
    private static final Pattern MAIN = Pattern.compile("void\\s+main\\s*\\(\\s*(?:void)?\\s*\\)\\s*\\{");
    private static final Pattern UV = Pattern.compile("\\bUV2\\b");
    private static final String FUNCTIONS = readFunctions();

    private static final Set<String> TERRAIN = Set.of("rendertype_solid", "rendertype_cutout",
            "rendertype_cutout_mipped", "rendertype_translucent", "rendertype_tripwire");
    private static final Set<String> fragmentTerrain = new HashSet<>();
    private static final Pattern TERRAIN_COLOR = Pattern.compile("vertexColor\\s*=\\s*Color\\s*\\*\\s*minecraft_sample_lightmap\\(\\s*Sampler2\\s*,\\s*UV2\\s*\\)\\s*;");
    private static final Pattern VERTEX_COLOR = Pattern.compile("\\bvertexColor\\b");

    public static String patch(String source, boolean vertex, String name) {
        String key = name.startsWith("minecraft:") ? name.substring(10) : name;
        if (!TERRAIN.contains(key)) return vertex ? patch(source) : source;
        if (vertex) {
            fragmentTerrain.remove(key);
            var main = MAIN.matcher(source);
            // 自定义材质不符合原版接口时，沿用通用顶点光照路径。
            if (!TERRAIN_COLOR.matcher(source).find() || !source.contains("uniform vec3 ChunkOffset;") || !main.find())
                return patch(source);
            fragmentTerrain.add(key);
            String declarations = """
                    uniform vec3 CmlCameraOffset;
                    out vec3 cmlPosition;
                    out vec2 cmlLightmap;
                    out vec4 cmlBaseColor;
                    out vec4 cmlStaticLight;
                    """;
            String setup = """
                        cmlPosition = Position + (ChunkOffset + CmlCameraOffset);
                        cmlLightmap = vec2(UV2);
                        cmlBaseColor = Color;
                        cmlStaticLight = minecraft_sample_lightmap(Sampler2, UV2);
                    """;
            return source.substring(0, main.start()) + declarations + main.group() + "\n"
                    + setup + source.substring(main.end());
        }
        if (!fragmentTerrain.contains(key) || !source.contains("in vec4 vertexColor;"))
            return source;
        var main = MAIN.matcher(source);
        if (!main.find()) return source;
        String declarations = """
                in vec3 cmlPosition;
                in vec2 cmlLightmap;
                in vec4 cmlBaseColor;
                in vec4 cmlStaticLight;
                """;
        if (!source.contains("uniform sampler2D Sampler2;"))
            declarations += "uniform sampler2D Sampler2;\n";
        String setup = """
                    vec4 cmlVertexColor = vertexColor;
                    if (CmlLightMask >= 0 && cmlLightmap.x < 240.0) {
                        vec2 uv = cmlLightUVAt(cmlLightmap, cmlPosition);
                        if (uv.x > cmlLightmap.x) {
                            vec3 light = texture(Sampler2, clamp(uv / 256.0, vec2(0.03125), vec2(0.96875))).rgb;
                            cmlVertexColor.rgb += cmlBaseColor.rgb * max(light - cmlStaticLight.rgb, vec3(0.0));
                        }
                    }
                """;
        return source.substring(0, main.start()) + declarations + FUNCTIONS + "\n" + main.group() + "\n"
                + setup + VERTEX_COLOR.matcher(source.substring(main.end())).replaceAll("cmlVertexColor");
    }

    private DynamicLightShader() {}

    private static String patch(String source) {
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
