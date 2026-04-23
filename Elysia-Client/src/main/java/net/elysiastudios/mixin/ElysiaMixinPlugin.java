package net.elysiastudios.mixin;

import net.elysiastudios.client.compat.EssentialCompat;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class ElysiaMixinPlugin implements IMixinConfigPlugin {
    private static final Set<String> ESSENTIAL_SENSITIVE_MIXINS = Set.of(
        "net.elysiastudios.mixin.client.TitleScreenMixin",
        "net.elysiastudios.mixin.client.AvatarRendererMixin"
    );

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!EssentialCompat.isEssentialLoaded()) {
            return true;
        }

        return !ESSENTIAL_SENSITIVE_MIXINS.contains(mixinClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
