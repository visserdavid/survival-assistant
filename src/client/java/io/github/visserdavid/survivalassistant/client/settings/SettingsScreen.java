package io.github.visserdavid.survivalassistant.client.settings;

import io.github.visserdavid.survivalassistant.advice.GuideChoices;
import io.github.visserdavid.survivalassistant.client.hud.ClientConfig;
import io.github.visserdavid.survivalassistant.config.JsonFiles;
import io.github.visserdavid.survivalassistant.config.ModConfig;
import io.github.visserdavid.survivalassistant.state.SettingsPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.MenuTabBar;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The settings screen: three tabs (Display, My guide, World) with the vanilla option buttons. All changes go into
 * drafts; "Done" saves them, "Cancel" (or Esc) throws them away after asking.
 * <p>
 * Display is stored on this computer. My guide and World live on the server: the screen asks for them when it opens
 * and sends the changes on "Done". Without a world, World edits the local config file directly.
 */
public final class SettingsScreen extends OptionsSubScreen {
	static final String KEY = "survivalassistant.settings.";
	private static final int TAB_DISPLAY = 0, TAB_GUIDE = 1, TAB_WORLD = 2;

	/**
	 * The screen waiting for the server's answer. Kept here (not looked up as the open screen) so the answer also
	 * arrives while a screen on top of it is open, such as the key bindings or the "discard changes?" question.
	 */
	private static @Nullable SettingsScreen waiting;

	/** Where the guide and world settings come from. */
	enum Source {
		/** No world open: the world settings are the local file, there is no player guide. */
		LOCAL,
		/** Asked the server, no answer yet. */
		WAITING,
		/** The server answered. */
		READY,
		/** The server doesn't have this mod. */
		MISSING
	}

	private ClientConfig display;
	private final String displayBefore;
	private @Nullable ModConfig world;
	private String worldBefore = "";
	private final ChoicesDraft choices = new ChoicesDraft();
	private @Nullable GuideChoices choicesBefore;
	private String serverName = "";
	private boolean canEditWorld;
	/** The server allows small talk (else the player's switch is greyed out). */
	private boolean chatAllowed;
	private Source source;
	private boolean advancedOpen;
	/** The server sent a config this screen couldn't read. */
	private boolean worldUnreadable;
	private int tabIndex;
	/** Text fields with an invalid value (field id -> its tab): "Done" waits until they are fixed. */
	private final Map<String, Integer> invalid = new LinkedHashMap<>();
	/** The tab whose rows are being built right now, or -1. */
	private int building = -1;

	private final List<SettingsTab> tabs = new ArrayList<>();
	private final TabManager tabManager = new TabManager(this::addRenderableWidget, this::removeWidget,
			tab -> onTabSelected(tabs.indexOf(tab)), tab -> {});
	private @Nullable MenuTabBar tabBar;
	private @Nullable Button resetButton;
	private @Nullable Button doneButton;

	public SettingsScreen(@Nullable Screen parent) {
		super(parent, Minecraft.getInstance().options, Component.translatable(KEY + "title"));
		display = ClientConfig.get().copy();
		displayBefore = JsonFiles.toJson(display);
		Minecraft mc = Minecraft.getInstance();
		if (mc.getConnection() == null) {
			source = Source.LOCAL;
			setWorld(ModConfig.get().copy(), true);
		} else if (ClientPlayNetworking.canSend(SettingsPayloads.Request.TYPE)) {
			source = Source.WAITING;
			waiting = this;
			ClientPlayNetworking.send(new SettingsPayloads.Request());
		} else {
			source = Source.MISSING;
		}
	}

	private void setWorld(ModConfig cfg, boolean editable) {
		world = cfg;
		worldBefore = cfg.toJson();
		canEditWorld = editable;
		serverName = cfg.guideName;
		chatAllowed = cfg.chatEnabled;
	}

	/** The server's answer, for the screen that asked for it (if it is still open). */
	public static void deliver(SettingsPayloads.Sync sync) {
		SettingsScreen screen = waiting;
		waiting = null;
		if (screen != null) screen.receive(sync);
	}

	/** The server's answer: fills the My guide and World tabs. */
	private void receive(SettingsPayloads.Sync sync) {
		if (source != Source.WAITING) return;
		world = null;
		if (sync.canEdit()) {
			try {
				setWorld(ModConfig.fromJson(sync.configJson()), true);
			} catch (RuntimeException e) {
				worldUnreadable = true;
			}
		}
		chatAllowed = sync.chatAllowed();
		serverName = sync.serverName();
		choices.set(sync.choices());
		choicesBefore = sync.choices();
		source = Source.READY;
		// when another screen is on top, init() builds the tabs again on return
		if (minecraft != null && minecraft.gui.screen() == this) {
			for (SettingsTab tab : tabs) tab.rebuild();
			updateButtons();
		}
	}

	// ---------------------------------------------------------------- layout

	@Override
	protected void init() {
		// init() runs again after a screen on top closes (key bindings, the discard question): start from scratch
		layout.removeChildren();
		tabs.clear();
		invalid.clear();
		tabs.add(new SettingsTab(KEY + "tab.display", l -> new DisplayTab(this, display).build(l)));
		tabs.add(new SettingsTab(KEY + "tab.guide", this::buildGuide));
		tabs.add(new SettingsTab(KEY + "tab.world", this::buildWorld));
		tabBar = MenuTabBar.builder(tabManager, width).addTabs(tabs.toArray(Tab[]::new)).build();
		addRenderableWidget(tabBar);

		LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));
		resetButton = footer.addChild(Button.builder(Component.translatable(KEY + "reset"), b -> resetTab())
				.width(100).tooltip(Tooltip.create(Component.translatable(KEY + "reset.tip"))).build());
		footer.addChild(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose()).width(100).build());
		doneButton = footer.addChild(Button.builder(CommonComponents.GUI_DONE, b -> saveAndClose()).width(100).build());
		layout.visitWidgets(this::addRenderableWidget);

		tabBar.selectTab(Math.clamp(tabIndex, 0, tabs.size() - 1), false);
		repositionElements();
		updateButtons();
	}

	@Override
	protected void addOptions() {
		// the tabs build their own lists (see init)
	}

	@Override
	protected void repositionElements() {
		if (tabBar == null) return;
		tabBar.arrangeElements(width);
		int top = tabBar.getRectangle().bottom();
		layout.setHeaderHeight(top);
		layout.arrangeElements();
		tabManager.setTabArea(new ScreenRectangle(0, top, width, height - layout.getFooterHeight() - top));
	}

	private void onTabSelected(int index) {
		if (index >= 0) tabIndex = index;
		updateButtons();
	}

	/** Rebuilds the list of the current tab (after "reset" or opening the advanced options). */
	void refreshTab() {
		if (tabIndex < tabs.size()) tabs.get(tabIndex).rebuild();
		updateButtons();
	}

	private void updateButtons() {
		if (resetButton != null) resetButton.active = tabEditable(tabIndex);
		if (doneButton != null) {
			doneButton.active = invalid.isEmpty();
			doneButton.setTooltip(invalid.isEmpty() ? null : Tooltip.create(Component.translatable(KEY + "fix_first", fieldNames())));
		}
	}

	/** The labels of the fields that still need fixing, for the tooltip of "Done". */
	private Component fieldNames() {
		MutableComponent names = Component.empty();
		for (String id : invalid.keySet()) {
			if (!names.getSiblings().isEmpty()) names.append(", ");
			names.append(Rows.label(id));
		}
		return names;
	}

	private boolean tabEditable(int tab) {
		return switch (tab) {
			case TAB_GUIDE -> source == Source.READY;
			case TAB_WORLD -> world != null && canEditWorld;
			default -> true;
		};
	}

	/** Marks a text field as valid or invalid; "Done" is only possible when all are valid. */
	void setInvalid(String field, boolean isInvalid) {
		invalid.remove(field);
		if (isInvalid) invalid.put(field, building >= 0 ? building : tabIndex);
		updateButtons();
	}

	// ---------------------------------------------------------------- tabs

	private void buildGuide(OptionsList list) {
		Component reason = switch (source) {
			case LOCAL -> Component.translatable(KEY + "guide.no_world");
			case WAITING -> Component.translatable(KEY + "loading");
			case MISSING -> Component.translatable(KEY + "no_server_mod");
			case READY -> null;
		};
		if (reason != null) {
			Rows.notice(list, reason);
			return;
		}
		new GuideTab(this, choices, serverName, chatAllowed).build(list);
	}

	private void buildWorld(OptionsList list) {
		if (world == null) {
			String reason = switch (source) {
				case MISSING -> "no_server_mod";
				case WAITING -> "loading";
				default -> worldUnreadable ? "world.unavailable" : "world.read_only";
			};
			Rows.notice(list, Component.translatable(KEY + reason).withStyle(ChatFormatting.YELLOW));
			return;
		}
		if (source == Source.LOCAL) Rows.notice(list, Component.translatable(KEY + "world.local").withStyle(ChatFormatting.GRAY));
		new WorldTab(this, world, canEditWorld).build(list);
	}

	boolean advancedOpen() {
		return advancedOpen;
	}

	void toggleAdvanced() {
		advancedOpen = !advancedOpen;
		refreshTab();
	}

	private void resetTab() {
		if (!tabEditable(tabIndex)) return;
		switch (tabIndex) {
			case TAB_DISPLAY -> {
				ClientConfig fresh = new ClientConfig();
				fresh.mode = display.mode; // the hotkeys switch these anyway; "reset" shouldn't hide the card
				fresh.showEnvironment = display.showEnvironment;
				display = fresh;
			}
			case TAB_GUIDE -> {
				ModConfig defaults = world != null ? world : new ModConfig();
				choices.set(new GuideChoices(defaults.npcEnabledByDefault, "", GuideChoices.MUSINGS_ON, chatAllowed));
			}
			case TAB_WORLD -> world = new ModConfig();
			default -> {}
		}
		refreshTab(); // also forgets the invalid fields of this tab only
	}

	// ---------------------------------------------------------------- closing

	private boolean displayChanged() {
		return !JsonFiles.toJson(display).equals(displayBefore);
	}

	private boolean choicesChanged() {
		return source == Source.READY && !Objects.equals(choices.toRecord(), choicesBefore);
	}

	private boolean worldChanged() {
		return world != null && canEditWorld && !world.toJson().equals(worldBefore);
	}

	private void applyListChanges() {
		for (SettingsTab tab : tabs) tab.list.applyUnsavedChanges();
	}

	private void saveAndClose() {
		applyListChanges();
		if (!invalid.isEmpty()) return;
		if (displayChanged()) ClientConfig.set(display);
		if (choicesChanged()) ClientPlayNetworking.send(new SettingsPayloads.ChoicesUpdate(choices.toRecord()));
		if (worldChanged()) {
			if (source == Source.LOCAL) ModConfig.update(world);
			else ClientPlayNetworking.send(new SettingsPayloads.ConfigUpdate(world.toJson()));
		}
		close();
	}

	/** Esc and "Cancel": throw the changes away, after asking when there are any. */
	@Override
	public void onClose() {
		applyListChanges();
		if (!displayChanged() && !choicesChanged() && !worldChanged()) {
			close();
			return;
		}
		minecraft.gui.setScreen(new ConfirmScreen(discard -> {
			if (discard) close();
			else minecraft.gui.setScreen(this);
		}, Component.translatable(KEY + "discard.title"), Component.translatable(KEY + "discard.message"),
				Component.translatable(KEY + "discard.yes"), Component.translatable(KEY + "discard.no")));
	}

	private void close() {
		if (waiting == this) waiting = null;
		minecraft.gui.setScreen(lastScreen);
	}

	// ---------------------------------------------------------------- pieces

	/** One tab: a scrolling list of options, built again when needed. */
	private final class SettingsTab implements Tab {
		private final int index;
		private final Component title;
		private final Consumer<OptionsList> builder;
		private final FrameLayout layout = new FrameLayout();
		private OptionsList list;
		private @Nullable ScreenRectangle area;

		SettingsTab(String titleKey, Consumer<OptionsList> builder) {
			this.index = tabs.size();
			this.title = Component.translatable(titleKey);
			this.builder = builder;
			this.list = newList();
		}

		/** Builds the rows; the fields report whether they are valid as they are made. */
		private OptionsList newList() {
			invalid.values().removeIf(tab -> tab == index);
			OptionsList l = new OptionsList(Minecraft.getInstance(), width, SettingsScreen.this);
			building = index;
			try {
				builder.accept(l);
			} finally {
				building = -1;
			}
			return l;
		}

		void rebuild() {
			double scroll = list.scrollAmount();
			boolean shown = tabManager.getCurrentTab() == this;
			if (shown) removeWidget(list);
			list = newList();
			if (shown) addRenderableWidget(list);
			if (area != null) doLayout(area);
			list.setScrollAmount(scroll);
		}

		@Override
		public Component getTabTitle() {
			return title;
		}

		@Override
		public Component getTabExtraNarration() {
			return Component.empty();
		}

		@Override
		public void visitChildren(Consumer<net.minecraft.client.gui.components.AbstractWidget> consumer) {
			consumer.accept(list);
		}

		@Override
		public void doLayout(ScreenRectangle rect) {
			area = rect;
			list.updateSizeAndPosition(rect.width(), rect.height(), rect.left(), rect.top());
		}

		@Override
		public Layout getLayout() {
			return layout;
		}
	}

	/** The editable copy of the player's guide choices. */
	static final class ChoicesDraft {
		boolean character = true;
		String name = "";
		String musings = GuideChoices.MUSINGS_ON;
		boolean chat;

		void set(GuideChoices c) {
			character = c.character();
			name = c.name();
			musings = c.musings();
			chat = c.chat();
		}

		GuideChoices toRecord() {
			return new GuideChoices(character, name, musings, chat);
		}
	}
}
