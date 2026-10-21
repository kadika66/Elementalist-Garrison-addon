package xyz.unbent.elementalist;

import org.jetbrains.annotations.NotNull;
import xyz.unbent.garrison.api.addon.AddonContext;
import xyz.unbent.garrison.api.addon.GarrisonAddon;

/**
 * Elementalist, a Garrison addon: registers {@code behaviour: elementalist}. Goes in plugins/Garrison/addons/.
 * Needs Garrison API 1.4.0 or later (menu buttons, guard data, guard damage).
 */
public final class ElementalistAddon implements GarrisonAddon {

	public static final String BEHAVIOUR_ID = "elementalist";
	private static final String MIN_API = "1.4.0";

	private Casts casts;

	@Override
	public void onEnable(@NotNull AddonContext context) {
		String api = context.getGarrison().getApiVersion();
		if (compare(api, MIN_API) < 0)
			throw new IllegalStateException("Elementalist needs Garrison API " + MIN_API + " or later; this Garrison has " + api + ". Update Garrison.");
		ElementalistSettings settings = ElementalistSettings.load(context);
		casts = new Casts(context.getPlugin(), context.getGarrison(), settings);
		context.getGarrison().registerBehaviour(context.getPlugin(), BEHAVIOUR_ID, new ElementalistBehaviour(casts));
		context.getLogger().info("Registered behaviour '" + BEHAVIOUR_ID + "'. Give a guard type 'behaviour: " + BEHAVIOUR_ID + "' in garrison.yml.");
	}

	@Override
	public void onDisable() {
		if (casts != null)
			casts.shutdown();
	}

	/** Compares dotted version numbers, e.g. "1.10.0" > "1.4.0". */
	static int compare(String a, String b) {
		String[] x = a.split("\\."), y = b.split("\\.");
		for (int i = 0; i < Math.max(x.length, y.length); i++) {
			int p = i < x.length ? parse(x[i]) : 0, q = i < y.length ? parse(y[i]) : 0;
			if (p != q)
				return Integer.compare(p, q);
		}
		return 0;
	}

	private static int parse(String part) {
		try {
			return Integer.parseInt(part.replaceAll("\\D.*", ""));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
