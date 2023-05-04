/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.webos;

/**
 * Balance class defined the enum to provides Balance name and value
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 4/21/2023
 * @version 1.4.0
 * @since 1.4.0
 */
public enum Balance {

	L50("L50", "00"),
	L49("L49", "01"),
	L48("L48", "02"),
	L47("L47", "03"),
	L46("L46", "04"),
	L45("L45", "05"),
	L44("L44", "06"),
	L43("L43", "07"),
	L42("L42", "08"),
	L41("L41", "09"),
	L40("L40", "0a"),
	L39("L39", "0b"),
	L38("L38", "0c"),
	L37("L37", "0d"),
	L36("L36", "0e"),
	L35("L35", "0f"),
	L34("L34", "10"),
	L33("L33", "11"),
	L32("L32", "12"),
	L31("L31", "13"),
	L30("L30", "14"),
	L29("L29", "15"),
	L28("L28", "16"),
	L27("L27", "17"),
	L26("L26", "18"),
	L25("L25", "19"),
	L24("L24", "1a"),
	L23("L23", "1b"),
	L22("L22", "1c"),
	L21("L21", "1d"),
	L20("L20", "1e"),
	L19("L19", "1f"),
	L18("L18", "20"),
	L17("L17", "21"),
	L16("L16", "22"),
	L15("L15", "23"),
	L14("L14", "24"),
	L13("L13", "25"),
	L12("L12", "26"),
	L11("L11", "27"),
	L10("L10", "28"),
	L09("L9", "29"),
	L08("L8", "2a"),
	L07("L7", "2b"),
	L06("L6", "2c"),
	L05("L5", "2d"),
	L04("L4", "2e"),
	L03("L3", "2f"),
	L02("L2", "30"),
	L01("L1", "31"),
	L0("0", "32"),
	R1("R1", "33"),
	R2("R2", "34"),
	R3("R3", "35"),
	R4("R4", "36"),
	R5("R5", "37"),
	R6("R6", "38"),
	R7("R7", "39"),
	R8("R8", "3a"),
	R9("R9", "3b"),
	R10("R10", "3c"),
	R11("R11", "3d"),
	R12("R12", "3e"),
	R13("R13", "3f"),
	R14("R14", "40"),
	R15("R15", "41"),
	R16("R16", "42"),
	R17("R17", "43"),
	R18("R18", "44"),
	R19("R19", "45"),
	R20("R20", "46"),
	R21("R21", "47"),
	R22("R22", "48"),
	R23("R23", "49"),
	R24("R24", "4a"),
	R25("R25", "4b"),
	R26("R26", "4c"),
	R27("R27", "4d"),
	R28("R28", "4e"),
	R29("R29", "4f"),
	R30("R30", "50"),
	R31("R31", "51"),
	R32("R32", "52"),
	R33("R33", "53"),
	R34("R34", "54"),
	R35("R35", "55"),
	R36("R36", "56"),
	R37("R37", "57"),
	R38("R38", "58"),
	R39("R39", "59"),
	R40("R40", "5a"),
	R41("R41", "5b"),
	R42("R42", "5c"),
	R43("R43", "5d"),
	R44("R44", "5e"),
	R45("R45", "5f"),
	R46("R46", "60"),
	R47("R47", "61"),
	R48("R48", "62"),
	R49("R49", "63"),
	R50("R50", "64");

	private final String name;
	private final String value;

	/**
	 * Balance Constructor instantiation
	 *
	 * @param name the name is name of Tint
	 * @param value the value is value of Tint
	 */
	Balance(String name, String value) {
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