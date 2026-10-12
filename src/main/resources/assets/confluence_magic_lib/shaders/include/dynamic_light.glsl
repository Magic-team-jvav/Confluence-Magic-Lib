uniform samplerBuffer CmlLightData;
uniform int CmlLightMask;
uniform mat4 CmlWorldFromView;

uint cmlHash(ivec3 cell) {
    return uint(cell.x) * 73856093u ^ uint(cell.y) * 19349663u ^ uint(cell.z) * 83492791u;
}

float cmlCellLight(ivec3 cell, vec3 position, float result) {
    int slot = int(cmlHash(cell) & uint(CmlLightMask));
    for (int probe = 0; probe <= CmlLightMask; ++probe) {
        vec4 header = texelFetch(CmlLightData, slot * 2);
        vec4 range = texelFetch(CmlLightData, slot * 2 + 1);
        int count = int(range.x);
        if (count == 0) return result;
        if (all(equal(ivec3(header.xyz), cell))) {
            if (range.y <= result) return result;
            int start = int(header.w);
            for (int i = 0; i < count; ++i) {
                vec4 light = texelFetch(CmlLightData, start + i);
                if (light.w <= result) return result;
                vec3 delta = position - light.xyz;
                float distanceSquared = dot(delta, delta);
                float remainingReach = (light.w - result) * (7.75 / 255.0);
                if (distanceSquared < 60.0625 && distanceSquared <= remainingReach * remainingReach + 0.0001) {
                    result = max(result, light.w - sqrt(distanceSquared) * (255.0 / 7.75));
                    if (result >= 255.0) return 255.0;
                }
            }
            return result;
        }
        slot = (slot + 1) & CmlLightMask;
    }
    return result;
}

vec2 cmlLightUVAt(vec2 original, vec3 position) {
    if (CmlLightMask < 0 || original.x >= 240) return original;
    ivec3 cell = ivec3(floor(position / 16.0));
    // 已有方块光照是下界，只查找能够让最终结果更亮的光源。
    float strength = cmlCellLight(cell, position, float(original.x) * (17.0 / 16.0));
    if (strength <= float(original.x) * (17.0 / 16.0)) return original;
    return vec2(clamp(strength * (16.0 / 17.0) + 8.0, original.x, 248.0), original.y);
}
// 保留实体等顶点光照路径；地形直接传入统一的相机相对坐标。
ivec2 cmlLightUV(ivec2 original, vec3 viewPosition) {
    return ivec2(cmlLightUVAt(vec2(original), (CmlWorldFromView * vec4(viewPosition, 1.0)).xyz));
}
