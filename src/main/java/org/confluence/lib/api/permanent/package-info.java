/**
 * 面向附属模组和跨模组联动的永久强化公共 API。
 *
 * <p>通常只需注册一个 {@link org.confluence.lib.api.permanent.PermanentUpgrade}，再将它传给
 * {@link org.confluence.lib.api.permanent.PermanentUpgradeItem}。默认玩家数据由 MagicLib 保存并在死亡后复制；
 * 已有自定义玩家数据的模组可以通过 {@code levelAccess} 接入。效果回调会在首次使用、登录和重生时执行，
 * 因此必须使用替换式、幂等的实现。</p>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
package org.confluence.lib.api.permanent;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
