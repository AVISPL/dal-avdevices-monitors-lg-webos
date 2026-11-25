/*
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.webos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;

/**
 * Button controls must report N/A as their value (SYAL-2242), both when created and after being pressed.
 *
 * @since 1.0.4
 */
@Tag("unitTest")
public class LgWebOSButtonValueTest {

	private final LgWebOSDevice lgWebOSDevice = new LgWebOSDevice();

	private AdvancedControllableProperty createButton(String name) throws Exception {
		Method method = LgWebOSDevice.class.getDeclaredMethod("createButton", String.class, String.class, String.class, long.class);
		method.setAccessible(true);
		return (AdvancedControllableProperty) method.invoke(lgWebOSDevice, name, name, LgWebOSConstants.PROCESSING, 0L);
	}

	/**
	 * A newly created button has N/A as its control value.
	 */
	@Test
	void testCreatedButtonValueIsNA() throws Exception {
		AdvancedControllableProperty reboot = createButton(LgWebOSConstants.REBOOT);
		Assertions.assertEquals(LgWebOSConstants.NA, reboot.getValue());
	}

	/**
	 * After a button is pressed, its statistic and control value stay N/A instead of the pressed value ("1").
	 */
	@Test
	void testPressedButtonKeepsNA() throws Exception {
		String property = "FailOver#" + LgWebOSConstants.PRIORITY_UP;
		Map<String, String> stats = new HashMap<>();
		stats.put(property, LgWebOSConstants.NA);
		List<AdvancedControllableProperty> controls = new ArrayList<>();
		controls.add(createButton(property));

		Method update = LgWebOSDevice.class.getDeclaredMethod("updateValueForTheControllableProperty", String.class, String.class, Map.class, List.class);
		update.setAccessible(true);
		update.invoke(lgWebOSDevice, property, "1", stats, controls);

		Assertions.assertEquals(LgWebOSConstants.NA, stats.get(property));
		Assertions.assertEquals(LgWebOSConstants.NA, controls.get(0).getValue());
	}

	/**
	 * Every button control has a statistic with the same name; Symphony does not show a control without one.
	 */
	private static void assertEveryButtonHasStatistic(Map<String, String> stats, List<AdvancedControllableProperty> controls) {
		Assertions.assertFalse(controls.isEmpty(), "no controls were created");
		for (AdvancedControllableProperty control : controls) {
			if (control.getType() instanceof AdvancedControllableProperty.Button) {
				Assertions.assertEquals(LgWebOSConstants.NA, stats.get(control.getName()), "statistic for button " + control.getName());
			}
		}
	}

	/**
	 * The Reboot button is reported with a Reboot statistic of N/A, also when the power state is unknown, and pauses
	 * monitoring for the reboot grace period.
	 */
	@Test
	void testRebootButtonHasStatistic() throws Exception {
		Map<String, String> stats = new HashMap<>();
		List<AdvancedControllableProperty> controls = new ArrayList<>();

		Method populate = LgWebOSDevice.class.getDeclaredMethod("populateDisplayPropertyGroup", LgControllingCommand.class, Map.class, List.class);
		populate.setAccessible(true);
		populate.invoke(lgWebOSDevice, LgControllingCommand.POWER, stats, controls);

		AdvancedControllableProperty reboot = controls.stream().filter(item -> LgWebOSConstants.REBOOT.equals(item.getName())).findFirst()
				.orElseThrow(() -> new AssertionError("Reboot control"));
		// SYAL-2241: Symphony pauses monitoring for the button's grace period while the display reboots
		Assertions.assertEquals(LgWebOSConstants.REBOOT_GRACE_PERIOD, ((AdvancedControllableProperty.Button) reboot.getType()).getGracePeriod());
		Assertions.assertEquals(LgWebOSConstants.NA, stats.get(LgWebOSConstants.REBOOT));
		assertEveryButtonHasStatistic(stats, controls);
	}

	/**
	 * The FailOver priority Up/Down buttons are reported with N/A statistics for the first, middle and last priority.
	 */
	@Test
	void testPriorityButtonsHaveStatistics() throws Exception {
		Map<String, String> priorities = new HashMap<>();
		priorities.put(LgWebOSConstants.PRIORITY + 1, "HDMI1");
		priorities.put(LgWebOSConstants.PRIORITY + 2, "HDMI2");
		priorities.put(LgWebOSConstants.PRIORITY + 3, "DP");
		Field cache = LgWebOSDevice.class.getDeclaredField("cacheMapOfPriorityInputAndValue");
		cache.setAccessible(true);
		cache.set(lgWebOSDevice, priorities);

		Method populate = LgWebOSDevice.class.getDeclaredMethod("populatePriorityInput", Map.class, List.class, String.class, String.class);
		populate.setAccessible(true);
		String group = "FailOver#";
		for (String current : new String[] { "HDMI1", "HDMI2", "DP" }) {
			Map<String, String> stats = new HashMap<>();
			List<AdvancedControllableProperty> controls = new ArrayList<>();
			populate.invoke(lgWebOSDevice, stats, controls, group, current);
			assertEveryButtonHasStatistic(stats, controls);
		}
	}
}
