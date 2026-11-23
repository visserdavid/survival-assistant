package io.github.visserdavid.survivalassistant;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import io.github.visserdavid.survivalassistant.chat.GuideMusings;

/** /guide call | dismiss | extend | continue | chat on|off | musings on|off|all | tip | mute &lt;minutes&gt; | character on|off */
public final class GuideCommand {
	private GuideCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("guide")
				.then(Commands.literal("call").executes(c -> run(c, "call")))
				.then(Commands.literal("dismiss").executes(c -> run(c, "dismiss")))
				.then(Commands.literal("extend").executes(c -> run(c, "extend")))
				.then(Commands.literal("continue").executes(c -> run(c, "continue")))
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
				.then(Commands.literal("musings")
						.then(Commands.literal("on").executes(c -> musings(c, null)))
						.then(Commands.literal("off").executes(c -> musings(c, GuideMusings.OFF)))
						.then(Commands.literal("all").executes(c -> musings(c, GuideMusings.ALL))))
				.then(Commands.literal("chat")
						.then(Commands.literal("on").executes(c -> chat(c, true)))
						.then(Commands.literal("off").executes(c -> chat(c, false))))
				.then(Commands.literal("character")
						.then(Commands.literal("on").executes(c -> character(c, true)))
						.then(Commands.literal("off").executes(c -> character(c, false)))));
	}

	/** /guide musings on|off|all: the guide's remarks about biomes and animals, stored per player. */
	private static int musings(CommandContext<CommandSourceStack> c, String value) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		io.github.visserdavid.survivalassistant.state.GuideState.of(p).musings = value;
		String key = value == null ? "on" : value;
		c.getSource().sendSuccess(() -> Component.translatable("survivalassistant.cmd.musings_" + key), false);
		return 1;
	}

	/** /guide chat on|off: small talk through a local language model, stored per player. */
	private static int chat(CommandContext<CommandSourceStack> c, boolean enabled) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		io.github.visserdavid.survivalassistant.state.GuideState.of(p).chatEnabled = enabled;
		String endpoint = io.github.visserdavid.survivalassistant.config.ModConfig.get().chatEndpoint;
		c.getSource().sendSuccess(() -> enabled
				? Component.translatable("survivalassistant.cmd.chat_on", endpoint)
				: Component.translatable("survivalassistant.cmd.chat_off"), false);
		return 1;
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
			case "extend" -> GuideManager.extend(p);
			case "continue" -> GuideManager.continueChapter(p);
			default -> GuideManager.tipNow(p);
		};
		if (!ok) {
			c.getSource().sendFailure(Component.translatable("survivalassistant.cmd.unavailable"));
			return 0;
		}
		return 1;
	}
}
