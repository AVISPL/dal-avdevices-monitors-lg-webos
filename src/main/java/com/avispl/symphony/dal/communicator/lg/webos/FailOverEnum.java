/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.communicator.lg.webos;

/**
 * FailOverEnum class defined the enum provides list fail over status
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 12/1/2022
 * @version 1.4.0
 * @since 1.4.0
 */
public enum FailOverEnum {

	OFF("Off", "00"),
	AUTO("Auto", "01"),
	MANUAL("Manual", "02");

	private final String name;
	private final String value;

	/**
	 * FailOverEnum instantiation
	 *
	 * @param name {@link #name}
	 * @param value {@link #value}
	 */
	FailOverEnum(String name, String value) {
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