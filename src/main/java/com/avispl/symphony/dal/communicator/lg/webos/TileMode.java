/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.communicator.lg.webos;

/**
 * TileMode class defined the enum provides tile mode status
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 12/1/2022
 * @version 1.4.0
 * @since 1.4.0
 */
public enum TileMode {

	ON("On", "01", true),
	OFF("Off", "00", true);

	private final String name;
	private final String value;
	private final boolean isStatus;

	/**
	 * PowerManagement instantiation
	 *
	 * @param name {@link #name}
	 * @param value {@link #value}
	 * @param isStatus {@link #isStatus}
	 */
	TileMode(String name, String value, boolean isStatus) {
		this.name = name;
		this.value = value;
		this.isStatus = isStatus;
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

	/**
	 * Retrieves {@link #isStatus}
	 *
	 * @return value of {@link #isStatus}
	 */
	public boolean isStatus() {
		return isStatus;
	}
}