/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.webos;

import com.avispl.symphony.dal.communicator.lg.webos.LgWebOSConstants.commandNames;

/**
 * LgControllingCommand class defined the enum provides list controlling command
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 12/28/2022
 * @version 1.4.0
 * @since 1.4.0
 */
public enum LgControllingCommand {

	INPUT_PRIORITY(LgWebOSConstants.INPUT_PRIORITY, false, false, LgWebOSConstants.commandNames.FAILOVER),
	PRIORITY_UP(LgWebOSConstants.PRIORITY_UP, false, false, LgWebOSConstants.commandNames.FAILOVER),
	PRIORITY_DOWN(LgWebOSConstants.PRIORITY_DOWN, false, false, LgWebOSConstants.commandNames.FAILOVER),
	PRIORITY_INPUT(LgWebOSConstants.PRIORITY_INPUT, false, false, LgWebOSConstants.commandNames.FAILOVER),
	TEMPERATURE(LgWebOSConstants.TEMPERATURE, true, false, LgWebOSConstants.commandNames.TEMPERATURE),
	SOFTWARE_VERSION(LgWebOSConstants.SOFTWARE_VERSION, true, false, LgWebOSConstants.commandNames.SOFTWARE_VERSION),
	TILE_MODE_SETTINGS(LgWebOSConstants.TILE_MODE_SETTINGS, true, false, LgWebOSConstants.commandNames.TILE_MODE_SETTINGS),
	SERIAL_NUMBER(LgWebOSConstants.SERIAL_NUMBER, true, false, LgWebOSConstants.commandNames.SERIAL_NUMBER),
	DISPLAY_STAND_BY_MODE(LgWebOSConstants.DISPLAY_STAND_BY_MODE, true, false, LgWebOSConstants.commandNames.DISPLAY_STAND_BY_MODE),
	POWER_MANAGEMENT_MODE(LgWebOSConstants.POWER_MANAGEMENT_MODE, false, true, LgWebOSConstants.commandNames.POWER_MANAGEMENT_MODE),
	DATE(LgWebOSConstants.DATE, true, false, LgWebOSConstants.commandNames.DATE),
	TIME(LgWebOSConstants.TIME, true, false, LgWebOSConstants.commandNames.TIME),
	NETWORK_SETTING(LgWebOSConstants.NETWORK_SETTING, true, false, LgWebOSConstants.commandNames.NETWORK_SETTING),
	INPUT_SELECT(LgWebOSConstants.INPUT_SELECT, true, false, LgWebOSConstants.commandNames.INPUT_SELECT),
	POWER(LgWebOSConstants.POWER, false, true, LgWebOSConstants.commandNames.POWER),
	FAN_STATUS(LgWebOSConstants.FAN, true, false, LgWebOSConstants.commandNames.FAN_STATUS),
	SYNC_STATUS(LgWebOSConstants.SIGNAL, true, false, LgWebOSConstants.commandNames.SYNC_STATUS),
	FAILOVER(LgWebOSConstants.FAILOVER_MODE, true, false, LgWebOSConstants.commandNames.FAILOVER),
	TILE_MODE(LgWebOSConstants.TILE_MODE, true, false, LgWebOSConstants.commandNames.TILE_MODE_CONTROL),
	NATURAL_MODE(LgWebOSConstants.NATURAL_MODE, true, false, LgWebOSConstants.commandNames.NATURAL_MODE),
	TILE_ID(LgWebOSConstants.TILE_MODE_ID, true, false, LgWebOSConstants.commandNames.TILE_ID),
	NATURAL_SIZE(LgWebOSConstants.NATURAL_SIZE, true, false, LgWebOSConstants.commandNames.NATURAL_SIZE),
	MUTE(LgWebOSConstants.MUTE, false, true, LgWebOSConstants.commandNames.MUTE),
	VOLUME(LgWebOSConstants.VOLUME, false, true, LgWebOSConstants.commandNames.VOLUME),
	BACKLIGHT(LgWebOSConstants.BACKLIGHT, false, true, LgWebOSConstants.commandNames.BACKLIGHT),
	FAILOVER_INPUT_LIST(LgWebOSConstants.FAILOVER_INPUT_LIST, true, false, LgWebOSConstants.commandNames.FAILOVER_INPUT_LIST),
	ASPECT_RATIO(LgWebOSConstants.ASPECT_RATIO, false, true, LgWebOSConstants.commandNames.ASPECT_RATIO),
	BRIGHTNESS_CONTROL(LgWebOSConstants.BRIGHTNESS_CONTROL, false, true, LgWebOSConstants.commandNames.BRIGHTNESS_CONTROL),
	CONTRAST(LgWebOSConstants.CONTRAST, false, true, LgWebOSConstants.commandNames.CONTRAST),
	PICTURE_MODE(LgWebOSConstants.PICTURE_MODE, false, true, LgWebOSConstants.commandNames.PICTURE_MODE),
	BRIGHTNESS(LgWebOSConstants.BRIGHTNESS, false, true, LgWebOSConstants.commandNames.BRIGHTNESS),
	SHARPNESS(LgWebOSConstants.SHARPNESS, false, true, LgWebOSConstants.commandNames.SHARPNESS),
	SCREEN_COLOR(LgWebOSConstants.SCREEN_COLOR, false, true, LgWebOSConstants.commandNames.SCREEN_COLOR),
	TINT(LgWebOSConstants.TINT, false, true, LgWebOSConstants.commandNames.TINT),
	COLOR_TEMPERATURE(LgWebOSConstants.COLOR_TEMPERATURE, false, true, LgWebOSConstants.commandNames.COLOR_TEMPERATURE),
	BALANCE(LgWebOSConstants.BALANCE, false, true, LgWebOSConstants.commandNames.BALANCE),
	SOUND_MODE(LgWebOSConstants.SOUND_MODE, false, true, LgWebOSConstants.commandNames.SOUND_MODE),
	NO_SIGNAL_POWER_OFF(LgWebOSConstants.NO_SIGNAL_POWER_OFF, false, true, LgWebOSConstants.commandNames.NO_SIGNAL_POWER_OFF),
	NO_IR_POWER_OFF(LgWebOSConstants.NO_IR_POWER_OFF, false, true, LgWebOSConstants.commandNames.NO_IR_POWER_OFF),
	LANGUAGE(LgWebOSConstants.LANGUAGE, false, true, LgWebOSConstants.commandNames.LANGUAGE),
	POWER_ON_STATUS(LgWebOSConstants.POWER_ON_STATUS, false, true, LgWebOSConstants.commandNames.POWER_ON_STATUS),
	REBOOT(LgWebOSConstants.REBOOT, false, false, LgWebOSConstants.commandNames.REBOOT);

	private final String name;
	private final boolean isMonitorType;
	private final boolean isControlType;
	private commandNames commandNames;

	/**
	 * InputSourceDropdown instantiation
	 *
	 * @param name {@link #name}
	 * @param isMonitor {@link #isMonitorType}
	 * @param isControl {@link #isControlType}
	 * @param commandNames {@link #commandNames}
	 */
	LgControllingCommand(String name, boolean isMonitor, boolean isControl, commandNames commandNames) {
		this.name = name;
		this.isMonitorType = isMonitor;
		this.isControlType = isControl;
		this.commandNames = commandNames;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves {@link #isControlType}
	 *
	 * @return value of {@link #isControlType}
	 */
	public boolean isControlType() {
		return isControlType;
	}

	/**
	 * Retrieves {@link #commandNames}
	 *
	 * @return value of {@link #commandNames}
	 */
	public LgWebOSConstants.commandNames getCommandNames() {
		return commandNames;
	}

	/**
	 * Retrieves {@link #isMonitorType}
	 *
	 * @return value of {@link #isMonitorType}
	 */
	public boolean isMonitorType() {
		return isMonitorType;
	}

	/**
	 * Get command name by name of property
	 *
	 * @param value the value is name of command
	 * @return LgControllingCommand is LgControllingCommand instance
	 */
	public static LgControllingCommand getCommandByName(String value) {
		for (LgControllingCommand lgControllingCommand : LgControllingCommand.values()) {
			if (lgControllingCommand.getName().equalsIgnoreCase(value)) {
				return lgControllingCommand;
			}
		}
		throw new IllegalArgumentException(String.format("The command %s doesn't support", value));
	}

	/**
	 * Get command name by name of property
	 *
	 * @param value the value is name of command
	 * @return LgControllingCommand is LgControllingCommand instance
	 */
	public static String getNameByCommand(commandNames value) {
		for (LgControllingCommand lgControllingCommand : LgControllingCommand.values()) {
			if (lgControllingCommand.getCommandNames().name().equals(value.name())) {
				return lgControllingCommand.getName();
			}
		}
		throw new IllegalArgumentException(String.format("The command %s doesn't support", value));
	}
}