package keystrokesmod.module.impl.movement;

import keystrokesmod.event.PreMotionEvent;
import keystrokesmod.event.ReceivePacketEvent;
import keystrokesmod.event.SendPacketEvent;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.Utils;
import net.minecraft.init.Items;
import net.minecraft.item.ItemFireball;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

public class LongJump extends Module {
    private final SliderSetting mode;
    private final SliderSetting horizontalBoost;
    private final SliderSetting motionTicks;
    private final ButtonSetting invertYaw;
    private final ButtonSetting jump;
    private int lastSlot = -1;
    private int ticks = -1;
    private boolean setSpeed;
    public static boolean stopModules;
    private boolean sentPlace;
    private int initTicks;
    private boolean threw;

    public LongJump() {
        super("Long Jump", category.movement);
        this.registerSetting(mode = new SliderSetting("Mode", new String[]{"Fireball", "Fireball Auto"}, 0));
        this.registerSetting(horizontalBoost = new SliderSetting("Horizontal boost", 1.7, 0.0, 8.0, 0.1));
        this.registerSetting(motionTicks = new SliderSetting("Motion ticks", 10, 1, 40, 1));
        this.registerSetting(invertYaw = new ButtonSetting("Invert yaw", true));
        this.registerSetting(jump = new ButtonSetting("Jump", false));
    }

    @SubscribeEvent
    public void onSendPacket(@NotNull SendPacketEvent e) {
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement && ((C08PacketPlayerBlockPlacement) e.getPacket()).getStack() != null && ((C08PacketPlayerBlockPlacement) e.getPacket()).getStack().getItem() instanceof ItemFireball) {
            threw = true;
            if (mc.thePlayer.onGround && jump.isToggled()) {
                mc.thePlayer.jump();
            }
        }
    }

    @SubscribeEvent
    public void onReceivePacket(@NotNull ReceivePacketEvent e) {
        if (e.getPacket() instanceof S12PacketEntityVelocity && Utils.nullCheck()) {
            if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId() && threw) {
                ticks = 0;
                setSpeed = true;
                threw = false;
                stopModules = true;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPreMotion(PreMotionEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (mode.getInput() == 1) {
            if (initTicks == 0) {
                if (invertYaw.isToggled()) {
                    e.setYaw(mc.thePlayer.rotationYaw - 180);
                    e.setPitch(89);
                }
                else {
                    e.setPitch(90);
                }
                int fireballSlot = getFireball();
                if (fireballSlot != -1 && fireballSlot != mc.thePlayer.inventory.currentItem) {
                    lastSlot = mc.thePlayer.inventory.currentItem;
                    mc.thePlayer.inventory.currentItem = fireballSlot;
                }
            }
            else if (initTicks == 1) {
                if (!sentPlace) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                    sentPlace = true;
                }
            }
            else if (initTicks == 2) {
                if (lastSlot != -1) {
                    mc.thePlayer.inventory.currentItem = lastSlot;
                    lastSlot = -1;
                }
            }
            if (ticks > motionTicks.getInput()) {
                this.disable();
                return;
            }
            if (setSpeed) {
                stopModules = true;
                setSpeed();
                ticks++;
            }
            if (initTicks < 3) {
                initTicks++;
            }
        }
        else {
            if (setSpeed) {
                if (ticks > motionTicks.getInput()) {
                    stopModules = setSpeed = false;
                    ticks = 0;
                    return;
                }
                stopModules = true;
                ticks++;
                setSpeed();
            }
        }
    }

    public void onDisable() {
        if (lastSlot != -1 && mode.getInput() == 1) {
            mc.thePlayer.inventory.currentItem = lastSlot;
        }
        ticks = lastSlot = -1;
        setSpeed = stopModules = sentPlace = false;
        initTicks = 0;
    }

    public void onEnable() {
        if (getFireball() == -1 && mode.getInput() == 1) {
            Utils.sendMessage("§cNo fireball found.");
            this.disable();
            return;
        }
        if (mode.getInput() == 1) {
            stopModules = true;
        }
    }

    private void setSpeed() {
        mc.thePlayer.motionY = 0.0;
        if (horizontalBoost.getInput() != 0.0) {
            Utils.setSpeed(horizontalBoost.getInput());
        }
    }

    private int getFireball() {
        int n = -1;
        for (int i = 0; i < 9; ++i) {
            final ItemStack getStackInSlot = mc.thePlayer.inventory.getStackInSlot(i);
            if (getStackInSlot != null && getStackInSlot.getItem() == Items.fire_charge) {
                n = i;
                break;
            }
        }
        return n;
    }
}
