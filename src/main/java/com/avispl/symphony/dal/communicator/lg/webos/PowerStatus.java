/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.communicator.lg.webos;

/**
 * PowerStatus class provides during the monitoring and controlling process
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 1/11/2023
 * @version 1.4.0
 * @since 1.4.0
 */
public enum PowerStatus {

	LST("LST (Last Status)", "00"),
	STD("STD (Standby)", "01"),
	PWR("PWR (Power On)", "02");

	private final String name;
	private final String value;

	/**
	 * PowerStatus instantiation
	 *
	 * @param name {@link #name}
	 * @param value {@link #value}
	 */
	PowerStatus(String name, String value) {
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