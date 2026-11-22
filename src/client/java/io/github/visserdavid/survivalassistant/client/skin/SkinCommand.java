package io.github.visserdavid.survivalassistant.client.skin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import io.github.visserdavid.survivalassistant.client.hud.HudConfig;
import io.github.visserdavid.survivalassistant.skin.SkinNames;

import java.util.ArrayList;
import java.util.List;

/** /guideskin list | set &lt;name&gt; | reload | folder (client side: only changes what you see). */
public final class SkinCommand {
	private SkinCommand() {}

	private static final String P = "survivalassistant.skin.";

	public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("guideskin")
				.executes(SkinCommand::list)
				.then(ClientCommands.literal("list").executes(SkinCommand::list))
				.then(ClientCommands.literal("reload").executes(SkinCommand::reload))
				.then(ClientCommands.literal("folder").executes(SkinCommand::folder))
				.then(ClientCommands.literal("set")
						.then(ClientCommands.argument("name", StringArgumentType.word())
								.suggests((c, b) -> {
									List<String> options = new ArrayList<>(GuideSkins.names());
									options.add(0, SkinNames.DEFAULT);
									return SharedSuggestionProvider.suggest(options, b);
								})
								.executes(SkinCommand::set))));
	}

	private static int list(CommandContext<FabricClientCommandSource> c) {
		List<String> names = GuideSkins.names();
		String active = HudConfig.get().guideSkin;
		c.getSource().sendFeedback(Component.translatable(P + "list_header", names.size()).withStyle(ChatFormatting.GOLD));
		c.getSource().sendFeedback(entry(SkinNames.DEFAULT, active));
		for (String name : names) c.getSource().sendFeedback(entry(name, active));
		if (names.isEmpty()) c.getSource().sendFeedback(Component.translatable(P + "empty").withStyle(ChatFormatting.GRAY));
		reportProblems(c);
		c.getSource().sendFeedback(folderLink());
		return 1;
	}

	private static Component entry(String name, String active) {
		boolean on = name.equals(active) || (active == null && name.equals(SkinNames.DEFAULT));
		return Component.literal((on ? " > " : "   ") + name)
				.withStyle(Style.EMPTY.withColor(on ? ChatFormatting.GREEN : ChatFormatting.WHITE)
						.withClickEvent(new ClickEvent.RunCommand("/guideskin set " + name))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatable(P + "click_to_use"))));
	}

	private static int set(CommandContext<FabricClientCommandSource> c) {
		String name = StringArgumentType.getString(c, "name").toLowerCase(java.util.Locale.ROOT);
		if (!name.equals(SkinNames.DEFAULT) && !GuideSkins.names().contains(name)) {
			c.getSource().sendError(Component.translatable(P + "not_found", name));
			return 0;
		}
		HudConfig.get().guideSkin = name;
		HudConfig.save();
		c.getSource().sendFeedback(Component.translatable(P + "set", name).withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int reload(CommandContext<FabricClientCommandSource> c) {
		GuideSkins.reload();
		c.getSource().sendFeedback(Component.translatable(P + "reloaded", GuideSkins.names().size()).withStyle(ChatFormatting.GREEN));
		reportProblems(c);
		if (GuideSkins.chosenMissing()) {
			c.getSource().sendError(Component.translatable(P + "fallback", HudConfig.get().guideSkin));
		}
		return 1;
	}

	private static int folder(CommandContext<FabricClientCommandSource> c) {
		c.getSource().sendFeedback(folderLink());
		return 1;
	}

	private static void reportProblems(CommandContext<FabricClientCommandSource> c) {
		for (String p : GuideSkins.problems()) {
			c.getSource().sendError(Component.translatable(P + "bad_file", p));
		}
	}

	/** Clickable path that opens the skins folder. */
	private static Component folderLink() {
		String path = GuideSkins.folder().toAbsolutePath().toString();
		return Component.translatable(P + "folder", Component.literal(path)
				.withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true)
						.withClickEvent(new ClickEvent.OpenFile(GuideSkins.folder().toAbsolutePath()))
						.withHoverEvent(new HoverEvent.ShowText(Component.translatable(P + "open_folder")))));
	}
}
