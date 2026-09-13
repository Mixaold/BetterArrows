package net.betterarrows.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Hands Mod Menu the settings screen.
 *
 * <p>Mod Menu is a compile-time-only dependency: this class is referenced from nothing but the
 * {@code modmenu} entrypoint, which only Mod Menu itself ever asks for. Without Mod Menu installed
 * the class is never loaded, so its missing imports cost nothing and the mod runs perfectly well -
 * you just configure it by file instead.
 */
public class BetterArrowsModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return BetterArrowsConfigScreen::create;
	}
}
