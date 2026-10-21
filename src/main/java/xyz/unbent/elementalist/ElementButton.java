package xyz.unbent.elementalist;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import xyz.unbent.garrison.api.guard.Guard;
import xyz.unbent.garrison.api.menu.MenuButton;

import java.util.List;

/** The element picker in the guard's menu: left-click for the next element, right-click for the previous one. */
final class ElementButton implements MenuButton {

	private final ElementalistBehaviour behaviour;

	ElementButton(ElementalistBehaviour behaviour) {
		this.behaviour = behaviour;
	}

	@Override
	public @NotNull ItemStack getIcon(@NotNull Guard guard, @NotNull Player viewer) {
		Element element = behaviour.elementOf(guard);
		ItemStack icon = new ItemStack(element.icon());
		icon.editMeta(meta -> {
			meta.displayName(plain(Component.text("Element: ", NamedTextColor.GRAY).append(Component.text(element.display(), element.colour()))));
			meta.lore(List.of(
				ability(element.ranged(), element),
				ability(element.close(), element),
				Component.empty(),
				plain(Component.text("Left-click: next element", NamedTextColor.DARK_GRAY)),
				plain(Component.text("Right-click: previous element", NamedTextColor.DARK_GRAY))));
		});
		return icon;
	}

	@Override
	public void onClick(@NotNull Guard guard, @NotNull Player viewer, boolean rightClick) {
		Element current = behaviour.elementOf(guard);
		Element chosen = rightClick ? current.previous() : current.next();
		behaviour.setElement(guard, chosen);
		viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
		viewer.sendMessage(Component.text("This Elementalist now wields ", NamedTextColor.GRAY)
			.append(Component.text(chosen.display(), chosen.colour()))
			.append(Component.text(".", NamedTextColor.GRAY)));
	}

	private static Component ability(Ability ability, Element element) {
		return plain(Component.text(ability.display(), element.colour())
			.append(Component.text(": " + ability.description(), NamedTextColor.GRAY)));
	}

	/** Item names and lore are italic unless told otherwise. */
	private static Component plain(Component component) {
		return component.decoration(TextDecoration.ITALIC, false);
	}
}
