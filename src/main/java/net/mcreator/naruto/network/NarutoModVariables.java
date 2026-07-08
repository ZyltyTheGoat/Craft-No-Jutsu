package net.mcreator.naruto.network;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

import net.mcreator.naruto.NarutoMod;

import java.util.function.Supplier;

@EventBusSubscriber
public class NarutoModVariables {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NarutoMod.MODID);
	public static final Supplier<AttachmentType<PlayerVariables>> PLAYER_VARIABLES = ATTACHMENT_TYPES.register("player_variables", () -> AttachmentType.serializable(() -> new PlayerVariables()).build());

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		NarutoMod.addNetworkMessage(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.STREAM_CODEC, PlayerVariablesSyncMessage::handleData);
	}

	@SubscribeEvent
	public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES), player.getId()));
	}

	@SubscribeEvent
	public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES), player.getId()));
	}

	@SubscribeEvent
	public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES), player.getId()));
	}

	@SubscribeEvent
	public static void onPlayerTickUpdateSyncPlayerVariables(PlayerTickEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer player && player.getData(PLAYER_VARIABLES)._syncDirty) {
			PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES), player.getId()));
			player.getData(PLAYER_VARIABLES)._syncDirty = false;
		}
	}

	@SubscribeEvent
	public static void clonePlayer(PlayerEvent.Clone event) {
		PlayerVariables original = event.getOriginal().getData(PLAYER_VARIABLES);
		PlayerVariables clone = new PlayerVariables();
		clone.rank = original.rank;
		clone.unlockedDojutsu = original.unlockedDojutsu;
		clone.unlockedJutsu = original.unlockedJutsu;
		clone.unlockedKekkeiGenkai = original.unlockedKekkeiGenkai;
		clone.unlockedNatures = original.unlockedNatures;
		clone.maxChakra = original.maxChakra;
		clone.activeJutsu = original.activeJutsu;
		clone.favouriteJutsu = original.favouriteJutsu;
		if (!event.isWasDeath()) {
			clone.currentChakra = original.currentChakra;
			clone.activeToggleJutsu = original.activeToggleJutsu;
			clone.chargeTime = original.chargeTime;
			clone.chargingAbility = original.chargingAbility;
			clone.attackDamage = original.attackDamage;
			clone.activeDojutsu = original.activeDojutsu;
		}
		event.getEntity().setData(PLAYER_VARIABLES, clone);
	}

	public static class PlayerVariables implements INBTSerializable<CompoundTag> {
		boolean _syncDirty = false;
		public String rank = "";
		public String unlockedDojutsu = "";
		public String unlockedJutsu = "";
		public String unlockedKekkeiGenkai = "";
		public String unlockedNatures = "";
		public double currentChakra = 0;
		public double maxChakra = 0.0;
		public String activeJutsu = "";
		public String activeToggleJutsu = "";
		public String favouriteJutsu = "";
		public double chargeTime = 0;
		public boolean chargingAbility = false;
		public double attackDamage = 0.0;
		public String activeDojutsu = "";

		@Override
		public CompoundTag serializeNBT(HolderLookup.Provider lookupProvider) {
			CompoundTag nbt = new CompoundTag();
			nbt.putString("rank", rank);
			nbt.putString("unlockedDojutsu", unlockedDojutsu);
			nbt.putString("unlockedJutsu", unlockedJutsu);
			nbt.putString("unlockedKekkeiGenkai", unlockedKekkeiGenkai);
			nbt.putString("unlockedNatures", unlockedNatures);
			nbt.putDouble("currentChakra", currentChakra);
			nbt.putDouble("maxChakra", maxChakra);
			nbt.putString("activeJutsu", activeJutsu);
			nbt.putString("activeToggleJutsu", activeToggleJutsu);
			nbt.putString("favouriteJutsu", favouriteJutsu);
			nbt.putDouble("chargeTime", chargeTime);
			nbt.putBoolean("chargingAbility", chargingAbility);
			nbt.putDouble("attackDamage", attackDamage);
			nbt.putString("activeDojutsu", activeDojutsu);
			return nbt;
		}

		@Override
		public void deserializeNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
			rank = nbt.getString("rank");
			unlockedDojutsu = nbt.getString("unlockedDojutsu");
			unlockedJutsu = nbt.getString("unlockedJutsu");
			unlockedKekkeiGenkai = nbt.getString("unlockedKekkeiGenkai");
			unlockedNatures = nbt.getString("unlockedNatures");
			currentChakra = nbt.getDouble("currentChakra");
			maxChakra = nbt.getDouble("maxChakra");
			activeJutsu = nbt.getString("activeJutsu");
			activeToggleJutsu = nbt.getString("activeToggleJutsu");
			favouriteJutsu = nbt.getString("favouriteJutsu");
			chargeTime = nbt.getDouble("chargeTime");
			chargingAbility = nbt.getBoolean("chargingAbility");
			attackDamage = nbt.getDouble("attackDamage");
			activeDojutsu = nbt.getString("activeDojutsu");
		}

		public void markSyncDirty() {
			_syncDirty = true;
		}
	}

	public record PlayerVariablesSyncMessage(PlayerVariables data, int player) implements CustomPacketPayload {
		public static final Type<PlayerVariablesSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(NarutoMod.MODID, "player_variables_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PlayerVariablesSyncMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, PlayerVariablesSyncMessage message) -> {
			buffer.writeInt(message.player());
			buffer.writeNbt(message.data().serializeNBT(buffer.registryAccess()));
		}, (RegistryFriendlyByteBuf buffer) -> {
			PlayerVariablesSyncMessage message = new PlayerVariablesSyncMessage(new PlayerVariables(), buffer.readInt());
			message.data.deserializeNBT(buffer.registryAccess(), buffer.readNbt());
			return message;
		});

		@Override
		public Type<PlayerVariablesSyncMessage> type() {
			return TYPE;
		}

		public static void handleData(final PlayerVariablesSyncMessage message, final IPayloadContext context) {
			if (context.flow() == PacketFlow.CLIENTBOUND && message.data != null) {
				context.enqueueWork(() -> {
					Entity player = context.player().level().getEntity(message.player);
					if (player == null)
						return;
					player.getData(PLAYER_VARIABLES).deserializeNBT(context.player().registryAccess(), message.data.serializeNBT(context.player().registryAccess()));
				}).exceptionally(e -> {
					context.connection().disconnect(Component.literal(e.getMessage()));
					return null;
				});
			}
		}
	}
}