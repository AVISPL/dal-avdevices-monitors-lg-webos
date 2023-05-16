/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.webos;

import java.util.HashMap;
import java.util.Map;

/**
 * Set of constants
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 30/1/2022
 * @version 1.4.0
 * @since 1.4.0
 */
public class LgWebOSConstants {
	enum powerStatusNames {ON, OFF, UNAVAILABLE}

	final static Map<powerStatusNames, byte[]> powerStatus = new HashMap<powerStatusNames, byte[]>() {{
		put(powerStatusNames.ON, new byte[] { '0', '0' });
		put(powerStatusNames.OFF, new byte[] { '0', '1' });
		put(powerStatusNames.UNAVAILABLE, new byte[] {});
	}};

	enum replyStatusNames {OK, NG}

	final static Map<replyStatusNames, byte[]> replyStatusCodes = new HashMap<replyStatusNames, byte[]>() {{
		put(replyStatusNames.OK, new byte[] { 'O', 'K' });
		put(replyStatusNames.NG, new byte[] { 'N', 'G' });
	}};

	enum fanStatusNames {FAULTY, NORMAL, NO_FAN, NOT_SUPPORTED}

	final static Map<fanStatusNames, byte[]> fanStatusCodes = new HashMap<fanStatusNames, byte[]>() {{
		put(fanStatusNames.FAULTY, new byte[] { '0', '0' });
		put(fanStatusNames.NORMAL, new byte[] { '0', '1' });
		put(fanStatusNames.NO_FAN, new byte[] { '0', '2' });
	}};

	enum commandNames {
		SYNC_STATUS_PARAM, POWER, INPUT, TEMPERATURE, FAN_STATUS, SYNC_STATUS, GET, SERIAL_NUMBER, SOFTWARE_VERSION, FAILOVER, DATE, TIME, TILE_MODE_SETTINGS, DISPLAY_STAND_BY_MODE, DISPLAY_AND_SOUND, BACKLIGHT, INPUT_SELECT, MUTE, VOLUME,
		POWER_MANAGEMENT_MODE, POWER_MANAGEMENT_MODE_PARAM, FAILOVER_INPUT_LIST, NETWORK_SETTING, NETWORK_SETTING_PARAM, TILE_ID, NATURAL_MODE, NATURAL_SIZE, NATURAL_SIZE_PARAM, TILE_MODE_CONTROL,
		ASPECT_RATIO, BRIGHTNESS_CONTROL, CONTRAST, PICTURE_MODE, BRIGHTNESS, SHARPNESS, SCREEN_COLOR, TINT, COLOR_TEMPERATURE, BALANCE, SOUND_MODE, NO_SIGNAL_POWER_OFF, NO_IR_POWER_OFF, LANGUAGE, POWER_ON_STATUS, REBOOT;
	}

	final static Map<commandNames, byte[]> commands = new HashMap<commandNames, byte[]>() {{
		put(commandNames.POWER, new byte[] { 'k', 'd' });
		put(commandNames.INPUT, new byte[] { 'x', 'b' });
		put(commandNames.TEMPERATURE, new byte[] { 'd', 'n' });
		put(commandNames.FAN_STATUS, new byte[] { 'd', 'w' });
		put(commandNames.SYNC_STATUS, new byte[] { 's', 'v' });
		put(commandNames.GET, new byte[] { 'F', 'F' });
		put(commandNames.SERIAL_NUMBER, new byte[] { 'f', 'y' });
		put(commandNames.SOFTWARE_VERSION, new byte[] { 'f', 'z' });
		put(commandNames.FAILOVER, new byte[] { 'm', 'i' });
		put(commandNames.FAILOVER_INPUT_LIST, new byte[] { 'm', 'j' });
		put(commandNames.DATE, new byte[] { 'f', 'a' });
		put(commandNames.TIME, new byte[] { 'f', 'x' });
		put(commandNames.TILE_MODE_SETTINGS, new byte[] { 'd', 'z' });
		put(commandNames.DISPLAY_STAND_BY_MODE, new byte[] { 'f', 'j' });
		put(commandNames.BACKLIGHT, new byte[] { 'm', 'g' });
		put(commandNames.MUTE, new byte[] { 'k', 'e' });
		put(commandNames.VOLUME, new byte[] { 'k', 'f' });
		put(commandNames.POWER_MANAGEMENT_MODE, new byte[] { 's', 'n' });
		put(commandNames.POWER_MANAGEMENT_MODE_PARAM, new byte[] { '0', 'c', ' ', 'f', 'f' });
		put(commandNames.INPUT_SELECT, new byte[] { 'x', 'b' });
		put(commandNames.NETWORK_SETTING, new byte[] { 's', 'n' });
		put(commandNames.NETWORK_SETTING_PARAM, new byte[] { '8', '2', ' ', 'f', 'f' });
		put(commandNames.TILE_ID, new byte[] { 'd', 'i' });
		put(commandNames.NATURAL_SIZE, new byte[] { 's', 'n' });
		put(commandNames.NATURAL_SIZE_PARAM, new byte[] { 'a', '5', ' ', 'f', 'f' });
		put(commandNames.NATURAL_MODE, new byte[] { 'd', 'j' });
		put(commandNames.TILE_MODE_CONTROL, new byte[] { 'd', 'd' });
		put(commandNames.ASPECT_RATIO, new byte[] { 'k', 'c' });
		put(commandNames.BRIGHTNESS_CONTROL, new byte[] { 'j', 'q' });
		put(commandNames.CONTRAST, new byte[] { 'k', 'g' });
		put(commandNames.PICTURE_MODE, new byte[] { 'd', 'x' });
		put(commandNames.BRIGHTNESS, new byte[] { 'k', 'h' });
		put(commandNames.SHARPNESS, new byte[] { 'k', 'k' });
		put(commandNames.SCREEN_COLOR, new byte[] { 'k', 'i' });
		put(commandNames.TINT, new byte[] { 'k', 'j' });
		put(commandNames.COLOR_TEMPERATURE, new byte[] { 'x', 'u' });
		put(commandNames.BALANCE, new byte[] { 'k', 't' });
		put(commandNames.SOUND_MODE, new byte[] { 'd', 'y' });
		put(commandNames.NO_SIGNAL_POWER_OFF, new byte[] { 'f', 'g' });
		put(commandNames.NO_IR_POWER_OFF, new byte[] { 'm', 'n' });
		put(commandNames.LANGUAGE, new byte[] { 'f', 'i' });
		put(commandNames.POWER_ON_STATUS, new byte[] { 't', 'r' });
		put(commandNames.SYNC_STATUS_PARAM, new byte[] { '0', '2', ' ', 'F', 'F' });
		put(commandNames.REBOOT, new byte[] { 'k', 'a' });
	}};

	final static byte[] signalStatus = { '0', '2', ' ', 'F', 'F' };

	enum syncStatusNames {NO_SYNC, SYNC}

	final static Map<syncStatusNames, byte[]> syncStatusCodes = new HashMap<syncStatusNames, byte[]>() {{
		put(syncStatusNames.NO_SYNC, new byte[] { '0', '2', '0', '0' });
		put(syncStatusNames.SYNC, new byte[] { '0', '2', '0', '1' });
	}};

	enum inputNames {PLAY_VIA_URL, HDMI3_DTV, HDMI3_PC, AV, COMPONENT, RGB, DVI_D_PC, DVI_D_DTV, HDMI1_DTV, HDMI1_PC, HDMI2_OPS_DTV, HDMI2_OPS_PC, HDMI3_OPS_DVID_DTV, HDMI3_OPS_DVID_PC, OPS_DVID_DTV, OPS_DVID_PC, HDMI3_DVID_DTV, HDMI3_DVID_PC, OPS_DTV, OPS_PC, DISPLAYPORT_DTV, DISPLAYPORT_PC, SUPERSIGN_PLAYER, OTHERS, MULTI_SCREEN, OFF}

	final static Map<inputNames, byte[]> inputs = new HashMap<inputNames, byte[]>() {{
		put(inputNames.AV, new byte[] { '2', '0' });
		put(inputNames.COMPONENT, new byte[] { '4', '0' });
		put(inputNames.RGB, new byte[] { '6', '0' });
		put(inputNames.DVI_D_PC, new byte[] { '7', '0' });
		put(inputNames.DVI_D_DTV, new byte[] { '8', '0' });
		put(inputNames.HDMI1_DTV, new byte[] { '9', '0' });
		put(inputNames.HDMI1_PC, new byte[] { 'a', '0' });
		put(inputNames.HDMI2_OPS_DTV, new byte[] { '9', '1' });
		put(inputNames.HDMI2_OPS_PC, new byte[] { 'a', '1' });
		put(inputNames.HDMI3_OPS_DVID_DTV, new byte[] { '9', '2' });
		put(inputNames.HDMI3_OPS_DVID_PC, new byte[] { 'a', '2' });
		put(inputNames.OPS_DVID_DTV, new byte[] { '9', '5' });
		put(inputNames.OPS_DVID_PC, new byte[] { 'a', '5' });
		put(inputNames.HDMI3_DVID_DTV, new byte[] { '9', '6' });
		put(inputNames.HDMI3_DVID_PC, new byte[] { 'a', '6' });
		put(inputNames.OPS_DTV, new byte[] { '9', '8' });
		put(inputNames.OPS_PC, new byte[] { 'a', '8' });
		put(inputNames.DISPLAYPORT_DTV, new byte[] { 'c', '0' });
		put(inputNames.DISPLAYPORT_PC, new byte[] { 'd', '0' });
		put(inputNames.SUPERSIGN_PLAYER, new byte[] { 'e', '0' });
		put(inputNames.OTHERS, new byte[] { 'e', '1' });
		put(inputNames.MULTI_SCREEN, new byte[] { 'e', '2' });
		put(inputNames.HDMI3_PC, new byte[] { 'd', '2' });
		put(inputNames.HDMI3_DTV, new byte[] { 'c', '2' });
		put(inputNames.PLAY_VIA_URL, new byte[] { 'e', '3' });
	}};

	enum controlProperties {power, input}

	public static String POWER = "Power";
	public static String REBOOT = "Reboot";
	public static String REBOOT_VALUE = "02";
	public static String FAN = "Fan";
	public static String INPUT = "Input";
	public static String TEMPERATURE = "Temperature(C)";
	public static String SIGNAL = "InputSignal";
	public static String SERIAL_NUMBER = "SerialNumber";
	public static String SOFTWARE_VERSION = "SoftwareVersion";
	public static String FAILOVER_MODE = "FailOverMode";
	public static String DATE_TIME = "DateTime";
	public static String TILE_MODE = "TileMode";
	public static String TILE_MODE_COLUMN = "Column";
	public static String NATURAL_MODE = "NaturalMode";
	public static String NATURAL_SIZE = "NaturalSize";
	public static String TILE_MODE_ROW = "Row";
	public static String TILE_MODE_ID = "TileID";
	public static String START_TIME = "DSTStartTime";
	public static String END_TIME = "DSTEndTime";
	public static String DISPLAY = "Display";
	public static String SOUND = "Sound";
	public static String INPUT_GROUP = "Input";
	public static String POWER_MANAGEMENT = "PowerManagement";
	public static String FAILOVER = "FailOver";
	public static String HASH = "#";
	public static String BACKLIGHT = "Backlight";
	public static String BACKLIGHT_VALUE = "BacklightCurrentValue";
	public static String INPUT_SELECT = "InputSelect";
	public static String FAILOVER_INPUT_LIST = "InputList";
	public static String MUTE = "Mute";
	public static String VOLUME = "Volume(%)";
	public static String VOLUME_VALUE = "VolumeCurrentValue(%)";
	public static String ON = "On";
	public static String OFF = "Off";
	public static String POWER_MANAGEMENT_MODE = "PowerManagementMode";
	public static String DISPLAY_STAND_BY_MODE = "DisplayStandbyMode";
	public static int NUMBER_ONE = 1;
	public static int ZERO = 0;
	public static int MAX_RANGE_BACKLIGHT = 100;
	public static int MAX_RANGE_VOLUME = 100;
	public static int MAX_RANGE_CONTRAST = 100;
	public static int MAX_RANGE_BRIGHTNESS = 100;
	public static int MAX_RANGE_SHARPNESS = 50;
	public static int MAX_RANGE_SCREEN_COLOR = 100;
	public static int MAX_RANGE_BALANCE = 100;
	public static int MIN_RANGE_COLOR_TEMPERATURE = 3200;
	public static int MAX_RANGE_COLOR_TEMPERATURE = 13000;
	public static String MUTE_VALUE = "00";
	public static String UNMUTE_VALUE = "01";
	public static String EMPTY_STRING = "";
	public static String COLON = ":";
	public static String SPACE = " ";
	public static String AUTO = "Auto";
	public static String INPUT_PRIORITY = "InputPriority";
	public static String PRIORITY = "Priority";
	public static String PRIORITY_INPUT = "PriorityInput";
	public static String PRIORITY_DOWN = "PriorityDown";
	public static String PROCESSING = "Processing";
	public static String UP = "Up";
	public static String PRIORITY_UP = "PriorityUp";
	public static String DOWN = "Down";
	public static String MANUAL = "Manual";
	public static String HDMI_1 = "HDMI1";
	public static String HDMI_2 = "HDMI2";
	public static String HDMI_3 = "HDMI3";
	public static String DISPLAYPORT = "DISPLAYPORT";
	public static String BYTE_COMMAND = "0c ";
	public static String IS_VALID_CONFIG_MANAGEMENT = "true";
	public static String AM = "AM";
	public static String PM = "PM";
	public static String IP_ADDRESS = "IPAddress";
	public static String SUBNET_MASK = "SubnetMask";
	public static String DNS_SERVER = "DNSServer";
	public static String GATEWAY = "Gateway";
	public static String DOT = ".";
	public static String TILE_MODE_SETTINGS = "TileModeSettings";
	public static String PC = "PC";
	public static String DTV = "DTV";
	public static String ASPECT_RATIO = "AspectRatio";
	public static String BRIGHTNESS_CONTROL = "BrightnessControl";
	public static String CONTRAST = "Contrast";
	public static String CONTRAST_VALUE = "ContrastCurrentValue";
	public static String PICTURE_MODE = "PictureMode";
	public static String BRIGHTNESS = "Brightness";
	public static String BRIGHTNESS_VALUE = "BrightnessCurrentValue";
	public static String SHARPNESS = "Sharpness";
	public static String SHARPNESS_VALUE = "SharpnessCurrentValue";
	public static String SCREEN_COLOR = "ScreenColor";
	public static String SCREEN_COLOR_VALUE = "ScreenColorCurrentValue";
	public static String TINT = "Tint";
	public static String COLOR_TEMPERATURE = "ColorTemperature(K)";
	public static String COLOR_TEMPERATURE_VALUE = "ColorTemperatureCurrentValue(K)";
	public static String BALANCE = "Balance";
	public static String SOUND_MODE = "SoundMode";
	public static String NO_SIGNAL_POWER_OFF = "NoSignalPowerOff(15Min)";
	public static String NO_IR_POWER_OFF = "NoIRPowerOff(4hour)";
	public static String LANGUAGE = "Language";
	public static String TIME_ELAPSED = "TimeElapsed";
	public static String POWER_ON_STATUS = "PowerOnStatus";
	public static String SETTINGS = "Settings";
	public static String[] INPUT_TYPE_DROPDOWN = { "PC", "DTV" };
	public static String COMMA = ",";
	public static String NA = "N/A";
	public static String UNAVAILABLE = "UNAVAILABLE";
	public static String CONTROL_PROTOCOL_STATUS = "ControlProtocolStatus";
	public static String DATE = "Date";
	public static String TIME = "Time";
	public static String NULL_STRING_VALUE = "null";
	public static String NETWORK_SETTING = "NetworkSetting";
	public static String PLAY_VIA_URL = "PLAY_VIA_URL";
	public static int COLOR_TEMPERATURE_MAX_VALUE = 210;
	public static int COLOR_TEMPERATURE_MIN_VALUE = 112;
	public static int COLOR_TEMPERATURE_UI_MIN_VALUE = 3200;
	public static int COLOR_TEMPERATURE_UI_MAX_VALUE = 13000;
	public static int DEFAULT_CACHING_LIFETIME = 5;
	public static int MAX_CACHING_LIFETIME = 5;
	public static int MIN_DELAY_TIME = 100;
	public static int DEFAULT_DELAY_TIME = 400;
	public static int MAX_DELAY_TIME = 500;
	public static int DEFAULT_CONFIG_TIMEOUT = 2000;
	public static int MAX_CONFIG_TIMEOUT = 3000;
	public static int DEFAULT_POLLING_INTERVAL = 2;
	public static int ORDINAL_TO_INDEX_CONVERT_FACTOR = 1;
}
