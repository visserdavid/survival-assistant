package io.github.visserdavid.survivalassistant.client.settings;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import static io.github.visserdavid.survivalassistant.client.settings.SettingsScreen.KEY;

/**
 * Builds the rows of a settings tab with the vanilla option buttons. Every option has a label
 * ({@code survivalassistant.settings.<id>}) and a one-line explanation as tooltip ({@code ...<id>.tip}).
 * Options are collected per group and placed two per row.
 */
final class Rows {
	private static final int WIDGET_WIDTH = 150;
	private static final int TEXT = 0xFFE0E0E0, TEXT_INVALID = 0xFFFF5555;

	private final SettingsScreen screen;
	private final OptionsList list;
	private final boolean editable;
	private final Options options = Minecraft.getInstance().options;
	private final List<AbstractWidget> pending = new ArrayList<>();

	Rows(SettingsScreen screen, OptionsList list, boolean editable) {
		this.screen = screen;
		this.list = list;
		this.editable = editable;
	}

	/** A centred line of text over the whole width (an explanation, or why a tab is empty). */
	static void notice(OptionsList list, Component text) {
		list.addHeader(text);
	}

	static Component label(String id) {
		return Component.translatable(KEY + id);
	}

	/** "16 blocks" in the player's language. */
	static Component blocks(int count) {
		return Component.translatable(KEY + "unit.blocks", count);
	}

	static Tooltip tip(String id) {
		return Tooltip.create(Component.translatable(KEY + id + ".tip"));
	}

	/** A heading above a group; places the options of the group before it first. */
	Rows header(String id) {
		flush();
		list.addHeader(Component.translatable(KEY + "group." + id).withStyle(ChatFormatting.YELLOW));
		return this;
	}

	Rows toggle(String id, boolean value, Consumer<Boolean> set) {
		return add(OptionInstance.createBoolean(KEY + id, v -> tip(id), value, set::accept).createButton(options));
	}

	<T> Rows cycle(String id, List<T> values, T value, Function<T, Component> name, Consumer<T> set) {
		// a cycle button puts the caption in front by itself, so only the value here
		OptionInstance<T> option = new OptionInstance<>(KEY + id, v -> tip(id), (caption, v) -> name.apply(v),
				new OptionInstance.Enum<>(values, Codec.INT.xmap(values::get, values::indexOf)),
				values.contains(value) ? value : values.getFirst(), set::accept);
		return add(option.createButton(options));
	}

	/**
	 * A slider from {@code min} to {@code max}. It only writes when the player moves it, so a value in the file that
	 * lies outside the slider's range stays as it is.
	 */
	Rows slider(String id, int min, int max, int value, IntFunction<Component> name, IntConsumer set) {
		OptionInstance<Integer> option = new OptionInstance<>(KEY + id, v -> tip(id),
				(caption, v) -> Options.genericValueLabel(caption, name.apply(v)),
				new OptionInstance.IntRange(min, max), Math.clamp(value, min, max), set::accept);
		return add(option.createButton(options));
	}

	/** A label with a text field next to it; an empty field shows {@code hint} (the value that applies then). */
	Rows text(String id, String value, String hint, int maxLength, Predicate<String> valid, Consumer<String> set) {
		flush();
		var font = Minecraft.getInstance().font;
		Label label = new Label(label(id));
		label.setTooltip(tip(id));
		EditBox box = new EditBox(font, WIDGET_WIDTH, 20, label(id));
		box.setMaxLength(maxLength);
		box.setValue(value);
		box.setHint(Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY));
		box.setTooltip(tip(id));
		box.setEditable(editable);
		box.active = editable;
		box.setResponder(s -> {
			boolean ok = valid.test(s);
			box.setTextColor(ok ? TEXT : TEXT_INVALID);
			screen.setInvalid(id, !ok);
			if (ok) set.accept(s);
		});
		list.addSmall(label, box);
		return this;
	}

	/** Greys out the option added last and explains why in its tooltip. */
	Rows lockLast(Component reason) {
		AbstractWidget last = pending.getLast();
		last.active = false;
		last.setTooltip(Tooltip.create(reason));
		return this;
	}

	/** A button over the whole width (opens something; always usable). */
	Rows wideButton(AbstractWidget button) {
		flush();
		list.addBig(button);
		return this;
	}

	/** A button in the two-per-row grid. */
	Rows button(AbstractWidget button) {
		pending.add(button);
		return this;
	}

	private Rows add(AbstractWidget widget) {
		widget.active = editable;
		pending.add(widget);
		return this;
	}

	/** Places the collected options, two per row. Call at the end of a tab. */
	void flush() {
		if (pending.isEmpty()) return;
		list.addSmall(List.copyOf(pending));
		pending.clear();
	}

	/** The label in front of a text field: plain text, vertically centred on the field next to it. */
	private static final class Label extends AbstractWidget {
		Label(Component text) {
			super(0, 0, WIDGET_WIDTH, 20, text);
			active = false; // not clickable and skipped by Tab
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			var font = Minecraft.getInstance().font;
			graphics.text(font, getMessage(), getX(), getY() + (getHeight() - font.lineHeight) / 2 + 1, 0xFFFFFFFF, true);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, getMessage());
		}
	}
}
