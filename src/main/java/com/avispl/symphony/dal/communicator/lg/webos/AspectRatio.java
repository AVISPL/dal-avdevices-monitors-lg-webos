/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.webos;

/**
 * AspectRatio class provides aspect ratio value
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 1/11/2023
 * @version 1.4.0
 * @since 1.4.0
 */
public enum AspectRatio {

	FULL_SCREEN("Full Screen", "02"),
	ORIGINAL("Original", "06");

	private final String name;
	private final String value;

	/**
	 * AspectRatio instantiation
	 *
	 * @param name {@link #name}
	 * @param value {@link #value}
	 */
	AspectRatio(String name, String value) {
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