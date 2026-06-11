package com.allisighs.caesar.sjp;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

public class SjpCommand extends CommandBase {

    @Override
    public String getName() { return "tch"; }

    @Override
    public java.util.List<String> getAliases() {
        java.util.List<String> a = new java.util.ArrayList<String>();
        a.add("teslive");
        return a;
    }

    @Override
    public String getUsage(ICommandSender sender) { return "/tch"; }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) { return true; }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        SjpKeys.openRequested = true;
    }
}
