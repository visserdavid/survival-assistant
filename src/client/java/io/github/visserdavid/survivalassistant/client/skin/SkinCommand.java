package io.github.visserdavid.survivalassistant.client.skin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.visserdavid.survivalassistant.client.hud.ClientConfig;
import io.github.visserdavid.survivalassistant.skin.SkinNames;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Locale;

/** /guideskin list | set &lt;name&gt; | reload | folder (client side: only changes what you see). */
public final class SkinCommand {
	private SkinCommand() {}

	private static final String KEY_PREFIX = "survivalassistant.skin.";

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("guideskin")
				.executes(SkinCommand::list)
				.then(ClientCommands.literal("list").executes(SkinCommand::list))
				.then(ClientCommands.literal("reload").executes(SkinCommand::reload))
				.then(ClientCommands.literal("folder").executes(SkinCommand::folder))
				.then(ClientCommands.literal("set")
						.then(ClientCommands.argument("name", StringArgumentType.word())
								.suggests((c, b) -> SharedSuggestionProvider.suggest(GuideSkins.choices(), b))
								.executes(SkinCommand::set))));
	}

	private static int list(CommandContext<FabricClientCommandSource> c) {
		List<String> names = GuideSkins.names();
		String active = ClientConfig.get().guideSkin;
		c.getSource().sendFeedback(Component.translatable(KEY_PREFIX + "list_header", names.size()).withStyle(ChatFormatting.GOLD));
		for (String name : GuideSkins.choices()) c.getSource().sendFeedback(entry(name, active));
		if (names.isEmpty()) c.getSource().sendFeedback(Component.translatable(KEY_PREFIX + "empty").withStyle(ChatFormatting.GRAY));
		reportProblems(c);
		c.getSource().sendFeedback(folderLink());
		return 1;
	}

	private static Component entry(String name, String active) {
		boolean on = name.equals(active) || (active == null && name.equals(SkinNames.DEFAULT));
		return Component.literal((on ? " > " : "   ") + name)
				.withStyle(Style.EMPTY.withColor(on ? ChatFormatting.GREEN : ChatFormatting.WHITE)
						.withClickEvent(new ClickEvent.RunCommand("/guideskin set " + name))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatable(KEY_PREFIX + "click_to_use"))));
	}

	private static int set(CommandContext<FabricClientCommandSource> c) {
		String name = StringArgumentType.getString(c, "name").toLowerCase(Locale.ROOT);
		if (!GuideSkins.choices().contains(name)) {
			c.getSource().sendError(Component.translatable(KEY_PREFIX + "not_found", name));
			return 0;
		}
		ClientConfig.get().guideSkin = name;
		ClientConfig.save();
		c.getSource().sendFeedback(Component.translatable(KEY_PREFIX + "set", name).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int reload(CommandContext<FabricClientCommandSource> c) {
		GuideSkins.reload();
		c.getSource().sendFeedback(Component.translatable(KEY_PREFIX + "reloaded", GuideSkins.names().size()).withStyle(ChatFormatting.GREEN));
		reportProblems(c);
		if (GuideSkins.chosenMissing()) {
			c.getSource().sendError(Component.translatable(KEY_PREFIX + "fallback", ClientConfig.get().guideSkin));
		}
		return 1;
	}

	private static int folder(CommandContext<FabricClientCommandSource> c) {
		c.getSource().sendFeedback(folderLink());
		return 1;
	}

	private static void reportProblems(CommandContext<FabricClientCommandSource> c) {
		for (String p : GuideSkins.problems()) {
			c.getSource().sendError(Component.translatable(KEY_PREFIX + "bad_file", p));
		}
	}

	/** Clickable path that opens the skins folder. */
	private static Component folderLink() {
		String path = GuideSkins.folder().toAbsolutePath().toString();
		return Component.translatable(KEY_PREFIX + "folder", Component.literal(path)
				.withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true)
						.withClickEvent(new ClickEvent.OpenFile(GuideSkins.folder().toAbsolutePath()))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatable(KEY_PREFIX + "open_folder")))));
	}
}
