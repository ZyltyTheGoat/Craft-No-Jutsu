package net.mcreator.naruto;

public class JutsuData {
	private final String name;
	private final String type;
	private final String nature;
	private final int chakraCost;
	private final int chargeTime;
	private final int cooldown;
	private final boolean isToggle;
	private final int color;

	public JutsuData(String name, String type, String nature, int chakraCost, int chargeTime, int cooldown, boolean isToggle, int color) {
		this.name = name;
		this.type = type;
		this.nature = nature;
		this.chakraCost = chakraCost;
		this.chargeTime = chargeTime;
		this.cooldown = cooldown;
		this.isToggle = isToggle;
		this.color = color;
	}

	// getters
	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getNature() {
		return nature;
	}

	public int getChakraCost() {
		return chakraCost;
	}

	public int getCooldown() {
		return cooldown;
	}

	public int getChargeTime() {
		return chargeTime;
	}

	public boolean getIsToggle() {
		return isToggle;
	}

	public int getColor() {
		return color;
	}

	public String getIconName() {
		return "icon_" + this.getNature().toLowerCase();
	}
}
