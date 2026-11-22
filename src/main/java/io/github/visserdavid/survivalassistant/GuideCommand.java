package io.github.visserdavid.survivalassistant;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /guide call | dismiss | tip | mute &lt;minutes&gt; | character on|off */
public final class GuideCommand {
	private GuideCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("guide")
				.then(Commands.literal("call").executes(c -> run(c, "call")))
				.then(Commands.literal("dismiss").executes(c -> run(c, "dismiss")))
				.then(Commands.literal("tip").executes(c -> run(c, "tip")))
				.then(Commands.literal("mute")
						.then(Commands.argument("minutes", IntegerArgumentType.integer(0, 120))
								.executes(c -> {
									ServerPlayer p = c.getSource().getPlayerOrException();
									int m = IntegerArgumentType.getInteger(c, "minutes");
									GuideManager.mute(p, m);
									c.getSource().sendSuccess(() -> Component.translatable("survivalassistant.cmd.muted", m), false);
									return 1;
								})))
				.then(Commands.literal("character")
						.then(Commands.literal("on").executes(c -> character(c, true)))
						.then(Commands.literal("off").executes(c -> character(c, false)))));
	}

	private static int character(CommandContext<CommandSourceStack> c, boolean enabled) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		GuideManager.setNpcEnabled(p, enabled);
		c.getSource().sendSuccess(() -> Component.translatable(
				enabled ? "survivalassistant.cmd.character_on" : "survivalassistant.cmd.character_off"), false);
		return 1;
	}

	private static int run(CommandContext<CommandSourceStack> c, String action) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		if (action.equals("call") && !GuideManager.npcEnabled(p)) {
			c.getSource().sendFailure(Component.translatable("survivalassistant.cmd.npc_off_recall"));
			return 0;
		}
		boolean ok = switch (action) {
			case "call" -> GuideManager.recall(p);
			case "dismiss" -> GuideManager.dismiss(p);
			default -> GuideManager.tipNow(p);
		};
		if (!ok) {
			c.getSource().sendFailure(Component.translatable("survivalassistant.cmd.unavailable"));
			return 0;
		}
		return 1;
	}
}
