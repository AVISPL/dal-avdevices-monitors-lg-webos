/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.communicator.lg.webos;

/**
 * Language class provides during the monitoring and controlling process
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 1/11/2023
 * @version 1.4.0
 * @since 1.4.0
 */
public enum Language {

	CZECH("Czech", "00"),
	DANISH("Danish", "01"),
	GERMAN("German", "02"),
	ENGLISH("English", "03"),
	SPANISH("Spanish (Europe)", "04"),
	GREEK("Greek", "05"),
	FRENCH("French", "06"),
	ITALIAN("Italian", "07"),
	DUTCH("Dutch", "08"),
	NORWEGIAN("Norwegian", "09"),
	PORTUGUESE("Portuguese", "0a"),
	PORTUGUESE_BRAZIL("Portuguese (Brazil)", "0b"),
	RUSSIAN("Russian", "0c"),
	FINNISH("Finnish", "0d"),
	SWEDISH("Swedish", "0e"),
	KOREAN("Korean", "0f"),
	CHINESE("Chinese (Mandarin)", "10"),
	JAPANESE("Japanese", "11"),
	CHINESE_CANTONESE("Chinese (Cantonese)", "12"),
	ARABIC("Arabic", "13"),
	TURKISH("Turkish", "14"),
	POLISH("Polish", "15");

	private final String name;
	private final String value;

	/**
	 * Language instantiation
	 *
	 * @param name {@link #name}
	 * @param value {@link #value}
	 */
	Language(String name, String value) {
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