package dev.zoffmann.playtimetracker.forge.leaderboard;

import dev.zoffmann.playtimetracker.forge.PlaytimeTrackerMod;
import dev.zoffmann.playtimetracker.forge.client.LeaderboardScreen;
import java.util.ArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class TrackerNetwork {
    private static final String VERSION = "1";
    private static final class ChannelHolder {
        private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(PlaytimeTrackerMod.MOD_ID + ":leaderboard"), () -> VERSION, VERSION::equals, VERSION::equals);
    }

    public static void register() {
        ChannelHolder.CHANNEL.messageBuilder(Request.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeBoolean(packet.open))
                .decoder(buffer -> new Request(buffer.readBoolean()))
                .consumer((packet, context) -> {
                    ServerPlayer sender = context.get().getSender();
                    context.get().enqueueWork(() -> { if (sender != null) respond(sender, packet.open); });
                    context.get().setPacketHandled(true);
                }).add();
        ChannelHolder.CHANNEL.messageBuilder(Response.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TrackerNetwork::encode).decoder(TrackerNetwork::decode)
                .consumer((packet, context) -> {
                    context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> LeaderboardScreen.accept(packet.snapshot, packet.open)));
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void request(boolean open) { ChannelHolder.CHANNEL.sendToServer(new Request(open)); }

    public static void respond(ServerPlayer player, boolean open) {
        LeaderboardService service = PlaytimeTrackerMod.leaderboard();
        if (service != null && service.allowRequest(player)) {
            ChannelHolder.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new Response(service.snapshot(), open));
        }
    }

    static void encode(Response packet, FriendlyByteBuf buffer) {
        var snapshot = packet.snapshot;
        buffer.writeBoolean(packet.open).writeBoolean(snapshot.loaded());
        buffer.writeVarInt(snapshot.knownPlayers());
        buffer.writeVarInt(snapshot.players().size());
        for (PlayerStats player : snapshot.players()) {
            buffer.writeUUID(player.id()).writeUtf(player.name(), 64);
            buffer.writeVarLong(player.playTicks()).writeVarLong(player.deaths()).writeVarLong(player.mobKills())
                    .writeVarLong(player.minedBlocks()).writeVarLong(player.distanceCm()).writeVarLong(player.jumps());
            buffer.writeBoolean(player.online()).writeBoolean(player.afk());
        }
    }

    static Response decode(FriendlyByteBuf buffer) {
        boolean open = buffer.readBoolean();
        boolean loaded = buffer.readBoolean();
        int known = buffer.readVarInt();
        int count = buffer.readVarInt();
        if (count < 0 || count > LeaderboardService.MAX_PLAYERS || known < count) {
            throw new IllegalArgumentException("Invalid leaderboard size");
        }
        var players = new ArrayList<PlayerStats>(count);
        for (int i = 0; i < count; i++) {
            players.add(new PlayerStats(buffer.readUUID(), buffer.readUtf(64),
                    buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong(),
                    buffer.readVarLong(), buffer.readVarLong(), buffer.readBoolean(), buffer.readBoolean()));
        }
        return new Response(new LeaderboardService.Snapshot(players, loaded, known), open);
    }

    private record Request(boolean open) {}
    record Response(LeaderboardService.Snapshot snapshot, boolean open) {}
    private TrackerNetwork() {}
}
