package net.betterarrows.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.betterarrows.BetterArrowsMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Every feature this mod adds is toggleable, because they fall into two very different groups: the
 * original stuck-arrow rendering is purely cosmetic, while ricochet changes how the game plays and
 * has to be agreed on by the server. People who installed this for the cosmetics alone must be able
 * to keep only that.
 *
 * <p>Read once at startup and never reloaded: these values are consulted per arrow hit and per
 * render frame, so re-reading a file there is out of the question, and a mid-session change to
 * ricochet on a live server would desync players anyway.
 *
 * <p>Which side reads what: gameplay fields (ricochet, and the sounds/particles this mod emits,
 * which are all spawned server-side so every nearby player gets them) are read by the SERVER;
 * render-only fields are read by the CLIENT. In singleplayer both come from the same file. On a
 * multiplayer server the server's file decides gameplay and each client's file decides rendering.
 */
public final class BetterArrowsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = BetterArrowsMod.MOD_ID + ".json";

	private static BetterArrowsConfig instance = new BetterArrowsConfig();

	public StuckArrows stuckArrows = new StuckArrows();
	public SurfaceSounds surfaceSounds = new SurfaceSounds();
	public ImpactParticles impactParticles = new ImpactParticles();
	public Ricochet ricochet = new Ricochet();
	public IceSlide iceSlide = new IceSlide();
	public StickThroughInvulnerability stickThroughInvulnerability = new StickThroughInvulnerability();

	/**
	 * Prints, per arrow, which model part it resolved onto and how far the hit landed from it.
	 *
	 * <p>Off by default and deliberately kept in the shipped mod. Where an arrow ends up is the one
	 * thing here that cannot be judged from the outside - the numbers are in a bone's own local
	 * space - and an unresolved arrow is INVISIBLE, so "no arrow appeared" and "the arrow appeared
	 * in the wrong place" look identical from the game and are completely different bugs. One run
	 * with this on separates them; a previous session lost several rounds to guessing between them.
	 */
	public boolean debugLogging = false;

	public static BetterArrowsConfig get() {
		return instance;
	}

	/** The original feature: arrows drawn at their real hit point, following the animated bone. */
	public static final class StuckArrows {
		public boolean enabled = true;
	}

	/** Replaces the one generic arrow "thunk" with the sound of the material actually hit. */
	public static final class SurfaceSounds {
		public boolean enabled = true;
		/**
		 * Below vanilla's 1.0 so the sound reads as a detail you notice when you are next to it
		 * rather than an event announced across the area. Note this does not shrink the 16-block
		 * radius Minecraft uses for the sound - the engine takes {@code max(volume, 1.0)} for the
		 * attenuation distance - it makes the sound quieter over that whole radius, which at this
		 * level amounts to the same thing in practice.
		 *
		 * <p>The sound played is the material's STEP sound, not its "hit" sound: the latter is the
		 * near-silent tick of mining progress, which at any sensible volume is simply inaudible.
		 */
		public float volume = 0.5F;
	}

	/**
	 * Crumble particles carrying the real texture of the block an arrow hit. Unrelated to wounds
	 * on living entities - that side of things (blood and other per-mob effects, on arrows,
	 * melee, and any other damage) is the standalone "Simple Mob Particles" mod's job now.
	 */
	public static final class ImpactParticles {
		public Block block = new Block();

		public static final class Block {
			public boolean enabled = true;
			public int count = 6;
		}
	}

	/**
	 * Arrows skate across ice instead of springing off it. Gameplay: needs the mod on the server.
	 *
	 * <p>Vanilla has no notion of this, and neither did the ricochet below: ice reports its sound
	 * type as GLASS, GLASS is listed as hard and ringing, so a shallow shot at a frozen lake pinged
	 * off it like a window. What it should do is keep going along the surface.
	 */
	public static final class IceSlide {
		public boolean enabled = true;

		/**
		 * Measured from the SURFACE, matching the ricochet setting below: 0 is sliding flat along
		 * the ice, 90 is straight down into it. Shots steeper than this bite in and stick, because
		 * ice is hard and a square-on arrow should embed rather than skate.
		 */
		public float maxAngleDegrees = 40.0F;

		/**
		 * Speed kept on each contact with the ice. High on purpose - that is what slippery means -
		 * but below 1, or an arrow would slide until it left the world.
		 */
		public float speedKept = 0.85F;

		/**
		 * How much of the speed INTO the ice survives each contact, instead of being cancelled.
		 *
		 * <p>Zero looks like the obviously right answer - a slide is exactly "no bounce" - and it is
		 * what this did at first. It is wrong at the EDGE: cancelling the whole normal component
		 * means the arrow leaves the last block of ice with a vertical speed of precisely nothing,
		 * and an arrow with no downward speed at all takes six ticks to fall its first block. At two
		 * blocks a tick that is twelve blocks of dead flat gliding, which over water looks like the
		 * arrow forgot about gravity. Keeping a quarter lets it settle onto the ice and leave the
		 * edge already falling.
		 */
		public float normalKept = 0.25F;

		/**
		 * Hard cap on how many times one arrow may graze ice before it simply sticks.
		 *
		 * <p>{@code speedKept} should stop it long before this - a backstop, the same role
		 * {@code maxBounces} plays for the ricochet, so that no combination of settings or geometry
		 * can produce an arrow skating across a frozen ocean forever.
		 */
		public int maxSlides = 40;

		/**
		 * Speed kept when an arrow meets ice too steeply to skate and is thrown off it instead.
		 *
		 * <p>An arrow cannot embed itself in ice. Driving a shaft into a frozen lake takes a great
		 * deal more than a bowstring, and an arrow standing upright in ice like a dart in a board is
		 * the single most obviously wrong thing this mod could draw. So a shot that is too steep to
		 * slide is bounced instead, weakly, and bounced again if it still has anything left - until
		 * it is spent and simply lies on the surface.
		 */
		public float hardBounceKept = 0.30F;

		/** Below this the slide is over and the arrow stays where it stopped, ready to pick up. */
		public double minSpeed = 0.15;
	}
	/**
	 * Glancing shots bounce off instead of sticking. This is the one section that changes how the
	 * game plays, so it needs the mod on the server to have any effect.
	 *
	 * <p>No damage tuning here on purpose: vanilla already scales arrow damage by
	 * {@code deltaMovement.length()}, so a bounce that sheds speed hits softer and an arrow that
	 * ricochets off a ceiling and accelerates on the way down hits harder, for free.
	 */
	public static final class Ricochet {
		public boolean enabled = true;
		/**
		 * Measured from the SURFACE, not from its normal: 0 is sliding along the wall, 90 is
		 * straight into it. Only hits at or below this angle bounce, so shooting a wall head-on
		 * still plants the arrow exactly like vanilla.
		 */
		public float maxGlancingAngleDegrees = 35.0F;
		/**
		 * The MOST of its speed an arrow keeps through a bounce - what a hit travelling flat along
		 * the surface gets back. Low on purpose: a bounce should shed most of the arrow's energy, and
		 * higher values send arrows skipping implausibly far across a room.
		 *
		 * <p>Two things cut into it from there, neither of them a setting. How square-on the hit was,
		 * by the same rule the hard bounce off ice answers to - the energy of a steep hit goes INTO
		 * the block, so there is less of it left to leave with. And what was hit: stone hands back all
		 * of this, wood a little over half, loose ground four fifths but only ever at the sliver of an
		 * angle it lets bounce at all.
		 */
		public float energyRetained = 0.35F;
		public int maxBounces = 2;
		/** Below this speed an arrow just sticks - a nearly-spent arrow skittering looks wrong. */
		public double minSpeed = 0.6;
	}

	/**
	 * Stops arrows pinging off a mob that is still in its invulnerability frames - they stick
	 * instead, without adding any damage vanilla did not already grant.
	 *
	 * <p>OFF by default, deliberately. An arrow standing in a mob that took no damage from it is a
	 * lie told by the renderer: it looks like a hit that landed when vanilla decided it did not.
	 * The bounce is honest, even if it looks odd. Left in for anyone who prefers the look.
	 */
	public static final class StickThroughInvulnerability {
		public boolean enabled = false;
	}

	/**
	 * Loads the config, writing a fully-populated file the first time. Any failure leaves the
	 * defaults in place rather than stopping the mod: a bad config file should cost you your
	 * customisation, not your game.
	 */
	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		try {
			if (Files.exists(path)) {
				try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
					BetterArrowsConfig loaded = GSON.fromJson(reader, BetterArrowsConfig.class);
					if (loaded != null) {
						loaded.fillMissingSections();
						loaded.clampToUsableRanges();
						instance = loaded;
					}
				}
			}
			save(path);
		} catch (Exception e) {
			// Includes a malformed file (JsonSyntaxException) and an unwritable config directory.
			BetterArrowsMod.logOnce("Failed to load " + FILE_NAME + ", using defaults", e);
		}
	}

	/** Writes the current values back out - used by the settings screen after an edit. */
	public static void save() {
		try {
			save(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
		} catch (Exception e) {
			BetterArrowsMod.logOnce("Failed to save " + FILE_NAME, e);
		}
	}

	private static void save(Path path) throws IOException {
		Files.createDirectories(path.getParent());
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			GSON.toJson(instance, writer);
		}
	}

	/**
	 * Gson leaves a field null when its key is absent, so a config written by an older version -
	 * or one a user trimmed by hand - would otherwise hand out null sections and NPE on the first
	 * arrow. Every section missing from the file falls back to its defaults.
	 */
	private void fillMissingSections() {
		if (this.stuckArrows == null) {
			this.stuckArrows = new StuckArrows();
		}
		if (this.surfaceSounds == null) {
			this.surfaceSounds = new SurfaceSounds();
		}
		if (this.impactParticles == null) {
			this.impactParticles = new ImpactParticles();
		}
		if (this.impactParticles.block == null) {
			this.impactParticles.block = new ImpactParticles.Block();
		}
		if (this.ricochet == null) {
			this.ricochet = new Ricochet();
		}
		if (this.iceSlide == null) {
			this.iceSlide = new IceSlide();
		}
		if (this.stickThroughInvulnerability == null) {
			this.stickThroughInvulnerability = new StickThroughInvulnerability();
		}
	}

	/**
	 * Hand-edited values are not to be trusted. A negative particle count, an angle of 800 degrees
	 * or an energy factor above 1 (an arrow gaining speed with every bounce, forever) would each
	 * turn a typo into a broken or runaway game.
	 */
	private void clampToUsableRanges() {
		this.surfaceSounds.volume = clamp(this.surfaceSounds.volume, 0.0F, 1.0F);
		this.impactParticles.block.count = clamp(this.impactParticles.block.count, 0, 64);
		this.ricochet.maxGlancingAngleDegrees = clamp(this.ricochet.maxGlancingAngleDegrees, 0.0F, 89.0F);
		this.ricochet.energyRetained = clamp(this.ricochet.energyRetained, 0.05F, 1.0F);
		this.ricochet.maxBounces = clamp(this.ricochet.maxBounces, 0, 16);
		this.ricochet.minSpeed = Math.max(this.ricochet.minSpeed, 0.0);
		// 89 rather than 90: at exactly 90 the arrow is travelling straight into the ice and there
		// is no tangential component left to slide on.
		this.iceSlide.maxAngleDegrees = clamp(this.iceSlide.maxAngleDegrees, 0.0F, 89.0F);
		// Strictly below 1: at 1 the arrow keeps all its speed and never stops sliding.
		this.iceSlide.speedKept = clamp(this.iceSlide.speedKept, 0.1F, 0.99F);
		this.iceSlide.normalKept = clamp(this.iceSlide.normalKept, 0.0F, 0.9F);
		this.iceSlide.hardBounceKept = clamp(this.iceSlide.hardBounceKept, 0.05F, 0.9F);
		this.iceSlide.maxSlides = clamp(this.iceSlide.maxSlides, 1, 200);
		this.iceSlide.minSpeed = Math.max(this.iceSlide.minSpeed, 0.01);
	}

	private static float clamp(float value, float min, float max) {
		if (!Float.isFinite(value)) {
			return min;
		}
		return value < min ? min : Math.min(value, max);
	}

	private static int clamp(int value, int min, int max) {
		return value < min ? min : Math.min(value, max);
	}
}
