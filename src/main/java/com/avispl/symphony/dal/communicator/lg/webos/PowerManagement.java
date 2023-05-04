/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.webos;

/**
 * PowerManagement class defined the enum provides list power management value
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 12/1/2022
 * @version 1.4.0
 * @since 1.4.0
 */
public enum PowerManagement {

	OFF("Off", "00"),
	SECOND_10("10 seconds", "02"),
	MINUTE_1("1 minute", "04"),
	MINUTE_3("3 minutes", "05"),
	MINUTE_5("5 minutes", "06"),
	MINUTE_10("10 minutes", "07");

	private final String name;
	private final String value;

	/**
	 * PowerManagement instantiation
	 *
	 * @param name {@link #name}
	 * @param value {@link #value}
	 */
	PowerManagement(String name, String value) {
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