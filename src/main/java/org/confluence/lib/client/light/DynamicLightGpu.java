package org.confluence.lib.client.light;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.client.DynamicLightDispatcher.LightSource;
import org.confluence.lib.util.LibUtils;
import org.joml.Matrix4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;

public final class DynamicLightGpu {
    // 原版默认 sampler 使用 0～11；不占用光照贴图使用的纹理单元。
    private static final int TEXTURE_UNIT = 12;
    private static final DynamicLightGrid GRID = new DynamicLightGrid();
    private static final Matrix4f WORLD_FROM_VIEW = new Matrix4f();
    private static final Matrix4f INVERSE_VIEW = new Matrix4f();
    private static FloatBuffer matrixData;
    private static final Matrix4f LAST_VIEW = new Matrix4f();
    private static Vec3 matrixCamera;
    private static long matrixVersion;
    private static int uploadedAnchorX;
    private static int uploadedAnchorY;
    private static int uploadedAnchorZ;
    private static FloatBuffer staging;
    private static int buffer;
    private static int texture;
    private static int mask = -1;
    private static int maxTexels;
    private static Boolean supported;
    private static volatile boolean terrainGpu;
    private static boolean worldPass;
    private static ClientLevel level;

    private DynamicLightGpu() {}

    public static boolean terrainGpu() {return terrainGpu;}

    public static boolean prepare() {
        RenderSystem.assertOnRenderThread();
        if (supported == null) {
            supported = GL.getCapabilities().OpenGL31
                    && GL11.glGetInteger(GL20.GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS) > 0
                    && GL11.glGetInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS) > TEXTURE_UNIT
                    && !LibUtils.isModLoaded("sodium") && !LibUtils.isModLoaded("embeddium")
                    && !LibUtils.isModLoaded("iris") && !LibUtils.isModLoaded("oculus");
            if (supported)
                maxTexels = Math.min(GL11.glGetInteger(GL31.GL_MAX_TEXTURE_BUFFER_SIZE), 1 << 23);
            ConfluenceMagicLib.LOGGER.info("Dynamic light backend: {}", supported ? "GPU spatial grid" : "CPU compatibility");
        }
        terrainGpu = supported;
        return terrainGpu;
    }

    public static void begin(Long2ObjectMap<LightSource[]> sources, Vec3 camera, Matrix4f view) {
        RenderSystem.assertOnRenderThread();
        level = Minecraft.getInstance().level;
        worldPass = true;
        if (!terrainGpu) return;
        int distance = Minecraft.getInstance().options.getEffectiveRenderDistance();
        int x = SectionPos.blockToSectionCoord(camera.x), y = SectionPos.blockToSectionCoord(camera.y), z = SectionPos.blockToSectionCoord(camera.z);
        boolean needsCollect = !GRID.canReuse(sources, camera, distance * 16.0 + 16.0);
        boolean needsUpload = needsCollect || x != uploadedAnchorX || y != uploadedAnchorY || z != uploadedAnchorZ;
        if (needsCollect) {
            GRID.collect(sources, camera, distance * 16.0 + 16.0);
            if (GRID.texels() > maxTexels) {
                supported = false;
                terrainGpu = false;
                mask = -1;
                ConfluenceMagicLib.LOGGER.warn("Dynamic light buffer exceeds GPU capacity; using CPU compatibility backend");
                return;
            }
            mask = GRID.mask();
        }
        if (mask < 0) return;
        if (matrixData == null || !camera.equals(matrixCamera) || !view.equals(LAST_VIEW)) {
            WORLD_FROM_VIEW.translation((float) (camera.x - x * 16.0), (float) (camera.y - y * 16.0), (float) (camera.z - z * 16.0))
                    .mul(INVERSE_VIEW.set(view).invert());
            if (matrixData == null) matrixData = MemoryUtil.memAllocFloat(16);
            WORLD_FROM_VIEW.get(matrixData);
            LAST_VIEW.set(view);
            matrixCamera = camera;
            matrixVersion++;
        }
        if (!needsUpload) return;
        int required = GRID.texels() * 4;
        if (staging == null || staging.capacity() < required) {
            if (staging != null) MemoryUtil.memFree(staging);
            staging = MemoryUtil.memAllocFloat(Integer.highestOneBit(required - 1) << 1);
        }
        GRID.write(staging, x, y, z);
        uploadedAnchorX = x;
        uploadedAnchorY = y;
        uploadedAnchorZ = z;
        if (buffer == 0) buffer = GL15.glGenBuffers();
        int previousBuffer = GL11.glGetInteger(GL31.GL_TEXTURE_BUFFER);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, buffer);
        // 替换存储，让驱动保留仍被上一帧使用的数据，避免显式等待 GPU。
        GL15.glBufferData(GL31.GL_TEXTURE_BUFFER, staging, GL15.GL_STREAM_DRAW);
        GL15.glBindBuffer(GL31.GL_TEXTURE_BUFFER, previousBuffer);
        if (texture == 0) {
            texture = GL11.glGenTextures();
            int active = GlStateManager._getActiveTexture();
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_UNIT);
            int previousTexture = GL11.glGetInteger(GL31.GL_TEXTURE_BINDING_BUFFER);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, texture);
            GL31.glTexBuffer(GL31.GL_TEXTURE_BUFFER, GL30.GL_RGBA32F, buffer);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, previousTexture);
            GL13.glActiveTexture(active);
        }
    }

    public static void end() {
        worldPass = false;
    }

    public static void clearWorld() {
        level = null;
        matrixCamera = null;
        GRID.clear();
        mask = -1;
        worldPass = false;
    }

    public static void close() {
        RenderSystem.assertOnRenderThread();
        clearWorld();
        if (texture != 0) GL11.glDeleteTextures(texture);
        if (buffer != 0) GL15.glDeleteBuffers(buffer);
        if (staging != null) MemoryUtil.memFree(staging);
        if (matrixData != null) MemoryUtil.memFree(matrixData);
        matrixData = null;
        texture = buffer = 0;
        staging = null;
    }

    // 每个 ShaderInstance 持有自己的位置与纹理绑定，资源重载后不复用旧 program ID。
    public static final class Binding {
        private final int data;
        private final int maskLocation;
        private final int matrix;
        private int previousTexture;
        private boolean bound;
        private boolean samplerInitialized;
        private int appliedMask = Integer.MIN_VALUE;
        private long appliedMatrixVersion = -1;

        public Binding(ShaderInstance shader) {
            data = GL20.glGetUniformLocation(shader.getId(), "CmlLightData");
            maskLocation = GL20.glGetUniformLocation(shader.getId(), "CmlLightMask");
            matrix = GL20.glGetUniformLocation(shader.getId(), "CmlWorldFromView");
        }

        public void apply() {
            if (maskLocation < 0) return;
            clear();
            boolean enabled = terrainGpu && worldPass && level == Minecraft.getInstance().level && level != null;
            int nextMask = enabled ? mask : -1;
            if (appliedMask != nextMask) {
                GL20.glUniform1i(maskLocation, nextMask);
                appliedMask = nextMask;
            }
            if (!samplerInitialized) {
                GL20.glUniform1i(data, TEXTURE_UNIT);
                samplerInitialized = true;
            }
            if (!enabled || mask < 0) return;
            if (appliedMatrixVersion != matrixVersion) {
                GL20.glUniformMatrix4fv(matrix, false, matrixData);
                appliedMatrixVersion = matrixVersion;
            }
            int active = GlStateManager._getActiveTexture();
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_UNIT);
            previousTexture = GL11.glGetInteger(GL31.GL_TEXTURE_BINDING_BUFFER);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, texture);
            GL13.glActiveTexture(active);
            bound = true;
        }

        public void clear() {
            if (!bound) return;
            int active = GlStateManager._getActiveTexture();
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + TEXTURE_UNIT);
            GL11.glBindTexture(GL31.GL_TEXTURE_BUFFER, previousTexture);
            GL13.glActiveTexture(active);
            bound = false;
        }
    }
}
