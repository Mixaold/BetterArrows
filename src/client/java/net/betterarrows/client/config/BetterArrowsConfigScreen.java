package net.betterarrows.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.betterarrows.config.BetterArrowsConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The settings screen, reached from Mod Menu. Editing a JSON file by hand is a poor way to offer
 * this many toggles, and most of them are matters of taste that people will want to try and undo.
 *
 * <p>Values are written straight back into the live config object, so a change takes effect the
 * moment it is saved - with one honest exception noted on the ricochet entries: on a server it is
 * the SERVER's config that decides gameplay, and editing it here only changes your own copy.
 */
public final class BetterArrowsConfigScreen {
	private BetterArrowsConfigScreen() {
	}

	public static Screen create(Screen parent) {
		BetterArrowsConfig config = BetterArrowsConfig.get();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(title("title"))
				.setSavingRunnable(BetterArrowsConfig::save);
		ConfigEntryBuilder entry = builder.entryBuilder();

		ConfigCategory arrows = builder.getOrCreateCategory(title("category.arrows"));
		arrows.addEntry(entry.startBooleanToggle(title("stuckArrows"), config.stuckArrows.enabled)
				.setDefaultValue(true)
				.setTooltip(tooltip("stuckArrows"))
				.setSaveConsumer(v -> config.stuckArrows.enabled = v)
				.build());
		arrows.addEntry(entry.startBooleanToggle(title("stickThroughInvulnerability"), config.stickThroughInvulnerability.enabled)
				.setDefaultValue(false)
				.setTooltip(tooltip("stickThroughInvulnerability"))
				.setSaveConsumer(v -> config.stickThroughInvulnerability.enabled = v)
				.build());

		ConfigCategory impact = builder.getOrCreateCategory(title("category.impact"));
		impact.addEntry(entry.startBooleanToggle(title("surfaceSounds"), config.surfaceSounds.enabled)
				.setDefaultValue(true)
				.setTooltip(tooltip("surfaceSounds"))
				.setSaveConsumer(v -> config.surfaceSounds.enabled = v)
				.build());
		impact.addEntry(entry.startIntSlider(title("surfaceSoundsVolume"), percent(config.surfaceSounds.volume), 0, 100)
				.setDefaultValue(50)
				.setTooltip(tooltip("surfaceSoundsVolume"))
				.setSaveConsumer(v -> config.surfaceSounds.volume = v / 100.0F)
				.build());
		impact.addEntry(entry.startBooleanToggle(title("blockParticles"), config.impactParticles.block.enabled)
				.setDefaultValue(true)
				.setTooltip(tooltip("blockParticles"))
				.setSaveConsumer(v -> config.impactParticles.block.enabled = v)
				.build());
		impact.addEntry(entry.startIntSlider(title("blockParticlesCount"), config.impactParticles.block.count, 0, 32)
				.setDefaultValue(6)
				.setSaveConsumer(v -> config.impactParticles.block.count = v)
				.build());

		// Blood and other per-mob wound particles moved to the standalone "Simple Mob Particles"
		// mod - install it alongside this one for that half of the look, configured from its own
		// settings screen.

		ConfigCategory ricochet = builder.getOrCreateCategory(title("category.ricochet"));
		ricochet.addEntry(entry.startBooleanToggle(title("ricochet"), config.ricochet.enabled)
				.setDefaultValue(true)
				.setTooltip(tooltip("ricochet"))
				.setSaveConsumer(v -> config.ricochet.enabled = v)
				.build());
		ricochet.addEntry(entry.startIntSlider(title("ricochetAngle"), Math.round(config.ricochet.maxGlancingAngleDegrees), 0, 89)
				.setDefaultValue(35)
				.setTooltip(tooltip("ricochetAngle"))
				.setSaveConsumer(v -> config.ricochet.maxGlancingAngleDegrees = v)
				.build());
		ricochet.addEntry(entry.startIntSlider(title("ricochetEnergy"), percent(config.ricochet.energyRetained), 5, 100)
				.setDefaultValue(35)
				.setTooltip(tooltip("ricochetEnergy"))
				.setSaveConsumer(v -> config.ricochet.energyRetained = v / 100.0F)
				.build());
		ricochet.addEntry(entry.startIntSlider(title("ricochetMaxBounces"), config.ricochet.maxBounces, 0, 8)
				.setDefaultValue(2)
				.setSaveConsumer(v -> config.ricochet.maxBounces = v)
				.build());

		arrows.addEntry(entry.startBooleanToggle(title("debugLogging"), config.debugLogging)
				.setDefaultValue(false)
				.setTooltip(tooltip("debugLogging"))
				.setSaveConsumer(v -> config.debugLogging = v)
				.build());

		ricochet.addEntry(entry.startBooleanToggle(title("iceSlide"), config.iceSlide.enabled)
				.setDefaultValue(true)
				.setTooltip(tooltip("iceSlide"))
				.setSaveConsumer(v -> config.iceSlide.enabled = v)
				.build());
		ricochet.addEntry(entry.startIntSlider(title("iceSlideAngle"),
						Math.round(config.iceSlide.maxAngleDegrees), 0, 89)
				.setDefaultValue(40)
				.setTooltip(tooltip("iceSlideAngle"))
				.setSaveConsumer(v -> config.iceSlide.maxAngleDegrees = v)
				.build());
		ricochet.addEntry(entry.startIntSlider(title("iceSlideKept"),
						percent(config.iceSlide.speedKept), 10, 99)
				.setDefaultValue(85)
				.setTooltip(tooltip("iceSlideKept"))
				.setSaveConsumer(v -> config.iceSlide.speedKept = v / 100.0F)
				.build());

		ricochet.addEntry(entry.startIntSlider(title("iceSlideSettle"),
						percent(config.iceSlide.normalKept), 0, 90)
				.setDefaultValue(25)
				.setTooltip(tooltip("iceSlideSettle"))
				.setSaveConsumer(v -> config.iceSlide.normalKept = v / 100.0F)
				.build());
		ricochet.addEntry(entry.startIntSlider(title("iceSlideMax"), config.iceSlide.maxSlides, 1, 200)
				.setDefaultValue(40)
				.setTooltip(tooltip("iceSlideMax"))
				.setSaveConsumer(v -> config.iceSlide.maxSlides = v)
				.build());

		ricochet.addEntry(entry.startIntSlider(title("iceBounce"),
						percent(config.iceSlide.hardBounceKept), 5, 90)
				.setDefaultValue(30)
				.setTooltip(tooltip("iceBounce"))
				.setSaveConsumer(v -> config.iceSlide.hardBounceKept = v / 100.0F)
				.build());

		return builder.build();
	}

	/** Fractions are shown as whole percentages: a 0..1 slider reads as noise to most people. */
	private static int percent(float fraction) {
		return Math.round(fraction * 100.0F);
	}

	private static Component title(String key) {
		return Component.translatable("betterarrows.config." + key);
	}

	private static Component[] tooltip(String key) {
		return new Component[] { Component.translatable("betterarrows.config." + key + ".tooltip") };
	}
}
