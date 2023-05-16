/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.webos;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.convert.ConversionException;

/**
 * ClassTypeHandler  class defined the enum for monitoring and controlling process
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 12/13/2022
 * @version 1.4.0
 * @since 1.4.0
 */
public class EnumTypeHandler {

	/**
	 * Get an array of all enum names
	 *
	 * @param enumType the enumtype is enum class
	 */
	public static <T extends Enum<T>> String[] getEnumNames(Class<T> enumType) {
		List<String> names = new ArrayList<>();
		for (T c : enumType.getEnumConstants()) {
			try {
				Method method = c.getClass().getMethod("getName");
				String name = (String) method.invoke(c); // getName executed
				names.add(name);
			} catch (Exception e) {
				throw new ConversionException("Error to convert enum " + enumType.getSimpleName() + " to names") {
				};
			}
		}
		return names.toArray(new String[names.size()]);
	}

	/**
	 * Get name of enum by value
	 *
	 * @param enumType the enumtype is enum class
	 * @param value the value is value of enum
	 * @param <T> is enum type instance
	 * @return String is value of enum or None if not found the value of enum
	 */
	public static <T extends Enum<T>> String getNameEnumByValue(Class<T> enumType, String value) {
		for (T c : enumType.getEnumConstants()) {
			try {
				Method method = c.getClass().getMethod("getValue");
				String name = (String) method.invoke(c);
				if (name.equals(value)) {
					method = c.getClass().getMethod("getName");
					return (String) method.invoke(c);
				}
			} catch (Exception e) {
				return LgWebOSConstants.NA;
			}
		}
		return LgWebOSConstants.NA;
	}

	/**
	 * Get name of enum by value
	 *
	 * @param enumType the enumype is enum class
	 * @param value the value is value of enum
	 * @param <T> is enum type instance
	 * @return String is value of enum or None if not found the value of enum
	 */
	public static <T extends Enum<T>> String getValueOfEnumByName(Class<T> enumType, String value) {
		for (T c : enumType.getEnumConstants()) {
			try {
				Method method = c.getClass().getMethod("getName");
				String name = (String) method.invoke(c);
				if (name.equals(value)) {
					method = c.getClass().getMethod("getValue");
					return (String) method.invoke(c);
				}
			} catch (Exception e) {
				return LgWebOSConstants.NA;
			}
		}
		return LgWebOSConstants.NA;
	}
}