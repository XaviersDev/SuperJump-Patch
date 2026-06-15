package com.allisighs.caesar;

import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketPlayerAbilities;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.client.ClientCommandHandler;
import org.lwjgl.input.Mouse;

import com.allisighs.caesar.sjp.SjpCommand;
import com.allisighs.caesar.sjp.SjpKeys;
import com.allisighs.caesar.sjp.gui.StatHud;
import com.allisighs.caesar.sjp.config.SjpConfig;

@Mod(modid = CaesarMain.MODID, name = CaesarMain.NAME, version = CaesarMain.VERSION, clientSideOnly = true)
public class CaesarMain {

	
    public static final String MODID = "caesarmod";
    public static final String NAME = "Teslive Community Hub";
    public static final String VERSION = "1.0";

    private static final String AXE_NAME = "Топор охотника";
    protected Minecraft mc = Minecraft.getMinecraft();

    private boolean wasRmbDown = false;
    
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
        com.allisighs.caesar.sjp.stats.StatManager.INST.onJoin();
    }


    @SubscribeEvent
    public void onTik(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (mc.player == null || event.player != mc.player) return;
        if (!SjpConfig.get().modEnabled) return;

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
    }
}
