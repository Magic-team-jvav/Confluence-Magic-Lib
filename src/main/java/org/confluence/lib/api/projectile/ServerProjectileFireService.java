package org.confluence.lib.api.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 玩家战斗弹幕的统一服务端发射入口。
 *
 * <p>固定执行顺序为：读取服务端手持物、检查触发与冷却、解析战斗快照、检查资源、
 * 创建弹幕、提交资源并加入世界。具体武器只负责提供数值、成本和弹幕布局。</p>
 */
public final class ServerProjectileFireService {
    private static final Map<ServerPlayer, RequestStamps> REQUEST_STAMPS = new WeakHashMap<>();

    private ServerProjectileFireService() {}

    /**
     * 从当前手持物解析动作并执行。
     */
    public static ProjectileFireResult fire(
            ServerPlayer player,
            InteractionHand hand,
            ProjectileFireTrigger trigger
    ) {
        ProjectileFireResult availability = checkAvailability(player, hand, trigger);
        if (availability != null) return availability;

        ProjectileFireContext context;
        try {
            context = ProjectileFireContext.capture(player, hand, trigger);
        } catch (RuntimeException exception) {
            logInvalid("Failed to capture projectile fire context", exception);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }
        if (!(player.getItemInHand(hand).getItem() instanceof ProjectileWeaponAction provider)) {
            return ProjectileFireResult.NO_ACTION;
        }

        ProjectileFireAction action;
        try {
            action = provider.createProjectileFireAction(context);
        } catch (RuntimeException exception) {
            logInvalid("Projectile weapon action provider failed", exception);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }
        if (action == null) return ProjectileFireResult.NO_ACTION;
        return execute(context, action);
    }

    /**
     * 生成一次已经成功提交动作的延迟后续批次。
     *
     * <p>该入口只供逻辑服务端的可信武器计时器调用，不接受网络动作选择，也不会再次扣资源、
     * 解析属性、添加冷却或摆手。后续批次必须继续使用首批冻结的战斗快照，并且玩家仍需持有
     * 同种武器。整批实体会沿用正常发射边界的所有者、世界、快照、有限数值与原子生成校验。</p>
     */
    public static ProjectileFireResult continueBurst(
            ServerPlayer player,
            InteractionHand hand,
            ProjectileCombatSnapshot snapshot,
            List<ProjectileLaunch> launches
    ) {
        return continueBurst(player, hand, snapshot, launches, ProjectileCost.none());
    }

    /**
     * 生成延迟后续批次，并在该批次入世前原子提交自己的资源成本。
     *
     * <p>适用于连发武器的逐发弹药和耐久：首发快照保持不变，但玩家取消连发时尚未执行的批次
     * 不会提前丢失资源。成本只允许读取服务端当前手持状态，不能重新解析伤害属性。</p>
     */
    public static ProjectileFireResult continueBurst(
            ServerPlayer player,
            InteractionHand hand,
            ProjectileCombatSnapshot snapshot,
            List<ProjectileLaunch> launches,
            ProjectileCost cost
    ) {
        if (player == null || hand == null || snapshot == null || launches == null || cost == null) {
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }
        if (!player.isAlive() || player.isRemoved() || player.isSpectator()
                || !(player.level() instanceof ServerLevel level)) {
            return ProjectileFireResult.PLAYER_UNAVAILABLE;
        }
        if (!level.getServer().isSameThread()) {
            logInvalid("Projectile burst continuation called outside the server thread", null);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }
        ItemStack currentWeapon = player.getItemInHand(hand);
        if (currentWeapon.isEmpty() || !ItemStack.isSameItem(currentWeapon, snapshot.weapon())) {
            return ProjectileFireResult.WEAPON_CHANGED;
        }

        final ProjectileFireContext context;
        try {
            context = ProjectileFireContext.capture(
                    player, hand, ProjectileFireTrigger.ATTACK_PRESSED);
        } catch (RuntimeException exception) {
            logInvalid("Failed to capture projectile burst continuation context", exception);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }
        return spawnFrozenBatchWithCost(
                player,
                level,
                snapshot,
                launches,
                cost,
                context,
                "Projectile burst continuation");
    }

    /**
     * 生成由已存在弹幕在命中、分裂或到期阶段产生的派生批次。
     *
     * <p>该入口只供逻辑服务端的可信弹幕回调使用，不接受客户端动作，也不会重新扣资源、解析
     * 属性或要求玩家仍持有原武器。调用方必须传入父弹幕冻结快照或由其
     * {@link ProjectileCombatSnapshot#derive(float, float, float)} 得到的派生快照。玩家死亡、离线、
     * 旁观或跨世界后会拒绝生成；整批仍经过所有者、世界、快照、数量和有限数值校验。</p>
     */
    public static ProjectileFireResult spawnDerived(
            ServerPlayer player,
            ProjectileCombatSnapshot snapshot,
            List<ProjectileLaunch> launches
    ) {
        if (player == null || snapshot == null || launches == null) {
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }
        if (!player.isAlive() || player.isRemoved() || player.isSpectator()
                || !(player.level() instanceof ServerLevel level)) {
            return ProjectileFireResult.PLAYER_UNAVAILABLE;
        }
        if (!level.getServer().isSameThread()) {
            logInvalid("Derived projectile batch called outside the server thread", null);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }

        return spawnFrozenBatch(player, level, snapshot, launches, "Derived projectile batch");
    }

    /**
     * 对无需再次扣费的冻结快照批次执行统一的预检和原子世界提交。
     */
    private static ProjectileFireResult spawnFrozenBatch(
            ServerPlayer player,
            ServerLevel level,
            ProjectileCombatSnapshot snapshot,
            List<ProjectileLaunch> launches,
            String diagnosticName
    ) {

        final List<ProjectileLaunch> copy;
        try {
            if (launches.isEmpty()) return ProjectileFireResult.NO_VALID_PROJECTILE;
            copy = List.copyOf(launches);
            prepareLaunches(level, player, snapshot, copy);
        } catch (RuntimeException exception) {
            discardAll(launches);
            logInvalid(diagnosticName + " was invalid", exception);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }

        try {
            for (ProjectileLaunch launch : copy) {
                if (!level.addFreshEntity(launch.projectile())) {
                    throw new IllegalStateException("Server level rejected a frozen projectile batch");
                }
            }
        } catch (RuntimeException exception) {
            discardAll(copy);
            logInvalid(diagnosticName + " spawn failed and was rolled back", exception);
            return ProjectileFireResult.SPAWN_ROLLED_BACK;
        }
        return ProjectileFireResult.SUCCESS;
    }

    /**
     * 对延迟批次执行逐批成本提交；失败时补偿资源并丢弃全部预构建实体。
     */
    private static ProjectileFireResult spawnFrozenBatchWithCost(
            ServerPlayer player,
            ServerLevel level,
            ProjectileCombatSnapshot snapshot,
            List<ProjectileLaunch> launches,
            ProjectileCost cost,
            ProjectileFireContext context,
            String diagnosticName
    ) {
        final List<ProjectileLaunch> copy;
        try {
            if (launches.isEmpty()) return ProjectileFireResult.NO_VALID_PROJECTILE;
            copy = List.copyOf(launches);
            prepareLaunches(level, player, snapshot, copy);
        } catch (RuntimeException exception) {
            discardAll(launches);
            logInvalid(diagnosticName + " was invalid", exception);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }

        final PreparedProjectileCost preparedCost;
        try {
            Optional<PreparedProjectileCost> prepared = cost.prepare(context);
            if (prepared == null) {
                discardAll(copy);
                logInvalid(diagnosticName + " cost returned a null preparation", null);
                return ProjectileFireResult.INVALID_ACTION_RESULT;
            }
            if (prepared.isEmpty()) {
                discardAll(copy);
                return ProjectileFireResult.NO_RESOURCE;
            }
            preparedCost = prepared.orElseThrow();
        } catch (RuntimeException exception) {
            discardAll(copy);
            logInvalid(diagnosticName + " cost preparation failed", exception);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }

        if (!context.matchesCurrentWeapon()) {
            discardAll(copy);
            return rollbackOr(preparedCost, ProjectileFireResult.WEAPON_CHANGED);
        }
        try {
            preparedCost.commit();
        } catch (RuntimeException exception) {
            discardAll(copy);
            logInvalid(diagnosticName + " cost commit failed", exception);
            return rollbackOr(preparedCost, ProjectileFireResult.COST_COMMIT_FAILED);
        }

        try {
            for (ProjectileLaunch launch : copy) {
                if (!level.addFreshEntity(launch.projectile())) {
                    throw new IllegalStateException("Server level rejected a paid frozen projectile batch");
                }
            }
        } catch (RuntimeException exception) {
            discardAll(copy);
            logInvalid(diagnosticName + " spawn failed and was rolled back", exception);
            return rollbackOr(preparedCost, ProjectileFireResult.SPAWN_ROLLED_BACK);
        }
        return ProjectileFireResult.SUCCESS;
    }

    /**
     * 执行由可信服务端回调直接提供的动作。
     *
     * <p>该入口供原版弓/连弩释放点和自动化测试接入；动作仍只能在服务端代码中构建，网络包不得
     * 直接选择或反序列化动作。</p>
     */
    public static ProjectileFireResult fire(
            ServerPlayer player,
            InteractionHand hand,
            ProjectileFireTrigger trigger,
            ProjectileFireAction action
    ) {
        ProjectileFireResult availability = checkAvailability(player, hand, trigger);
        if (availability != null) return availability;
        if (action == null) return ProjectileFireResult.INVALID_ACTION_RESULT;

        ProjectileFireContext context;
        try {
            context = ProjectileFireContext.capture(player, hand, trigger);
        } catch (RuntimeException exception) {
            logInvalid("Failed to capture projectile fire context", exception);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }
        return execute(context, action);
    }

    private static @Nullable ProjectileFireResult checkAvailability(
            @Nullable ServerPlayer player,
            @Nullable InteractionHand hand,
            @Nullable ProjectileFireTrigger trigger
    ) {
        if (player == null || hand == null || trigger == null) {
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }
        if (!player.isAlive() || player.isRemoved() || player.isSpectator() ||
                !(player.level() instanceof ServerLevel level)) {
            return ProjectileFireResult.PLAYER_UNAVAILABLE;
        }
        if (!level.getServer().isSameThread()) {
            logInvalid("Projectile fire service called outside the server thread", null);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }
        return player.getItemInHand(hand).isEmpty() ? ProjectileFireResult.NO_ACTION : null;
    }

    private static ProjectileFireResult execute(
            ProjectileFireContext context,
            ProjectileFireAction action
    ) {
        if (!action.supports(context.trigger())) return ProjectileFireResult.UNSUPPORTED_TRIGGER;
        if (isDuplicate(context)) return ProjectileFireResult.DUPLICATE_REQUEST;

        ItemStack currentWeapon = context.player().getItemInHand(context.hand());
        if (context.player().getCooldowns().isOnCooldown(currentWeapon.getItem())) {
            return ProjectileFireResult.COOLDOWN;
        }
        try {
            if (!action.validate(context)) return ProjectileFireResult.VALIDATION_REJECTED;
        } catch (RuntimeException exception) {
            logInvalid("Projectile fire validator failed", exception);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }

        if (!context.matchesCurrentWeapon()) return ProjectileFireResult.WEAPON_CHANGED;

        ProjectileCombatSnapshot snapshot;
        try {
            snapshot = ProjectileAttributeResolver.resolve(
                    context.player(),
                    context.weapon(),
                    action.damageChannel(),
                    action.baseDamage(),
                    action.baseVelocity(),
                    action.baseKnockback(),
                    action.inherentCritical(),
                    action.criticalChanceBonus()
            );
        } catch (RuntimeException exception) {
            logInvalid("Failed to resolve projectile combat snapshot", exception);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }

        PreparedProjectileCost preparedCost;
        try {
            Optional<PreparedProjectileCost> prepared = action.cost().prepare(context);
            if (prepared == null) {
                logInvalid("Projectile cost returned a null preparation", null);
                return ProjectileFireResult.INVALID_ACTION_RESULT;
            }
            if (prepared.isEmpty()) return ProjectileFireResult.NO_RESOURCE;
            preparedCost = prepared.orElseThrow();
        } catch (RuntimeException exception) {
            logInvalid("Projectile cost preparation failed", exception);
            return ProjectileFireResult.INVALID_ACTION_RESULT;
        }

        List<ProjectileLaunch> launches;
        try {
            List<ProjectileLaunch> generated = action.pattern().create(context, snapshot);
            if (generated == null) {
                logInvalid("Projectile pattern returned null", null);
                return rollbackOr(preparedCost, ProjectileFireResult.INVALID_ACTION_RESULT);
            }
            if (generated.isEmpty()) {
                return rollbackOr(preparedCost, ProjectileFireResult.NO_VALID_PROJECTILE);
            }
            launches = List.copyOf(generated);
            prepareLaunches(context.level(), context.player(), snapshot, launches);
        } catch (RuntimeException exception) {
            logInvalid("Projectile pattern preparation failed", exception);
            return rollbackOr(preparedCost, ProjectileFireResult.INVALID_ACTION_RESULT);
        }

        if (!context.matchesCurrentWeapon()) {
            discardAll(launches);
            return rollbackOr(preparedCost, ProjectileFireResult.WEAPON_CHANGED);
        }
        if (context.player().getCooldowns().isOnCooldown(currentWeapon.getItem())) {
            discardAll(launches);
            return rollbackOr(preparedCost, ProjectileFireResult.COOLDOWN);
        }
        try {
            preparedCost.commit();
        } catch (RuntimeException exception) {
            discardAll(launches);
            logInvalid("Projectile cost commit failed", exception);
            return rollbackOr(preparedCost, ProjectileFireResult.COST_COMMIT_FAILED);
        }

        boolean cooldownAdded = action.cooldownTicks() > 0;
        if (cooldownAdded) {
            context.player().getCooldowns().addCooldown(currentWeapon.getItem(), action.cooldownTicks());
        }

        try {
            for (ProjectileLaunch launch : launches) {
                Projectile projectile = launch.projectile();
                if (!context.level().addFreshEntity(projectile)) {
                    throw new IllegalStateException("Server level rejected a prepared projectile");
                }
            }
        } catch (RuntimeException exception) {
            discardAll(launches);
            if (cooldownAdded)
                context.player().getCooldowns().removeCooldown(currentWeapon.getItem());
            logInvalid("Projectile batch spawn failed and was rolled back", exception);
            return rollbackOr(preparedCost, ProjectileFireResult.SPAWN_ROLLED_BACK);
        }

        context.player().swing(context.hand(), true);
        context.beginSuccessCommit();
        try {
            action.runSuccessAction(context);
        } catch (RuntimeException exception) {
            logInvalid("Projectile success action failed after commit", exception);
        } finally {
            context.endSuccessCommit();
        }
        return ProjectileFireResult.SUCCESS;
    }

    private static void prepareLaunches(
            ServerLevel level,
            ServerPlayer player,
            ProjectileCombatSnapshot snapshot,
            List<ProjectileLaunch> launches
    ) {
        Set<Projectile> identities = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Set<java.util.UUID> uuids = new HashSet<>();
        for (ProjectileLaunch launch : launches) {
            if (launch == null)
                throw new IllegalArgumentException("Projectile launch must not be null");
            Projectile projectile = launch.projectile();
            if (!identities.add(projectile) || !uuids.add(projectile.getUUID())) {
                throw new IllegalArgumentException("Projectile launches must contain unique entities and UUIDs");
            }
            if (projectile.level() != level) {
                throw new IllegalArgumentException("Projectile must belong to the firing server level");
            }
            if (projectile.isRemoved() || level.getEntity(projectile.getUUID()) != null) {
                throw new IllegalArgumentException("Projectile must not already exist in the server level");
            }
            Entity owner = projectile.getOwner();
            if (owner != null && owner != player) {
                throw new IllegalArgumentException("Projectile owner must match the firing player");
            }
            if (owner == null) {
                // 1.20.1 的 AbstractArrow#setOwner 会同时按照玩家模式重写 pickup。构造器已经设置
                // 正确所有者的箭不能重复调用，否则无限箭、特殊箭和多重射击箭的拾取语义会被覆盖。
                projectile.setOwner(player);
            }
            if (!(projectile instanceof ProjectileCombatSnapshotCarrier carrier)) {
                throw new IllegalArgumentException("Projectile must implement ProjectileCombatSnapshotCarrier");
            }
            carrier.setProjectileCombatSnapshot(snapshot);
            projectile.setPos(launch.position().x, launch.position().y, launch.position().z);
            double resolvedVelocity = snapshot.resolvedVelocity() * launch.velocityMultiplier();
            if (!Double.isFinite(resolvedVelocity)) {
                throw new IllegalArgumentException("Resolved launch velocity must be finite");
            }
            projectile.setDeltaMovement(launch.direction().scale(resolvedVelocity));
        }
    }

    private static boolean isDuplicate(ProjectileFireContext context) {
        RequestStamps stamps = REQUEST_STAMPS.computeIfAbsent(context.player(), ignored -> new RequestStamps());
        return stamps.markAndCheckDuplicate(context.hand(), context.trigger(), context.gameTime());
    }

    private static void discardAll(List<ProjectileLaunch> launches) {
        for (ProjectileLaunch launch : launches) {
            if (launch != null) launch.projectile().discard();
        }
    }

    private static ProjectileFireResult rollbackOr(
            PreparedProjectileCost cost,
            ProjectileFireResult successResult
    ) {
        try {
            cost.rollback();
            return successResult;
        } catch (RuntimeException exception) {
            logInvalid("Projectile cost rollback failed", exception);
            return ProjectileFireResult.ROLLBACK_FAILED;
        }
    }

    private static void logInvalid(String message, @Nullable RuntimeException exception) {
        if (exception == null) {
            ConfluenceMagicLib.LOGGER.error(message);
        } else {
            ConfluenceMagicLib.LOGGER.error(message, exception);
        }
    }

    private static final class RequestStamps {
        private final EnumMap<InteractionHand, EnumMap<ProjectileFireTrigger, Long>> values =
                new EnumMap<>(InteractionHand.class);

        private boolean markAndCheckDuplicate(
                InteractionHand hand,
                ProjectileFireTrigger trigger,
                long gameTime
        ) {
            EnumMap<ProjectileFireTrigger, Long> byTrigger = values.computeIfAbsent(
                    hand, ignored -> new EnumMap<>(ProjectileFireTrigger.class));
            Long previous = byTrigger.put(trigger, gameTime);
            return previous != null && previous == gameTime;
        }
    }
}
