/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.webos;

/**
 * Tint class provides list of Tint name and value
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 4/19/2023
 * @version 1.4.0
 * @since 1.4.0
 */
public enum Tint {

	R50("R50", "00"),
	R49("R49", "01"),
	R48("R48", "02"),
	R47("R47", "03"),
	R46("R46", "04"),
	R45("R45", "05"),
	R44("R44", "06"),
	R43("R43", "07"),
	R42("R42", "08"),
	R41("R41", "09"),
	R40("R40", "0a"),
	R39("R39", "0b"),
	R38("R38", "0c"),
	R37("R37", "0d"),
	R36("R36", "0e"),
	R35("R35", "0f"),
	R34("R34", "10"),
	R33("R33", "11"),
	R32("R32", "12"),
	R31("R31", "13"),
	R30("R30", "14"),
	R29("R29", "15"),
	R28("R28", "16"),
	R27("R27", "17"),
	R26("R26", "18"),
	R25("R25", "19"),
	R24("R24", "1a"),
	R23("R23", "1b"),
	R22("R22", "1c"),
	R21("R21", "1d"),
	R20("R20", "1e"),
	R19("R19", "1f"),
	R18("R18", "20"),
	R17("R17", "21"),
	R16("R16", "22"),
	R15("R15", "23"),
	R14("R14", "24"),
	R13("R13", "25"),
	R12("R12", "26"),
	R11("R11", "27"),
	R10("R10", "28"),
	R09("R9", "29"),
	R08("R8", "2a"),
	R07("R7", "2b"),
	R06("R6", "2c"),
	R05("R5", "2d"),
	R04("R4", "2e"),
	R03("R3", "2f"),
	R02("R2", "30"),
	R01("R1", "31"),
	R0("0", "32"),
	G1("G1", "33"),
	G2("G2", "34"),
	G3("G3", "35"),
	G4("G4", "36"),
	G5("G5", "37"),
	G6("G6", "38"),
	G7("G7", "39"),
	G8("G8", "3a"),
	G9("G9", "3b"),
	G10("G10", "3c"),
	G11("G11", "3d"),
	G12("G12", "3e"),
	G13("G13", "3f"),
	G14("G14", "40"),
	G15("G15", "41"),
	G16("G16", "42"),
	G17("G17", "43"),
	G18("G18", "44"),
	G19("G19", "45"),
	G20("G20", "46"),
	G21("G21", "47"),
	G22("G22", "48"),
	G23("G23", "49"),
	G24("G24", "4a"),
	G25("G25", "4b"),
	G26("G26", "4c"),
	G27("G27", "4d"),
	G28("G28", "4e"),
	G29("G29", "4f"),
	G30("G30", "50"),
	G31("G31", "51"),
	G32("G32", "52"),
	G33("G33", "53"),
	G34("G34", "54"),
	G35("G35", "55"),
	G36("G36", "56"),
	G37("G37", "57"),
	G38("G38", "58"),
	G39("G39", "59"),
	G40("G40", "5a"),
	G41("G41", "5b"),
	G42("G42", "5c"),
	G43("G43", "5d"),
	G44("G44", "5e"),
	G45("G45", "5f"),
	G46("G46", "60"),
	G47("G47", "61"),
	G48("G48", "62"),
	G49("G49", "63"),
	G50("G50", "64"),
	;
	private final String name;
	private final String value;

	/**
	 * Tint Constructor instantiation
	 *
	 * @param name the name is name of Tint
	 * @param value the value is value of Tint
	 */
	Tint(String name, String value) {
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