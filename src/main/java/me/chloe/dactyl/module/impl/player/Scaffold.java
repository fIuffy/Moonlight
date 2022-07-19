package me.chloe.dactyl.module.impl.player;

import me.chloe.dactyl.event.ForgeEvent;
import me.chloe.dactyl.event.impl.player.EventUpdateWalkingPlayer;
import me.chloe.dactyl.event.impl.player.MoveEvent;
import me.chloe.dactyl.module.impl.movement.ElytraFly;
import me.chloe.dactyl.module.impl.movement.LongJump;
import me.chloe.dactyl.util.PlaceUtil;
import me.chloe.dactyl.util.TimeUtil;
import me.chloe.dactyl.module.Module;
import me.chloe.dactyl.setting.Setting;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Scaffold extends Module {
    public Setting<Double> expand = new Setting<Double>("Offset", 1.0d, 0.1d, 6.0d);
    public Setting<Boolean> packetSwitch = new Setting<Boolean>("SilentSwitch", false);
    public Setting<SneakMode> sneakModeSetting = new Setting<SneakMode>("AntiFall", SneakMode.SNEAK);
    public Setting<Integer> checkDelay = new Setting<Integer>("Check Delay", 100, 1, 1000, vis->sneakModeSetting.getValue()==SneakMode.STRICT);
    public Setting<Boolean> rotate = new Setting<Boolean>("Rotate", true);

    public static Scaffold INSTANCE;
    public Scaffold() {
        super("Scaffold", Category.PLAYER);
        INSTANCE = this;
    }

    private TimeUtil timerMotion = new TimeUtil();

    private TimeUtil blockCheckTimer = new TimeUtil();

    private BlockData blockData;

    public boolean Switch = true;

    public boolean tower = true;

    public boolean center = true;

    public boolean keepY = false;

    public boolean doWait = false;

    public int lastY;

    public float lastYaw;

    public float lastPitch;

    public BlockPos pos;

    public boolean teleported;

    @SubscribeEvent
    public void onMove(MoveEvent event) {
        if (event.getStage() != ForgeEvent.Stage.PRE) {
            return;
        }
        if (Freecam.INSTANCE.isEnabled()) {
            return;
        }
        if (LongJump.INSTANCE.isEnabled()) {
            return;
        }
        if (mc.player.isElytraFlying() || mc.player.isInLava() || mc.player.isInWater() || ElytraFly.INSTANCE.isEnabled()) {
            return;
        }
        if(sneakModeSetting.getValue() == SneakMode.STRICT && doWait) {
            if(blockCheckTimer.hasPassed(checkDelay.getValue().longValue())) {
                doWait = false;
            } else {
                event.setMotion(0.0d, 0.0d, 0.0d);
            }
        }
    }

    @SubscribeEvent
    public void onUpdate(EventUpdateWalkingPlayer event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if(event.getStage() == ForgeEvent.Stage.PRE) {
            int downDistance = 1;
            if (this.keepY) {
                if ((!((mc.player.moveForward != 0.0F || mc.player.moveStrafing != 0.0F)) && mc.gameSettings.keyBindJump.isKeyDown()) || mc.player.collidedVertically || mc.player.onGround)
                    this.lastY = MathHelper.floor(mc.player.posY);
            } else {
                this.lastY = MathHelper.floor(mc.player.posY);
            }
            this.blockData = null;
            double x = mc.player.posX;
            double z = mc.player.posZ;
            double y = this.keepY ? this.lastY : mc.player.posY;
            double forward = mc.player.movementInput.moveForward;
            double strafe = mc.player.movementInput.moveStrafe;
            float yaw = mc.player.rotationYaw;
            if (!mc.player.collidedHorizontally) {
                double[] coords = PlaceUtil.getExpandCoords(x, z, forward, strafe, yaw);
                x = coords[0];
                z = coords[1];
            }
            if (PlaceUtil.canPlace(mc.world.getBlockState(new BlockPos(mc.player.posX, mc.player.posY - downDistance, mc.player.posZ)).getBlock())) {
                x = mc.player.posX;
                z = mc.player.posZ;
            }
            BlockPos blockBelow = new BlockPos(x, y - downDistance, z);
            this.pos = blockBelow;
            if (mc.world.getBlockState(blockBelow).getBlock() == Blocks.AIR || (mc.world.getBlockState(blockBelow).getBlock().equals(Blocks.WATER) || mc.world.getBlockState(blockBelow).getBlock().equals(Blocks.FLOWING_WATER) || mc.world.getBlockState(blockBelow).getBlock().equals(Blocks.LAVA) || mc.world.getBlockState(blockBelow).getBlock().equals(Blocks.FLOWING_LAVA))) {
                this.blockData = PlaceUtil.getBlockData2(blockBelow);
                if (this.blockData != null) {
                    float yaw1 = PlaceUtil.aimAtLocation(this.blockData.position.getX(), this.blockData.position.getY(), this.blockData.position.getZ(), this.blockData.face)[0];
                    float pitch = PlaceUtil.aimAtLocation(this.blockData.position.getX(), this.blockData.position.getY(), this.blockData.position.getZ(), this.blockData.face)[1];
                    if(rotate.getValue()) {
                        event.setYaw(yaw1);
                        event.setPitch(pitch);
                    }
                }
            }
        } else if (this.blockData != null) {
            if (PlaceUtil.getBlockCountHotbar() <= 0 || (!this.Switch && mc.player.getHeldItemMainhand().getItem() != null && !(mc.player.getHeldItemMainhand().getItem() instanceof ItemBlock))) {
                return;
            }
            int heldItem = mc.player.inventory.currentItem;
            if (this.Switch && !(mc.player.getHeldItemMainhand().getItem() instanceof ItemBlock)) {
                for (int j = 0; j < 9; j++) {
                    if (mc.player.inventory.getStackInSlot(j) != null && mc.player.inventory.getStackInSlot(j).getCount() != 0 && mc.player.inventory.getStackInSlot(j).getItem() instanceof ItemBlock && !PlaceUtil.invalid.contains(((ItemBlock) mc.player.inventory.getStackInSlot(j).getItem()).getBlock())) {
                        if (packetSwitch.getValue()) {
                            mc.player.connection.sendPacket(new CPacketHeldItemChange(j));
                        } else {
                            mc.player.inventory.currentItem = j;
                        }
                        break;
                    }
                }
            }
            boolean towering = false;
            if (this.tower) {
                towering = mc.gameSettings.keyBindJump.isKeyDown();
                if (towering && mc.player.moveForward == 0.0F && mc.player.moveStrafing == 0.0F && this.tower && !mc.player.isPotionActive(MobEffects.JUMP_BOOST)) {
                    if (!this.teleported && this.center) {
                        this.teleported = true;
                        BlockPos pos = new BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ);
                        mc.player.setPosition(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                    }
                    if (this.center && !this.teleported)
                        return;
                    mc.player.motionY = 0.41999998688697815D;
                    mc.player.motionZ = 0.0D;
                    mc.player.motionX = 0.0D;
                    if (this.timerMotion.sleep(1500L))
                        mc.player.motionY = -0.28D;
                } else {
                    this.timerMotion.reset();
                    if (this.teleported && this.center)
                        this.teleported = false;
                }
            }
            Vec3d hitVec = new Vec3d(this.blockData.position).add(0.5, 0.5, 0.5)
                    .add(new Vec3d(this.blockData.face.getDirectionVec()).scale(0.5));
            //if (mc.playerController.processRightClickBlock(mc.player, mc.world, this.blockData.position, this.blockData.face, new Vec3d(this.blockData.position.getX() + Math.random(), this.blockData.position.getY() + Math.random(), this.blockData.position.getZ() + Math.random()), EnumHand.MAIN_HAND) != EnumActionResult.FAIL) {
            mc.playerController.processRightClickBlock(mc.player, mc.world, this.blockData.position, this.blockData.face, hitVec, EnumHand.MAIN_HAND);
            mc.player.connection.sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND));
            if(!towering) {
                doWait = true;
                blockCheckTimer.reset();
            }
            //}
            if(packetSwitch.getValue()) {
                mc.player.connection.sendPacket(new CPacketHeldItemChange(heldItem));
            } else {
                mc.player.inventory.currentItem = heldItem;
            }
        }
    }

    public boolean shouldSneak() {
        return this.sneakModeSetting.getValue() == SneakMode.SNEAK;
    }

    public enum SneakMode {
        SNEAK,
        STRICT
    }


    public static class BlockData {
        public BlockPos position;

        public EnumFacing face;

        public BlockData(BlockPos position, EnumFacing face) {
            this.position = position;
            this.face = face;
        }
    }
}
