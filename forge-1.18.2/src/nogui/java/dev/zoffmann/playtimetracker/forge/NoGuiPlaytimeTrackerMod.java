package dev.zoffmann.playtimetracker.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkConstants;

/** Server-only build: no custom packets, screens, sidebar buttons or client requirement. */
@Mod(ServerTracking.MOD_ID)
public final class NoGuiPlaytimeTrackerMod extends ServerTracking {
    public NoGuiPlaytimeTrackerMod() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY,
                        (remoteVersion, isServer) -> true));
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) MinecraftForge.EVENT_BUS.register(this);
    }
}
