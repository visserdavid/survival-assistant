package io.github.visserdavid.survivalassistant;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.visserdavid.survivalassistant.advice.Chapter;
import io.github.visserdavid.survivalassistant.advice.Chapters;
import io.github.visserdavid.survivalassistant.advice.GuideNames;
import io.github.visserdavid.survivalassistant.advice.Routes;
import io.github.visserdavid.survivalassistant.chat.GuideMusings;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.state.GuideState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/** /guide call | dismiss | continue | chapters | chapter &lt;id&gt; | route [&lt;id&gt;|free] | about | name &lt;name&gt;|reset | chat on|off | musings on|off|all | tip | mute &lt;minutes&gt; | character on|off */
public final class GuideCommand {
	private GuideCommand() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("guide")
				.then(Commands.literal("call").executes(GuideCommand::call))
				.then(Commands.literal("dismiss").executes(c -> run(c, GuideActions::dismiss)))
				.then(Commands.literal("continue").executes(c -> run(c, GuideActions::continueChapter)))
				.then(Commands.literal("chapters").executes(c -> run(c, GuideActions::chapters)))
				.then(Commands.literal("chapter")
						.then(Commands.argument("id", StringArgumentType.word())
								.suggests((c, b) -> SharedSuggestionProvider.suggest(Chapters.ALL.stream().map(Chapter::id), b))
								.executes(GuideCommand::chapter)))
				.then(Commands.literal("route").executes(c -> run(c, GuideActions::routes))
						.then(Commands.argument("route", StringArgumentType.word())
								.suggests((c, b) -> SharedSuggestionProvider.suggest(Routes.CHOICES, b))
								.executes(c -> run(c, p -> GuideActions.chooseRoute(p, StringArgumentType.getString(c, "route"))))))
				.then(Commands.literal("tip").executes(c -> run(c, GuideActions::tipNow)))
				.then(Commands.literal("about").executes(c -> run(c, GuideActions::tellAbout)))
				.then(Commands.literal("name")
						.then(Commands.literal("reset").executes(c -> name(c, null)))
						.then(Commands.argument("name", StringArgumentType.greedyString())
								.executes(c -> name(c, StringArgumentType.getString(c, "name")))))
				.then(Commands.literal("mute")
						.then(Commands.argument("minutes", IntegerArgumentType.integer(0, 120))
								.executes(c -> {
									ServerPlayer p = c.getSource().getPlayerOrException();
									int m = IntegerArgumentType.getInteger(c, "minutes");
									GuideActions.mute(p, m);
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
	private static int musings(CommandContext<CommandSourceStack> c, @Nullable String value) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		GuideState.of(p).musings = value;
		String key = value == null ? "on" : value;
		c.getSource().sendSuccess(() -> Component.translatable("survivalassistant.cmd.musings_" + key), false);
		return 1;
	}

	/** /guide chapter &lt;id&gt;: start or switch to a chapter that is open. */
	private static int chapter(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		Chapter chapter = Chapters.byId(StringArgumentType.getString(c, "id"));
		if (chapter == null || !GuideActions.startChapter(p, chapter)) {
			c.getSource().sendFailure(Component.translatable("survivalassistant.chapters.unavailable"));
			return 0;
		}
		return 1;
	}

	/** /guide name &lt;name&gt;|reset: the player's own name for the guide. */
	private static int name(CommandContext<CommandSourceStack> c, @Nullable String name) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		if (name != null && !GuideNames.valid(name)) {
			c.getSource().sendFailure(Component.translatable("survivalassistant.cmd.name_invalid", GuideNames.MAX_LENGTH));
			return 0;
		}
		GuideActions.rename(p, name);
		String now = GuideManager.guideName(p);
		c.getSource().sendSuccess(() -> Component.translatable(
				name == null ? "survivalassistant.cmd.name_reset" : "survivalassistant.cmd.name_set", now), false);
		return 1;
	}

	/** /guide chat on|off: small talk through a local language model, stored per player (only when the server allows it). */
	private static int chat(CommandContext<CommandSourceStack> c, boolean enabled) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		if (enabled && !ModConfig.get().chatEnabled) {
			c.getSource().sendFailure(Component.translatable("survivalassistant.cmd.chat_not_allowed"));
			return 0;
		}
		GuideState.of(p).chatEnabled = enabled;
		c.getSource().sendSuccess(() -> Component.translatable(
				enabled ? "survivalassistant.cmd.chat_on" : "survivalassistant.cmd.chat_off"), false);
		return 1;
	}

	private static int character(CommandContext<CommandSourceStack> c, boolean enabled) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		GuideActions.setNpcEnabled(p, enabled);
		c.getSource().sendSuccess(() -> Component.translatable(
				enabled ? "survivalassistant.cmd.character_on" : "survivalassistant.cmd.character_off"), false);
		return 1;
	}

	/** /guide call: only with the guide character on (without it there is nobody to call). */
	private static int call(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		if (!GuideActions.npcEnabled(c.getSource().getPlayerOrException())) {
			c.getSource().sendFailure(Component.translatable("survivalassistant.cmd.npc_off_recall"));
			return 0;
		}
		return run(c, GuideActions::recall);
	}

	/** Runs an action for the player; when it can't be done now, says so. */
	private static int run(CommandContext<CommandSourceStack> c, Predicate<ServerPlayer> action) throws CommandSyntaxException {
		if (!action.test(c.getSource().getPlayerOrException())) {
			c.getSource().sendFailure(Component.translatable("survivalassistant.cmd.unavailable"));
			return 0;
		}
		return 1;
	}
}
