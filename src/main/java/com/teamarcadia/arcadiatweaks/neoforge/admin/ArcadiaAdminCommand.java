package com.teamarcadia.arcadiatweaks.neoforge.admin;

import com.mojang.brigadier.Command;
import com.teamarcadia.arcadiatweaks.common.module.ArcadiaPatchVariant;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ArcadiaAdminCommand {

    private ArcadiaAdminCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(ArcadiaPatchVariant.commandRoot())
                .requires(source -> source.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("gui")
                        .executes(ctx -> open(ctx.getSource().getPlayerOrException())))
                .then(Commands.literal("admin")
                        .executes(ctx -> open(ctx.getSource().getPlayerOrException()))));
    }

    private static int open(ServerPlayer player) {
        ArcadiaAdminMenu.open(player);
        return Command.SINGLE_SUCCESS;
    }
}
