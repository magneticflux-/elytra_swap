package net.szum123321.elytra_swap.mixin;

import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.szum123321.elytra_swap.ElytraSwap;
import net.szum123321.elytra_swap.PlayerSwapDataHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerEntity.class)
public abstract class PlayerFallCapture extends LivingEntity {
    protected PlayerFallCapture(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition){
        fallingHandler(heightDifference, onGround, landedState, landedPosition);
        super.fall(heightDifference, onGround, landedState, landedPosition);
    }

    private void fallingHandler(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition){
        if((Object)this instanceof ServerPlayerEntity) {
            PlayerEntity player = (PlayerEntity) (Object) this;

            if(checkIfPlayerHasElytra(player)){
                if(!PlayerSwapDataHandler.get(player))
                    return;

                if(!onGround){
                    if (PlayerSwapDataHandler.get(player) && heightDifference < 0 && getFallHeight(landedPosition) > 5 && (ServerSidePacketRegistry.INSTANCE.canPlayerReceive(player, ElytraSwap.DUMMY_PACKAGE) || ElytraSwap.config.noModPlayersHandlingMethod > 0)) {
                        replaceArmorWithElytra(player);
                        setSevenFlagState(true);    // thanks to this line you do not have to press space in order to start gliding
                    }
                }else if(PlayerSwapDataHandler.get(player) && (ServerSidePacketRegistry.INSTANCE.canPlayerReceive(player, ElytraSwap.DUMMY_PACKAGE) || ElytraSwap.config.noModPlayersHandlingMethod > 0)){
                    replaceElytraWithArmor(player);
                    setSevenFlagState(false);
                }
            }
        }
    }

    private void setSevenFlagState(boolean val){
        setFlag(7, val);
    }

    private void replaceElytraWithArmor(PlayerEntity player){
        if(player.inventory.armor.get(2).getItem() == Items.ELYTRA){
            for(int i = 0; i < player.inventory.main.size(); i++){
                if(player.inventory.main.get(i).getItem().toString().toLowerCase().contains("chestplate")){  //kinda sketchy but should make this compatible with modded armor
                    ItemStack elytra = player.inventory.armor.get(2);
                    player.inventory.armor.set(2, player.inventory.main.get(i));
                    player.inventory.main.set(i, elytra);
                }
            }
        }
    }

    private void replaceArmorWithElytra(PlayerEntity player){
        for (int i = 0; i < player.inventory.main.size(); i++){
            if(player.inventory.main.get(i).getItem() == Items.ELYTRA){
                ItemStack chestplate = player.inventory.armor.get(2);

                player.inventory.armor.set(2, player.inventory.main.get(i));
                player.inventory.main.set(i, chestplate);

                return;
            }
        }
    }

    private int getFallHeight(BlockPos currentPosition){
        int height = currentPosition.getY();

        while(!world.getBlockState(new BlockPos(currentPosition.getX(), height, currentPosition.getZ())).getMaterial().isSolid() && height > -1){
            height--;
        }

        if(height <= -1){
            ElytraSwap.LOGGER.info("WTF! Why are you trying to glide below bedrock?");
            return -Math.abs(currentPosition.getY()) * 2; //even if you would fly below bedrock it should work :)
        }
        return currentPosition.getY() - height;
    }

    private boolean checkIfPlayerHasElytra(PlayerEntity player){
        return player.inventory.contains(new ItemStack(Items.ELYTRA));
    }
}
