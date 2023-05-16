/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.communicator.lg.webos;

/**
 * BrightnessSize  class defined the enum for monitoring and controlling process
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 1/11/2023
 * @version 1.4.0
 * @since 1.4.0
 */
public enum BrightnessSize {

	FULL_SCREEN("Off", "00"),
	ORIGINAL("Minimum", "01"),
	MEDIUM("Medium", "02"),
	MAXIMUM("Maximum", "03"),
	AUTO("Auto", "04");

	private final String name;
	private final String value;

	/**
	 * BrightnessSize instantiation
	 *
	 * @param name {@link #name}
	 * @param value {@link #value}
	 */
	BrightnessSize(String name, String value) {
		this.name = name;
		this.value = value;
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
	 * Retrieves {@link #value}
	 *
	 * @return value of {@link #value}
	 */
	public String getValue() {
		return value;
	}
}