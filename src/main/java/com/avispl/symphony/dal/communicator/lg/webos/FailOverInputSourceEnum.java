/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.communicator.lg.webos;

/**
 * FailOverInputSourceEnum class defined the enum provides list input source
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 1/4/2023
 * @version 1.4.0
 * @since 1.4.0
 */
public enum FailOverInputSourceEnum {

	RGB("AV", "60"),
	DVI_D("DVI-D", "70"),
	HDMI1("HDMI1", "90"),
	HDMI2("HDMI2", "91"),
	OPS_HDMI3_DVI_D("HDMI3/OPS/DVI", "92"),
	OPS_DVI_D("OPS/DVI-D", "95"),
	HDMI3_DVI_D("HDMI3/DVI-D", "96"),
	HDMI3_HDMI2_DVI_D("HDMI3/HDMI2/DVI-D", "97"),
	OPS("OPS", "98"),
	HDMI2_OPSV("HDMI2/OPS", "99"),
	DISPLAYPORT("DISPLAYPORT", "c0"),
	DISPLAYPORT_USB_C("DISPLAYPORT/USB-C", "c1"),
	HDMI3("HDMI3", "c2"),
	HDBASE_T("HDBaseT", "c3");

	private final String name;
	private final String value;

	/**
	 * FailOverInputSourceEnum instantiation
	 *
	 * @param name {@link #name}
	 * @param value {@link #value}
	 */
	FailOverInputSourceEnum(String name, String value) {
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