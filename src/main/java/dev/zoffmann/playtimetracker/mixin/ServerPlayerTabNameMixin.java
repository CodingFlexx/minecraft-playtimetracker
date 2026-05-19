package dev.zoffmann.playtimetracker.mixin;

import dev.zoffmann.playtimetracker.TabDisplayManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public class ServerPlayerTabNameMixin {
    @Inject(method = "getTabListDisplayName", at = @At("HEAD"), cancellable = true)
    private void playtimetracker$tabName(CallbackInfoReturnable<Component> cir) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        Component name = TabDisplayManager.getDisplayName(self.getUUID());
        if (name != null) {
            cir.setReturnValue(name);
        }
    }
}
