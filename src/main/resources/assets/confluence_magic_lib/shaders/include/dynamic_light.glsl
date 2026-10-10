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
            // section AABB 给出距离下界；整组都无法提高亮度时跳过光源遍历。
            vec3 nearest = max(max(vec3(cell) * 16.0 - position, position - (vec3(cell) * 16.0 + 16.0)), vec3(0.0));
            float reach = (range.y - result) * (7.75 / 255.0);
            if (dot(nearest, nearest) > reach * reach + 0.0001) return result;
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

ivec2 cmlLightUV(ivec2 original, vec3 viewPosition) {
    if (CmlLightMask < 0 || original.x >= 240) return original;
    vec3 position = (CmlWorldFromView * vec4(viewPosition, 1.0)).xyz;
    ivec3 cell = ivec3(floor(position / 16.0));
    vec3 local = position - vec3(cell) * 16.0;
    ivec3 first = cell - ivec3(lessThanEqual(local, vec3(7.75)));
    ivec3 last = cell + ivec3(greaterThanEqual(local, vec3(8.25)));
    // 已有方块光照是下界，只查找能够让最终结果更亮的光源。
    float strength = cmlCellLight(cell, position, float(original.x) * (17.0 / 16.0));
    for (int x = first.x; x <= last.x; ++x)
    for (int y = first.y; y <= last.y; ++y)
    for (int z = first.z; z <= last.z; ++z)
    if (x != cell.x || y != cell.y || z != cell.z)
    strength = cmlCellLight(ivec3(x, y, z), position, strength);
    if (strength <= float(original.x) * (17.0 / 16.0)) return original;
    return ivec2(clamp(int(strength * (16.0 / 17.0)) + 8, original.x, 248), original.y);
}
