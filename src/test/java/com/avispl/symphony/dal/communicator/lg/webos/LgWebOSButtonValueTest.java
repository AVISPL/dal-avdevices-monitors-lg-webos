/*
 * Copyright (c) 2026 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.webos;

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
}
