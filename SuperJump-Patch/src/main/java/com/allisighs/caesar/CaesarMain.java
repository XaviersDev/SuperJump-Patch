package com.allisighs.caesar;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
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
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Mouse;

import com.allisighs.caesar.sjp.SjpCommand;
import com.allisighs.caesar.sjp.SjpKeys;
import com.allisighs.caesar.sjp.gui.StatHud;
import com.allisighs.caesar.sjp.config.SjpConfig;

import java.lang.reflect.Field;

@Mod(modid = CaesarMain.MODID, name = CaesarMain.NAME, version = CaesarMain.VERSION, clientSideOnly = true)
public class CaesarMain {

	
    public static final String MODID = "caesarmod";
    public static final String NAME = "Teslive Community Hub";
    public static final String VERSION = "1.0";

    private static final String AXE_NAME = "Топор охотника";
    private static final int BOOST_WINDOW = 8;
    private static final float BOOST_MULT = 2.0f;
    protected Minecraft mc = Minecraft.getMinecraft();

    private boolean wasRmbDown = false;
    private int boostTicks = 0;
    private ChannelDuplexHandler packetListener;
    
    public CaesarMain() {
    }

    @Mod.EventHandler
    public void setUp(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);

        SjpConfig.load();
        SjpKeys.register();
        SjpKeys keys = new SjpKeys();
        MinecraftForge.EVENT_BUS.register(keys);
        com.allisighs.caesar.sjp.ZoomHandler.register();
        MinecraftForge.EVENT_BUS.register(new com.allisighs.caesar.sjp.ZoomHandler());
        MinecraftForge.EVENT_BUS.register(new StatHud());
        MinecraftForge.EVENT_BUS.register(new com.allisighs.caesar.sjp.stats.StatChatHandler());
        MinecraftForge.EVENT_BUS.register(new com.allisighs.caesar.sjp.chat.ChatManager());
        MinecraftForge.EVENT_BUS.register(new com.allisighs.caesar.sjp.stats.AfkHandler());
        MinecraftForge.EVENT_BUS.register(new com.allisighs.caesar.sjp.pvp.PvpEsp());
        MinecraftForge.EVENT_BUS.register(new com.allisighs.caesar.sjp.pvp.EntityCuller());
        MinecraftForge.EVENT_BUS.register(new com.allisighs.caesar.sjp.account.AccountButton());
        MinecraftForge.EVENT_BUS.register(new com.allisighs.caesar.sjp.troll.TrollHandler());
        ClientCommandHandler.instance.registerCommand(new SjpCommand());
    }

    @SubscribeEvent
    public void onConect(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        injectHandelr();
        com.allisighs.caesar.sjp.stats.StatManager.INST.onJoin();
    }


    @SubscribeEvent
    public void onTik(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.player == null || event.player != mc.player) return;
        if (!SjpConfig.get().modEnabled) return;
        checkHandler();

        if (boostTicks > 0) boostTicks--;

        if (mc.currentScreen != null) {
            wasRmbDown = Mouse.isButtonDown(1);
            return;
        }

        if (!SjpConfig.get().superJump) {
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