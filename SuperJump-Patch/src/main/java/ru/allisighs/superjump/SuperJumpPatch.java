package ru.allisighs.superjump;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayerAbilities;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;

@Mod(modid = SuperJumpPatch.MODID, name = SuperJumpPatch.NAME, version = SuperJumpPatch.VERSION, clientSideOnly = true)
public class SuperJumpPatch {

    public static final String MODID = "superjumppatch";
    public static final String NAME = "SuperJump-Patch";
    public static final String VERSION = "1.0";

    private static final String AXE_NAME = "Топор охотника";
    private static final int BOOST_WINDOW = 8;
    private static final float BOOST_MULT = 2.0f;

    private final Minecraft mc = Minecraft.getMinecraft();

    private boolean wasRmbDown = false;
    private int boostTicks = 0;
    private ChannelDuplexHandler packetListener;

    @Mod.EventHandler
    public void setUp(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onConect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        injectHandelr();
    }

    @SubscribeEvent
    public void onTik(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.player == null || event.player != mc.player) return;

        checkHandler();

        if (boostTicks > 0) boostTicks--;

        if (mc.currentScreen != null) {
            wasRmbDown = Mouse.isButtonDown(1);
            return;
        }

        boolean rmbDown = Mouse.isButtonDown(1);
        if (rmbDown && !wasRmbDown && isHoldingAxe()) {
            jumpp();
        }
        wasRmbDown = rmbDown;
    }

    private boolean isHoldingAxe() {
        ItemStack held = mc.player.getHeldItemMainhand();
        return held != null
                && held.getItem() == Items.DIAMOND_AXE
                && held.hasDisplayName()
                && held.getDisplayName().contains(AXE_NAME);
    }

    private void jumpp() {
        if (mc.player.connection == null) return;

        boolean prevAllowFlying = mc.player.capabilities.allowFlying;
        mc.player.capabilities.allowFlying = true;
        mc.player.capabilities.isFlying = true;
        mc.player.connection.sendPacket(new CPacketPlayerAbilities(mc.player.capabilities));
        mc.player.capabilities.isFlying = false;
        mc.player.capabilities.allowFlying = prevAllowFlying;

        boostTicks = BOOST_WINDOW;
    }

    private void checkHandler() {
        if (mc.player == null || mc.player.connection == null) return;
        try {
            if (getPipline().get("sj_handler") == null) {
                injectHandelr();
            }
        } catch (Exception ignored) {}
    }

    private void injectHandelr() {
        if (mc.player == null || mc.player.connection == null) return;
        try {
            if (getPipline().get("sj_handler") != null) {
                getPipline().remove("sj_handler");
            }
            packetListener = new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    onPaket(msg);
                    super.channelRead(ctx, msg);
                }
            };
            getPipline().addBefore("packet_handler", "sj_handler", packetListener);
        } catch (Exception ignored) {}
    }

    private void onPaket(Object msg) {
        if (!(msg instanceof SPacketEntityVelocity) || mc.player == null) return;

        SPacketEntityVelocity packet = (SPacketEntityVelocity) msg;
        if (packet.getEntityID() != mc.player.getEntityId() || boostTicks <= 0) return;

        int boosted = (int) (packet.getMotionY() * BOOST_MULT);
        setMotionY(packet, boosted);
        mc.addScheduledTask(() -> mc.player.fallDistance = 0);
        boostTicks = 0;
    }

    private static void setMotionY(SPacketEntityVelocity packet, int value) {
        try {
            Field field = ReflectionHelper.findField(SPacketEntityVelocity.class, "field_149415_c", "motionY");
            field.setAccessible(true);
            field.setInt(packet, value);
        } catch (Exception ignored) {}
    }

    private io.netty.channel.ChannelPipeline getPipline() {
        return mc.player.connection.getNetworkManager().channel().pipeline();
    }
}
