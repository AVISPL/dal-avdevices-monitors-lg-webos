/*
 * Copyright (c) 2022 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.lg.webos;

import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.SocketCommunicator;
import com.avispl.symphony.dal.communicator.lg.webos.LgWebOSConstants.commandNames;
import com.avispl.symphony.dal.communicator.lg.webos.LgWebOSConstants.controlProperties;
import com.avispl.symphony.dal.communicator.lg.webos.LgWebOSConstants.fanStatusNames;
import com.avispl.symphony.dal.communicator.lg.webos.LgWebOSConstants.replyStatusNames;
import com.avispl.symphony.dal.communicator.lg.webos.LgWebOSConstants.syncStatusNames;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * LG WebOS Device Adapter
 * An implementation of SocketCommunicator to provide communication and interaction with LG device
 *
 * Static Monitored Statistics
 * <li>
 * Temperature(C), SerialNumber, SoftwareVersion, InputSignal, InputSelect, Fan, StandbyMode, TileMode,
 * FailOverMode, SoftwareVersion, SerialNumber, DateTim, SubNetmask, IPAddress, DNSServer, Gateway
 * </li>
 *
 * Management Control
 *
 * Display
 * <li>
 * Power, Language, BackLight(%), Mute, Volume(%), AspectRatio, BrightnessSize, PictureMode, Contrast, Brightness, Sharpness, Color, Tint, ColorTemperature(K), Balance, SoundMode
 * </li>
 *
 * FailOver
 * <li>
 * FailOverMode, InputPriority, PriorityInput, PriorityUp, PriorityDown
 * </li>
 *
 * Input
 * <li>
 * InputType, InputSelect
 * </li>
 * Historical Monitored Statistics
 * <li> Temperature </li>
 *
 * @author Harry, Kevin
 * @version 1.4.0
 * @since 1.4.0
 */
public class LgWebOSDevice extends SocketCommunicator implements Controller, Monitorable {

	int monitorID;
	private int currentCommandIndex = 0;
	private int defaultConfigTimeout;
	private int currentCachingLifetime;
	private int pollingIntervalInIntValue;
	private int currentGetMultipleInPollingInterval = 0;
	private boolean isEmergencyDelivery;
	private final Set<String> historicalProperties = new HashSet<>();
	private final Set<String> failedMonitor = new HashSet<>();
	private int localCachedFailedMonitor = 0;
	private Map<String, String> cacheMapOfPriorityInputAndValue = new HashMap<>();
	private int countControlUnavailable = 0;
	private ExtendedStatistics localExtendedStatistics;

	/**
	 * a variable to check the adapter init
	 */
	private boolean isFirstInit;

	/**
	 * To avoid timeout errors, caused by the unavailability of the control protocol, all polling-dependent communication operations (monitoring)
	 * should be performed asynchronously. This executor service executes such operations.
	 */
	private ExecutorService fetchingDataExSer;
	private ExecutorService timeoutManagementExSer;

	/**
	 * Local caching to store failed requests after a period of time
	 */
	private final Map<String, Integer> localCachingLifeTimeOfMap = new HashMap<>();

	/**
	 * Local cache stores data after a period of time
	 */
	private final Map<String, String> localCacheMapOfPropertyNameAndValue = new HashMap<>();

	/**
	 * store pollingInterval adapter properties
	 */
	private String pollingInterval;

	/**
	 * store configTimeout adapter properties
	 */
	private String configTimeout;

	/**
	 * Timestamp of the latest command sent to a device.
	 */
	private long lastCommandTimestamp;

	/**
	 * Apply default delay in between of all the commands performed by the adapter.
	 */
	private long commandsCoolDownDelay;

	/**
	 * store cachingLifetime adapter properties
	 */
	private String cachingLifetime;

	/**
	 * store delayTimeInterVal adapter properties
	 */
	private String coolDownDelay;

	/**
	 * store configManagement adapter properties
	 */
	private String configManagement;

	/**
	 * configManagement in boolean value
	 */
	private boolean isConfigManagement;

	/**
	 * ReentrantLock to prevent null pointer exception to localExtendedStatistics when controlProperty method is called before GetMultipleStatistics method.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	/**
	 * Using Condition to pause the current thread execution until control is complete
	 */
	private final Condition condition = reentrantLock.newCondition();

	/**
	 * {@inheritDoc}
	 *
	 * Override the send() method to add a cool down delay time after every send command
	 */
	@Override
	public byte[] send(byte[] data) throws Exception {
		try {
			long currentTime = System.currentTimeMillis() - lastCommandTimestamp;
			//check next command wait commandsCoolDownDelay time
			if (currentTime < commandsCoolDownDelay) {
				Thread.sleep(commandsCoolDownDelay - currentTime);
			}
			lastCommandTimestamp = System.currentTimeMillis();
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Issuing command %s, timestamp: %s", data, lastCommandTimestamp));
			}
			return super.send(data);
		} finally {
			logger.debug("send data command successfully");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		fetchingDataExSer = Executors.newFixedThreadPool(1);
		timeoutManagementExSer = Executors.newFixedThreadPool(1);
		isFirstInit = false;
		super.internalInit();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalDestroy() {
		if (localExtendedStatistics != null && localExtendedStatistics.getStatistics() != null && localExtendedStatistics.getControllableProperties() != null) {
			localExtendedStatistics.getStatistics().clear();
			localExtendedStatistics.getControllableProperties().clear();
		}
		if (!cacheMapOfPriorityInputAndValue.isEmpty()) {
			cacheMapOfPriorityInputAndValue.clear();
		}

		if (!localCacheMapOfPropertyNameAndValue.isEmpty()) {
			localCacheMapOfPropertyNameAndValue.clear();
		}
		isConfigManagement = false;
		failedMonitor.clear();
		localCachingLifeTimeOfMap.clear();
		try {
			fetchingDataExSer.shutdownNow();
			timeoutManagementExSer.shutdownNow();
		} catch (Exception e) {
			logger.warn("Unable to end the TCP connection.", e);
		} finally {
			super.internalDestroy();
		}
	}

	/**
	 * Constructor set the TCP/IP port to be used as well the default monitor ID
	 */
	public LgWebOSDevice() {
		super();
		this.setPort(9761);
		this.monitorID = 1;

		// set list of command success strings (included at the end of response when command succeeds, typically ending with command prompt)
		this.setCommandSuccessList(Collections.singletonList("OK"));
		// set list of error response strings (included at the end of response when command fails, typically ending with command prompt)
		this.setCommandErrorList(Collections.singletonList("NG"));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 *
	 * Check for available devices before retrieving the value
	 * ping latency information to Symphony
	 */
	@Override
	public int ping() throws Exception {
		if (isInitialized()) {
			long pingResultTotal = 0L;

			for (int i = 0; i < this.getPingAttempts(); i++) {
				long startTime = System.currentTimeMillis();

				try (Socket puSocketConnection = new Socket(this.host, this.getPort())) {
					puSocketConnection.setSoTimeout(this.getPingTimeout());
					if (puSocketConnection.isConnected()) {
						long pingResult = System.currentTimeMillis() - startTime;
						pingResultTotal += pingResult;
						if (this.logger.isTraceEnabled()) {
							this.logger.trace(String.format("PING OK: Attempt #%s to connect to %s on port %s succeeded in %s ms", i + 1, host, this.getPort(), pingResult));
						}
					} else {
						if (this.logger.isDebugEnabled()) {
							this.logger.debug(String.format("PING DISCONNECTED: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
						}
						return this.getPingTimeout();
					}
				} catch (SocketTimeoutException | ConnectException tex) {
					if (this.logger.isDebugEnabled()) {
						this.logger.error(String.format("PING TIMEOUT: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
					}
					throw new SocketTimeoutException("Connection timed out");
				} catch (Exception e) {
					if (this.logger.isDebugEnabled()) {
						this.logger.error(String.format("PING TIMEOUT: Connection to %s did not succeed, UNKNOWN ERROR %s: ", host, e.getMessage()));
					}
					return this.getPingTimeout();
				}
			}
			return Math.max(1, Math.toIntExact(pingResultTotal / this.getPingAttempts()));
		} else {
			throw new IllegalStateException("Cannot use device class without calling init() first");
		}
	}

	/**
	 * Retrieves {@link #configTimeout}
	 *
	 * @return value of {@link #configTimeout}
	 */
	public String getConfigTimeout() {
		return configTimeout;
	}

	/**
	 * Sets {@link #configTimeout} value
	 *
	 * @param configTimeout new value of {@link #configTimeout}
	 */
	public void setConfigTimeout(String configTimeout) {
		this.configTimeout = configTimeout;
	}

	/**
	 * Retrieves {@link #cachingLifetime}
	 *
	 * @return value of {@link #cachingLifetime}
	 */
	public String getCachingLifetime() {
		return cachingLifetime;
	}

	/**
	 * Sets {@link #cachingLifetime} value
	 *
	 * @param cachingLifetime new value of {@link #cachingLifetime}
	 */
	public void setCachingLifetime(String cachingLifetime) {
		this.cachingLifetime = cachingLifetime;
	}

	/**
	 * Retrieves {@link #configManagement}
	 *
	 * @return value of {@link #configManagement}
	 */
	public String getConfigManagement() {
		return configManagement;
	}

	/**
	 * Sets {@link #configManagement} value
	 *
	 * @param configManagement new value of {@link #configManagement}
	 */
	public void setConfigManagement(String configManagement) {
		this.configManagement = configManagement;
	}

	/**
	 * Pool for keeping all the async operations in, to track any operations in progress and cancel them if needed
	 */
	private final List<Future> devicesExecutionPool = new CopyOnWriteArrayList<>();

	/**
	 * Retrieves {@link #historicalProperties}
	 *
	 * @return value of {@link #historicalProperties}
	 */
	public String getHistoricalProperties() {
		return String.join(LgWebOSConstants.COMMA, this.historicalProperties);
	}

	/**
	 * Sets {@link #historicalProperties} value
	 *
	 * @param historicalProperties new value of {@link #historicalProperties}
	 */
	public void setHistoricalProperties(String historicalProperties) {
		this.historicalProperties.clear();
		Arrays.asList(historicalProperties.split(LgWebOSConstants.COMMA)).forEach(propertyName -> this.historicalProperties.add(propertyName.trim()));
	}

	/**
	 * This method is recalled by Symphony to get the current monitor ID (Future purpose)
	 *
	 * @return int This returns the current monitor ID.
	 */
	public int getMonitorID() {
		return monitorID;
	}

	/**
	 * This method is used by Symphony to set the monitor ID (Future purpose)
	 *
	 * @param monitorID This is the monitor ID to be set
	 */
	public void setMonitorID(int monitorID) {
		this.monitorID = monitorID;
	}

	/**
	 * Retrieves {@link #coolDownDelay}
	 *
	 * @return value of {@link #coolDownDelay}
	 */
	public String getCoolDownDelay() {
		return coolDownDelay;
	}

	/**
	 * Sets {@link #coolDownDelay} value
	 *
	 * @param coolDownDelay new value of {@link #coolDownDelay}
	 */
	public void setCoolDownDelay(String coolDownDelay) {
		this.coolDownDelay = coolDownDelay;
	}

	/**
	 * Retrieves {@link #pollingInterval}
	 *
	 * @return value of {@link #pollingInterval}
	 */
	public String getPollingInterval() {
		return pollingInterval;
	}

	/**
	 * Sets {@link #pollingInterval} value
	 *
	 * @param pollingInterval new value of {@link #pollingInterval}
	 */
	public void setPollingInterval(String pollingInterval) {
		this.pollingInterval = pollingInterval;
	}

	/**
	 * This method is recalled by Symphony to control a list of properties
	 *
	 * @param controllableProperties This is the list of properties to be controlled
	 * @return byte This returns the calculated xor checksum.
	 */
	@Override
	public void controlProperties(List<ControllableProperty> controllableProperties) {
		if (CollectionUtils.isEmpty(controllableProperties)) {
			throw new IllegalArgumentException("ControllableProperties can not be null or empty");
		}
		for (ControllableProperty p : controllableProperties) {
			try {
				controlProperty(p);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method is recalled by Symphony to control specific property
	 *
	 * @param controllableProperty This is the property to be controlled
	 */
	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
		reentrantLock.lock();
		try {
			if (localExtendedStatistics == null) {
				return;
			}
			condition.signal();
			isEmergencyDelivery = true;
			Map<String, String> stats = this.localExtendedStatistics.getStatistics();
			List<AdvancedControllableProperty> advancedControllableProperties = this.localExtendedStatistics.getControllableProperties();
			String value = String.valueOf(controllableProperty.getValue());
			String property = controllableProperty.getProperty();
			if (controllableProperty.getProperty().equalsIgnoreCase(controlProperties.power.name())) {
				if (controllableProperty.getValue().toString().equals(String.valueOf(LgWebOSConstants.NUMBER_ONE))) {
					powerON();
				} else if (controllableProperty.getValue().toString().equals(String.valueOf(LgWebOSConstants.ZERO))) {
					powerOFF();
				}
			} else {
				String propertyKey;
				String[] propertyList = property.split(LgWebOSConstants.HASH);
				String group = property + LgWebOSConstants.HASH;
				if (property.contains(LgWebOSConstants.HASH)) {
					propertyKey = propertyList[1];
					group = propertyList[0] + LgWebOSConstants.HASH;
				} else {
					propertyKey = property;
				}
				LgControllingCommand lgControllingCommand = LgControllingCommand.getCommandByName(propertyKey);
				switch (lgControllingCommand) {
					case VOLUME:
						String dataConvert = Integer.toHexString((int) Float.parseFloat(value));
						sendRequestToControlValue(commandNames.VOLUME, dataConvert.getBytes(StandardCharsets.UTF_8), false, value);
						String volumeValue = String.valueOf((int) Float.parseFloat(value));
						stats.put(group + LgWebOSConstants.VOLUME_VALUE, volumeValue);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.VOLUME, volumeValue);
						updateValueForTheControllableProperty(group + LgWebOSConstants.MUTE, String.valueOf(LgWebOSConstants.ZERO), stats, advancedControllableProperties);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.MUTE, String.valueOf(LgWebOSConstants.ZERO));
						break;
					case MUTE:
						String mute = LgWebOSConstants.UNMUTE_VALUE;
						if (String.valueOf(LgWebOSConstants.NUMBER_ONE).equals(value)) {
							mute = LgWebOSConstants.MUTE_VALUE;
						}
						sendRequestToControlValue(commandNames.MUTE, mute.getBytes(StandardCharsets.UTF_8), false, value);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.MUTE, String.valueOf(Integer.parseInt(mute)));
						break;
					case BACKLIGHT:
						dataConvert = Integer.toHexString((int) Float.parseFloat(value));
						sendRequestToControlValue(commandNames.BACKLIGHT, dataConvert.getBytes(StandardCharsets.UTF_8), false, value);
						String backlight = String.valueOf((int) Float.parseFloat(value));
						stats.put(group + LgWebOSConstants.BACKLIGHT_VALUE, backlight);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.BACKLIGHT, backlight);
						break;
					case INPUT_SELECT:
						dataConvert = InputSourceDropdown.getValueOfEnumByNameAndType(value, true);
						try {
							sendRequestToControlValue(commandNames.INPUT_SELECT, dataConvert.getBytes(StandardCharsets.UTF_8), true, value);
						} catch (Exception e) {
							dataConvert = InputSourceDropdown.getValueOfEnumByNameAndType(value, false);
							sendRequestToControlValue(commandNames.INPUT_SELECT, dataConvert.getBytes(StandardCharsets.UTF_8), true, value);
						}
						String inputSelect = getValueByName(LgWebOSConstants.INPUT_SELECT);
						stats.put(LgWebOSConstants.INPUT_SELECT, inputSelect);
						retrieveDataByCommandName(commandNames.SYNC_STATUS, commandNames.SYNC_STATUS_PARAM, lgControllingCommand);
						String signal = getValueByName(LgWebOSConstants.SIGNAL);
						addOrUpdateStatisticProperties(stats,LgWebOSConstants.SIGNAL, signal);
						addOrUpdateStatisticProperties(stats,group + LgWebOSConstants.SIGNAL, signal);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.SIGNAL, signal);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.INPUT_SELECT, inputSelect);
						break;
					case POWER_MANAGEMENT_MODE:
						dataConvert = LgWebOSConstants.BYTE_COMMAND + EnumTypeHandler.getValueOfEnumByName(PowerManagementModeEnum.class, value);
						sendRequestToControlValue(commandNames.POWER_MANAGEMENT_MODE, dataConvert.getBytes(StandardCharsets.UTF_8), true, value);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.POWER_MANAGEMENT_MODE, value);
						break;
					case DISPLAY_STAND_BY_MODE:
						dataConvert = EnumTypeHandler.getValueOfEnumByName(PowerManagement.class, value);
						sendRequestToControlValue(commandNames.DISPLAY_STAND_BY_MODE, dataConvert.getBytes(StandardCharsets.UTF_8), true, value);
						if (LgWebOSConstants.OFF.equalsIgnoreCase(value)) {
							stats.put(LgWebOSConstants.DISPLAY_STAND_BY_MODE, LgWebOSConstants.OFF);
						} else {
							stats.put(LgWebOSConstants.DISPLAY_STAND_BY_MODE, LgWebOSConstants.ON);
						}
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.DISPLAY_STAND_BY_MODE, value);
						break;
					case FAILOVER:
						String inputPriority = group + LgWebOSConstants.INPUT_PRIORITY;
						String priorityInput = group + LgWebOSConstants.PRIORITY_INPUT;
						String priorityInputDown = group + LgWebOSConstants.PRIORITY_DOWN;
						String priorityInputUp = group + LgWebOSConstants.PRIORITY_UP;
						int failOverStatus = Integer.parseInt(value);
						String failOverName = LgWebOSConstants.OFF;
						if (failOverStatus == LgWebOSConstants.ZERO) {
							sendRequestToControlValue(commandNames.FAILOVER, FailOverEnum.OFF.getValue().getBytes(StandardCharsets.UTF_8), false, value);
							//Remove all priority 0,1,2,3.etc, priorityInput, and inputPriority.
							stats.remove(inputPriority);
							advancedControllableProperties.removeIf(item -> item.getName().equals(inputPriority));

							stats.remove(priorityInput);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInput));

							stats.remove(priorityInputDown);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInputDown));

							stats.remove(priorityInputUp);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInputUp));

							if (cacheMapOfPriorityInputAndValue != null) {
								for (Entry<String, String> input : cacheMapOfPriorityInputAndValue.entrySet()) {
									stats.remove(group + input.getKey());
								}
							}
						} else if (failOverStatus == LgWebOSConstants.NUMBER_ONE) {
							sendRequestToControlValue(commandNames.FAILOVER, FailOverEnum.AUTO.getValue().getBytes(StandardCharsets.UTF_8), false, value);
							updateValueForTheControllableProperty(property, value, stats, advancedControllableProperties);

							AdvancedControllableProperty controlInputPriority = controlSwitch(stats, group + LgWebOSConstants.INPUT_PRIORITY, String.valueOf(LgWebOSConstants.ZERO),
									LgWebOSConstants.AUTO,
									LgWebOSConstants.MANUAL);
							checkControlPropertyBeforeAddNewProperty(controlInputPriority, advancedControllableProperties);
							failOverName = LgWebOSConstants.AUTO;
						}
						stats.put(LgWebOSConstants.FAILOVER_MODE, failOverName);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.FAILOVER_MODE, failOverName);
						break;
					case INPUT_PRIORITY:
						String failoverStatus = LgWebOSConstants.AUTO;
						if (String.valueOf(LgWebOSConstants.ZERO).equals(value)) {
							if (cacheMapOfPriorityInputAndValue != null) {
								for (Entry<String, String> input : cacheMapOfPriorityInputAndValue.entrySet()) {
									stats.remove(group + input.getKey());
								}
							}
							priorityInputDown = group + LgWebOSConstants.PRIORITY_DOWN;
							priorityInputUp = group + LgWebOSConstants.PRIORITY_UP;
							priorityInput = group + LgWebOSConstants.PRIORITY_INPUT;

							stats.remove(priorityInputDown);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInputDown));

							stats.remove(priorityInputUp);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInputUp));

							stats.remove(priorityInput);
							advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInput));

							sendRequestToControlValue(commandNames.FAILOVER, FailOverEnum.AUTO.getValue().getBytes(StandardCharsets.UTF_8), false, value);
						} else {
							failoverStatus = LgWebOSConstants.MANUAL;
							sendRequestToControlValue(commandNames.FAILOVER, FailOverEnum.MANUAL.getValue().getBytes(StandardCharsets.UTF_8), false, value);
							retrieveDataByCommandName(commandNames.FAILOVER_INPUT_LIST, commandNames.GET, lgControllingCommand);
							// failover is Manual
							AdvancedControllableProperty controlInputPriority = controlSwitch(stats, group + LgWebOSConstants.INPUT_PRIORITY, String.valueOf(LgWebOSConstants.NUMBER_ONE), LgWebOSConstants.AUTO,
									LgWebOSConstants.MANUAL);
							checkControlPropertyBeforeAddNewProperty(controlInputPriority, advancedControllableProperties);
							for (Entry<String, String> entry : cacheMapOfPriorityInputAndValue.entrySet()) {
								if (LgWebOSConstants.PLAY_VIA_URL.equalsIgnoreCase(entry.getValue())) {
									continue;
								}
								addOrUpdateStatisticProperties(stats,group + entry.getKey(), entry.getValue());
							}
							addOrUpdateStatisticProperties(stats,group + LgWebOSConstants.PRIORITY_UP, LgWebOSConstants.EMPTY_STRING);
							advancedControllableProperties.add(createButton(group + LgWebOSConstants.PRIORITY_UP, LgWebOSConstants.UP, LgWebOSConstants.PROCESSING, 0));

							addOrUpdateStatisticProperties(stats,group + LgWebOSConstants.PRIORITY_DOWN, LgWebOSConstants.EMPTY_STRING);
							advancedControllableProperties.add(createButton(group + LgWebOSConstants.PRIORITY_DOWN, LgWebOSConstants.DOWN, LgWebOSConstants.PROCESSING, 0));

							String[] inputSelected = cacheMapOfPriorityInputAndValue.values().stream().filter(item -> !item.equalsIgnoreCase(LgWebOSConstants.PLAY_VIA_URL)).collect(Collectors.toList())
									.toArray(new String[0]);

							String inputSourceDefaultValue = getValueByName(LgWebOSConstants.PRIORITY_INPUT);
							if (!LgWebOSConstants.NA.equals(inputSourceDefaultValue)) {
								Optional<Entry<String, String>> priorityInputOption = cacheMapOfPriorityInputAndValue.entrySet().stream().filter(item -> !item.getValue().equalsIgnoreCase(LgWebOSConstants.PLAY_VIA_URL))
										.findFirst();
								if (priorityInputOption.isPresent()) {
									inputSourceDefaultValue = priorityInputOption.get().getValue();
								}
								localCacheMapOfPropertyNameAndValue.put(LgWebOSConstants.PRIORITY_INPUT, inputSourceDefaultValue);
							}
							populatePriorityInput(stats, advancedControllableProperties, group, inputSourceDefaultValue);
							AdvancedControllableProperty controlInputSource = controlDropdown(stats, inputSelected, group + LgWebOSConstants.PRIORITY_INPUT, inputSourceDefaultValue);
							checkControlPropertyBeforeAddNewProperty(controlInputSource, advancedControllableProperties);
						}
						addOrUpdateStatisticProperties(stats,LgWebOSConstants.FAILOVER_MODE, failoverStatus);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.FAILOVER_MODE, failoverStatus);
						break;
					case PRIORITY_INPUT:
						localCacheMapOfPropertyNameAndValue.remove(LgWebOSConstants.PRIORITY_INPUT);
						localCacheMapOfPropertyNameAndValue.put(propertyKey, value);
						populatePriorityInput(stats, advancedControllableProperties, group, value);
						break;
					case PRIORITY_DOWN:
						String currentPriority = getValueByName(LgWebOSConstants.PRIORITY_INPUT);
						Map<String, String> newPriorityMap = new HashMap<>();
						Entry<String, String> priorityKey = cacheMapOfPriorityInputAndValue.entrySet().stream().filter(item -> item.getValue().equals(currentPriority)).findFirst().orElse(null);
						int len = cacheMapOfPriorityInputAndValue.size();
						for (int i = 1; i <= len; i++) {
							String currentKeyOfPriority = LgWebOSConstants.PRIORITY + i;
							String previousKeyOfPriority = LgWebOSConstants.PRIORITY + (i - 1);
							String nextKeyOfPriority = LgWebOSConstants.PRIORITY + (i + 1);
							if (currentPriority.equals(cacheMapOfPriorityInputAndValue.get(LgWebOSConstants.PRIORITY + len))) {
								break;
							} else {
								if (priorityKey.getKey().equals(currentKeyOfPriority)) {
									newPriorityMap.put(currentKeyOfPriority, cacheMapOfPriorityInputAndValue.get(nextKeyOfPriority));
								} else if (priorityKey.getKey().equals(previousKeyOfPriority)) {
									newPriorityMap.put(currentKeyOfPriority, cacheMapOfPriorityInputAndValue.get(previousKeyOfPriority));
								} else {
									newPriorityMap.put(currentKeyOfPriority, cacheMapOfPriorityInputAndValue.get(currentKeyOfPriority));
								}
							}
						}
						if (!newPriorityMap.isEmpty()) {
							if (StringUtils.isNullOrEmpty(newPriorityMap.get(LgWebOSConstants.PRIORITY + newPriorityMap.size()))) {
								newPriorityMap.remove(LgWebOSConstants.PRIORITY + newPriorityMap.size());
							}
							cacheMapOfPriorityInputAndValue = newPriorityMap;
						}
						StringBuilder stringBuilder = new StringBuilder();
						for (String values : cacheMapOfPriorityInputAndValue.values()) {
							if (StringUtils.isNullOrEmpty(values) || LgWebOSConstants.PLAY_VIA_URL.equalsIgnoreCase(values)) {
								continue;
							}
							stringBuilder.append(EnumTypeHandler.getValueOfEnumByName(FailOverInputSourceEnum.class, values));
							stringBuilder.append(LgWebOSConstants.SPACE);
						}
						sendRequestToControlValue(commandNames.FAILOVER_INPUT_LIST, stringBuilder.substring(0, stringBuilder.length() - 1).getBytes(StandardCharsets.UTF_8), false, value);
						for (Entry<String, String> input : cacheMapOfPriorityInputAndValue.entrySet()) {
							addOrUpdateStatisticProperties(stats,group + input.getKey(), input.getValue());
						}
						populatePriorityInput(stats, advancedControllableProperties, group, currentPriority);
						break;
					case PRIORITY_UP:
						currentPriority = getValueByName(LgWebOSConstants.PRIORITY_INPUT);
						newPriorityMap = new HashMap<>();
						priorityKey = cacheMapOfPriorityInputAndValue.entrySet().stream().filter(item -> item.getValue().equals(currentPriority)).findFirst().orElse(null);
						len = cacheMapOfPriorityInputAndValue.size();
						for (int i = 1; i <= len; i++) {
							String currentKeyOfPriority = LgWebOSConstants.PRIORITY + i;
							String previousKeyOfPriority = LgWebOSConstants.PRIORITY + (i - 1);
							String nextKeyOfPriority = LgWebOSConstants.PRIORITY + (i + 1);
							if (currentPriority.equals(cacheMapOfPriorityInputAndValue.get(LgWebOSConstants.PRIORITY + 1))) {
								break;
							} else {
								if (priorityKey.getKey().equals(nextKeyOfPriority)) {
									newPriorityMap.put(currentKeyOfPriority, cacheMapOfPriorityInputAndValue.get(nextKeyOfPriority));
								} else if (priorityKey.getKey().equals(currentKeyOfPriority)) {
									newPriorityMap.put(currentKeyOfPriority, cacheMapOfPriorityInputAndValue.get(previousKeyOfPriority));
								} else {
									newPriorityMap.put(currentKeyOfPriority, cacheMapOfPriorityInputAndValue.get(currentKeyOfPriority));
								}
							}
						}
						if (!newPriorityMap.isEmpty()) {
							if (StringUtils.isNullOrEmpty(newPriorityMap.get(LgWebOSConstants.PRIORITY + newPriorityMap.size()))) {
								newPriorityMap.remove(LgWebOSConstants.PRIORITY + newPriorityMap.size());
							}
							cacheMapOfPriorityInputAndValue = newPriorityMap;
						}
						stringBuilder = new StringBuilder();
						for (String values : cacheMapOfPriorityInputAndValue.values()) {
							if (StringUtils.isNullOrEmpty(values) || LgWebOSConstants.PLAY_VIA_URL.equalsIgnoreCase(values)) {
								continue;
							}
							stringBuilder.append(EnumTypeHandler.getValueOfEnumByName(FailOverInputSourceEnum.class, values));
							stringBuilder.append(LgWebOSConstants.SPACE);
						}
						sendRequestToControlValue(commandNames.FAILOVER_INPUT_LIST, stringBuilder.substring(0, stringBuilder.length() - 1).getBytes(StandardCharsets.UTF_8), false, value);
						for (Entry<String, String> entry : cacheMapOfPriorityInputAndValue.entrySet()) {
							if (LgWebOSConstants.PLAY_VIA_URL.equalsIgnoreCase(entry.getValue())) {
								continue;
							}
							stats.remove(group + entry.getKey());
							addOrUpdateStatisticProperties(stats,group + entry.getKey(), entry.getValue());
						}
						populatePriorityInput(stats, advancedControllableProperties, group, currentPriority);
						break;
					case TILE_MODE:
						String tileModeValue = LgWebOSConstants.OFF;
						String naturalModeKey = group + LgWebOSConstants.NATURAL_MODE;
						String naturalSize = group + LgWebOSConstants.NATURAL_SIZE;
						String tileID = group + LgWebOSConstants.TILE_MODE_ID;
						String paramTileMode;
						if (String.valueOf(LgWebOSConstants.ZERO).equals(value)) {
							stats.remove(naturalModeKey);
							stats.remove(naturalSize);
							stats.remove(tileID);
							advancedControllableProperties.removeIf(item -> item.getName().equals(naturalModeKey));
							paramTileMode = String.valueOf(LgWebOSConstants.ZERO) + LgWebOSConstants.ZERO;
							sendRequestToControlValue(commandNames.TILE_MODE_CONTROL, paramTileMode.getBytes(StandardCharsets.UTF_8), false, value);
						} else {
							tileModeValue = LgWebOSConstants.ON;
							retrieveDataByCommandName(commandNames.TILE_MODE_SETTINGS, commandNames.GET, lgControllingCommand);
							paramTileMode =
									Integer.toHexString(Integer.parseInt(stats.get(group + LgWebOSConstants.TILE_MODE_COLUMN))) + Integer.toHexString(Integer.parseInt(stats.get(group + LgWebOSConstants.TILE_MODE_ROW)));
							sendRequestToControlValue(commandNames.TILE_MODE_CONTROL, paramTileMode.getBytes(StandardCharsets.UTF_8), false, value);
							retrieveDataByCommandName(commandNames.NATURAL_MODE, commandNames.GET, lgControllingCommand);
							String naturalMode = getValueByName(LgWebOSConstants.NATURAL_MODE);
							if (!LgWebOSConstants.NA.equals(naturalMode)) {
								naturalMode = String.valueOf(LgWebOSConstants.ZERO == Integer.parseInt(naturalMode) ? 0 : 1);
							}
							AdvancedControllableProperty controlNaturalMode = controlSwitch(stats, group + LgWebOSConstants.NATURAL_MODE, naturalMode, LgWebOSConstants.OFF, LgWebOSConstants.ON);
							checkControlPropertyBeforeAddNewProperty(controlNaturalMode, advancedControllableProperties);
							if (String.valueOf(LgWebOSConstants.NUMBER_ONE).equals(naturalMode)) {
								retrieveDataByCommandName(commandNames.NATURAL_SIZE, commandNames.NATURAL_SIZE_PARAM, lgControllingCommand);
								addOrUpdateStatisticProperties(stats,group + LgWebOSConstants.NATURAL_SIZE, getValueByName(LgWebOSConstants.NATURAL_SIZE));
							}
							retrieveDataByCommandName(commandNames.TILE_ID, commandNames.GET, lgControllingCommand);
							String tileModeID = getValueByName(LgWebOSConstants.TILE_MODE_ID);
							if (!LgWebOSConstants.NA.equals(tileModeID)) {
								tileModeID = String.valueOf(Integer.parseInt(tileModeID));
							}
							addOrUpdateStatisticProperties(stats,group + LgWebOSConstants.TILE_MODE_ID, tileModeID);
						}
						addOrUpdateStatisticProperties(stats,LgWebOSConstants.TILE_MODE, tileModeValue);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TILE_MODE, tileModeValue);
						break;
					case NATURAL_MODE:
						naturalSize = group + LgWebOSConstants.NATURAL_SIZE;
						String paramNatural = String.valueOf(LgWebOSConstants.ZERO);
						if (String.valueOf(LgWebOSConstants.ZERO).equals(value)) {
							stats.remove(naturalSize);
							paramNatural = paramNatural + LgWebOSConstants.ZERO;
							sendRequestToControlValue(commandNames.NATURAL_MODE, paramNatural.getBytes(StandardCharsets.UTF_8), false, value);
						} else {
							paramNatural = paramNatural + LgWebOSConstants.NUMBER_ONE;
							sendRequestToControlValue(commandNames.NATURAL_MODE, paramNatural.getBytes(StandardCharsets.UTF_8), false, value);
							retrieveDataByCommandName(commandNames.NATURAL_SIZE, commandNames.NATURAL_SIZE_PARAM, lgControllingCommand);
							addOrUpdateStatisticProperties(stats,group + LgWebOSConstants.NATURAL_SIZE, getValueByName(LgWebOSConstants.NATURAL_SIZE));
						}
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.NATURAL_MODE, String.valueOf(Integer.parseInt(paramNatural)));
						break;
					case BALANCE:
						String balance = EnumTypeHandler.getValueOfEnumByName(Balance.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), balance.getBytes(StandardCharsets.UTF_8), true, value);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.BALANCE, balance);
						break;
					case BRIGHTNESS:
						int brightness = (int) Float.parseFloat(value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), Integer.toHexString(brightness).getBytes(StandardCharsets.UTF_8), false, value);
						stats.put(group + LgWebOSConstants.BRIGHTNESS_VALUE, String.valueOf(brightness));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.BRIGHTNESS_VALUE, String.valueOf(brightness));
						break;
					case COLOR_TEMPERATURE:
						int colorTemperature = (int) convertFromUIValueToApiValue(String.valueOf((int) Float.parseFloat(value)), LgWebOSConstants.COLOR_TEMPERATURE_UI_MAX_VALUE,
								LgWebOSConstants.COLOR_TEMPERATURE_UI_MIN_VALUE);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), Integer.toHexString(colorTemperature).getBytes(StandardCharsets.UTF_8), false, value);
						int newValue = (int) convertFromApiValueToUIValue(String.valueOf(colorTemperature), LgWebOSConstants.COLOR_TEMPERATURE_MAX_VALUE, LgWebOSConstants.COLOR_TEMPERATURE_MIN_VALUE);
						stats.put(group + LgWebOSConstants.COLOR_TEMPERATURE_VALUE, String.valueOf(newValue));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.COLOR_TEMPERATURE, String.valueOf(colorTemperature));
						break;
					case CONTRAST:
						int contrast = (int) Float.parseFloat(value);
						dataConvert = Integer.toHexString(contrast);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), dataConvert.getBytes(StandardCharsets.UTF_8), false, value);
						stats.put(group + LgWebOSConstants.CONTRAST_VALUE, String.valueOf(contrast));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.CONTRAST, String.valueOf(contrast));
						break;
					case SCREEN_COLOR:
						int screenColor = (int) Float.parseFloat(value);
						dataConvert = Integer.toHexString(screenColor);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), dataConvert.getBytes(StandardCharsets.UTF_8), false, value);
						stats.put(group + LgWebOSConstants.SCREEN_COLOR_VALUE, String.valueOf(screenColor));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.SCREEN_COLOR, String.valueOf(screenColor));
						break;
					case SHARPNESS:
						int sharpness = (int) Float.parseFloat(value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), Integer.toHexString(sharpness).getBytes(StandardCharsets.UTF_8), false, value);
						stats.put(group + LgWebOSConstants.SHARPNESS_VALUE, String.valueOf(sharpness));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.SHARPNESS, String.valueOf(sharpness));
						break;
					case TINT:
						String tint = EnumTypeHandler.getValueOfEnumByName(Tint.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), tint.getBytes(StandardCharsets.UTF_8), true, value);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TINT, tint);
						break;
					case ASPECT_RATIO:
						String aspectRatio = EnumTypeHandler.getValueOfEnumByName(AspectRatio.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), aspectRatio.getBytes(StandardCharsets.UTF_8), true, value);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.ASPECT_RATIO, value);
						break;
					case BRIGHTNESS_CONTROL:
						String brightnessSize = EnumTypeHandler.getValueOfEnumByName(BrightnessSize.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), brightnessSize.getBytes(StandardCharsets.UTF_8), true, value);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.BRIGHTNESS_CONTROL, value);
						break;
					case LANGUAGE:
						String language = EnumTypeHandler.getValueOfEnumByName(Language.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), language.getBytes(StandardCharsets.UTF_8), true, value);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.LANGUAGE, value);
						break;
					case SOUND_MODE:
						String soundMode = EnumTypeHandler.getValueOfEnumByName(SoundMode.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), soundMode.getBytes(StandardCharsets.UTF_8), true, value);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.SOUND_MODE, value);
						break;
					case PICTURE_MODE:
						String pictureMode = EnumTypeHandler.getValueOfEnumByName(PictureMode.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), pictureMode.getBytes(StandardCharsets.UTF_8), true, value);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.PICTURE_MODE, pictureMode);
						break;
					case POWER_ON_STATUS:
						String powerStatus = EnumTypeHandler.getValueOfEnumByName(PowerStatus.class, value);
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), powerStatus.getBytes(StandardCharsets.UTF_8), false, value);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.POWER_ON_STATUS, value);
						break;
					case NO_IR_POWER_OFF:
					case NO_SIGNAL_POWER_OFF:
						String powerValue = String.valueOf(LgWebOSConstants.ZERO) + LgWebOSConstants.ZERO;
						if (String.valueOf(LgWebOSConstants.NUMBER_ONE).equals(value)) {
							powerValue = String.valueOf(LgWebOSConstants.ZERO) + LgWebOSConstants.NUMBER_ONE;
						}
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), powerValue.getBytes(StandardCharsets.UTF_8), false, value);
						powerValue = Integer.parseInt(powerValue) == LgWebOSConstants.ZERO ? LgWebOSConstants.OFF : LgWebOSConstants.ON;
						if (lgControllingCommand.getName().equals(LgControllingCommand.NO_IR_POWER_OFF.getName())) {
							updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.NO_IR_POWER_OFF, powerValue);
						} else {
							updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.NO_SIGNAL_POWER_OFF, powerValue);
						}
						break;
					case REBOOT:
						String rebootValue = LgWebOSConstants.REBOOT_VALUE;
						sendRequestToControlValue(lgControllingCommand.getCommandNames(), rebootValue.getBytes(StandardCharsets.UTF_8), false, rebootValue);
						break;
					default:
						logger.debug(String.format("Property name %s doesn't support", propertyKey));
				}
			}
			updateValueForTheControllableProperty(property, value, stats, advancedControllableProperties);
		} finally {
			reentrantLock.unlock();
		}
	}

	/**
	 * This method is recalled by Symphony to get the list of statistics to be displayed
	 *
	 * @return List<Statistics> This return the list of statistics.
	 */
	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {

		ExtendedStatistics extendedStatistics = new ExtendedStatistics();
		List<AdvancedControllableProperty> advancedControllableProperties = new ArrayList<>();
		Map<String, String> statistics = new HashMap<>();
		Map<String, String> controlStatistics = new HashMap<>();
		Map<String, String> dynamicStatistics = new HashMap<>();
		reentrantLock.lock();
		try {
			if (localExtendedStatistics == null) {
				localExtendedStatistics = new ExtendedStatistics();
			}
			if (!isEmergencyDelivery) {
				convertCacheLifetime();
				convertDelayTime();
				convertConfigTimeout();
				convertPollingInterval();
				failedMonitor.clear();
				convertConfigManagement();
				//Use thread to fetching the monitoring and controlling data if connected with the device successfully
				populateMonitoringAndControllingData();
				//destroy channel after collecting all device's information
				destroyChannel();
				if (!isFirstInit && currentGetMultipleInPollingInterval < pollingIntervalInIntValue) {
					return Collections.singletonList(localExtendedStatistics);
				}
				//We will check if the value of localCachedFailedMonitor is greater than or equal to the value of currentCommandIndex,
				// as we have 36 properties by default and if all options are enabled, then we have a total of 37 properties.
				if (localCachedFailedMonitor >= currentCommandIndex && currentGetMultipleInPollingInterval == pollingIntervalInIntValue) {
					//Handle the case where all properties receive an error response and the case where 2 connections run in parallel to the device
					ping();
					isFirstInit = false;
					statistics.put(LgWebOSConstants.CONTROL_PROTOCOL_STATUS, LgWebOSConstants.UNAVAILABLE);
					countControlUnavailable++;
					if (countControlUnavailable > currentCachingLifetime) {
						localCacheMapOfPropertyNameAndValue.clear();
						localCachingLifeTimeOfMap.clear();
					}
				} else {
					isFirstInit = true;
					populateMonitoringData(statistics, dynamicStatistics);
					if (isConfigManagement) {
						populateControllingData(controlStatistics, advancedControllableProperties);
						extendedStatistics.setControllableProperties(advancedControllableProperties);
						statistics.putAll(controlStatistics);
					} else {
						statistics.remove(LgWebOSConstants.INPUT + LgWebOSConstants.HASH + LgWebOSConstants.SIGNAL);
					}
					//If failed for all monitoring data
					checkFailedCommand(statistics, advancedControllableProperties);
					extendedStatistics.setDynamicStatistics(dynamicStatistics);
					countControlUnavailable = 0;
				}
				extendedStatistics.setStatistics(statistics);
				extendedStatistics.setControllableProperties(advancedControllableProperties);
				localExtendedStatistics = extendedStatistics;
			}
			isEmergencyDelivery = false;
		} finally {
			reentrantLock.unlock();
		}
		return Collections.singletonList(localExtendedStatistics);
	}

	/**
	 * populate Priority input
	 *
	 * @param stats the stats are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 * @param groupName the groupName instance in GroupName#Key
	 * @param currentPriority the currentPriority is current value of priority property
	 */
	private void populatePriorityInput(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties, String groupName, String currentPriority) {
		String priorityInputUp = groupName + LgWebOSConstants.PRIORITY_UP;
		String priorityInputDown = groupName + LgWebOSConstants.PRIORITY_DOWN;
		stats.remove(priorityInputDown);
		advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInputDown));

		stats.remove(priorityInputUp);
		advancedControllableProperties.removeIf(item -> item.getName().equals(priorityInputUp));

		String priorityInputStart = cacheMapOfPriorityInputAndValue.get(LgWebOSConstants.PRIORITY + LgWebOSConstants.NUMBER_ONE);
		String priorityInputEnd = cacheMapOfPriorityInputAndValue.get(LgWebOSConstants.PRIORITY + cacheMapOfPriorityInputAndValue.size());
		if (StringUtils.isNullOrEmpty(priorityInputEnd)) {
			if (cacheMapOfPriorityInputAndValue.entrySet().stream().map(item -> item.getValue().equalsIgnoreCase(LgWebOSConstants.PLAY_VIA_URL)).findFirst().isPresent()) {
				priorityInputEnd = cacheMapOfPriorityInputAndValue.get(LgWebOSConstants.PRIORITY + (cacheMapOfPriorityInputAndValue.size() - 1));
			}
		}
		if (LgWebOSConstants.NA.equals(currentPriority) || cacheMapOfPriorityInputAndValue.isEmpty()) {
			addOrUpdateStatisticProperties(stats,groupName + LgWebOSConstants.PRIORITY_UP, LgWebOSConstants.NA);
			addOrUpdateStatisticProperties(stats,groupName + LgWebOSConstants.PRIORITY_DOWN, LgWebOSConstants.NA);
			return;
		}
		if (!currentPriority.equals(priorityInputStart) && !currentPriority.equals(priorityInputEnd)) {
			addOrUpdateStatisticProperties(stats,groupName + LgWebOSConstants.PRIORITY_UP, LgWebOSConstants.EMPTY_STRING);
			advancedControllableProperties.add(createButton(groupName + LgWebOSConstants.PRIORITY_UP, LgWebOSConstants.UP, LgWebOSConstants.PROCESSING, 0));

			addOrUpdateStatisticProperties(stats,groupName + LgWebOSConstants.PRIORITY_DOWN, LgWebOSConstants.EMPTY_STRING);
			advancedControllableProperties.add(createButton(groupName + LgWebOSConstants.PRIORITY_DOWN, LgWebOSConstants.DOWN, LgWebOSConstants.PROCESSING, 0));
		} else if (!StringUtils.isNullOrEmpty(priorityInputEnd) && !currentPriority.equals(priorityInputEnd)) {
			addOrUpdateStatisticProperties(stats,groupName + LgWebOSConstants.PRIORITY_DOWN, LgWebOSConstants.EMPTY_STRING);
			advancedControllableProperties.add(createButton(groupName + LgWebOSConstants.PRIORITY_DOWN, LgWebOSConstants.DOWN, LgWebOSConstants.PROCESSING, 0));
		} else {
			addOrUpdateStatisticProperties(stats,groupName + LgWebOSConstants.PRIORITY_UP, LgWebOSConstants.EMPTY_STRING);
			advancedControllableProperties.add(createButton(groupName + LgWebOSConstants.PRIORITY_UP, LgWebOSConstants.UP, LgWebOSConstants.PROCESSING, 0));
		}
	}

	/**
	 * populate monitoring and controlling data
	 * using 2 thread to get monitoring and controlling data
	 * Thread 1 fetches device monitoring and controlling data of each property's
	 * Thread 2 manages the timeout for each command sent by Thread 1
	 *
	 * if the response time is greater than the default timeout => Close connection and update failedMonitor
	 */
	private void populateMonitoringAndControllingData() throws InterruptedException {
		List<LgControllingCommand> commands = Arrays.stream(LgControllingCommand.values()).filter(item -> item.isMonitorType() || item.isControlType()).collect(Collectors.toList());
		Future manageTimeOutWorkerThread;
		int range = 0;
		if (currentGetMultipleInPollingInterval == pollingIntervalInIntValue - 1) {
			range = commands.size();
		}
		if (currentGetMultipleInPollingInterval == pollingIntervalInIntValue) {
			devicesExecutionPool.clear();
			currentGetMultipleInPollingInterval = 0;
			localCachedFailedMonitor = 0;
			range = 0;
			currentCommandIndex = 0;
		}
		int intervalIndex = currentGetMultipleInPollingInterval * commands.size() / pollingIntervalInIntValue;
		if (range == 0) {
			range = (currentGetMultipleInPollingInterval + LgWebOSConstants.NUMBER_ONE) * commands.size() / pollingIntervalInIntValue;
		}
		for (int i = intervalIndex; i < range; i++) {
			LgControllingCommand controllingCommand = commands.get(i);
			if (!isConfigManagement && controllingCommand.isControlType()) {
				continue;
			}
			if ((controllingCommand.isControlType() || controllingCommand.isMonitorType())) {
				commandNames param = getParamByCommandName(controllingCommand);
				//Count the number of requests in one polling cycle.
				currentCommandIndex++;
				if (param == null) {
					continue;
				}
				//Submit thread to fetch data
				devicesExecutionPool.add(fetchingDataExSer.submit(() -> {
					retrieveDataByCommandName(controllingCommand.getCommandNames(), param, controllingCommand);
				}));
				// The thread responsible for checking the ExecutorService waits until the defaultConfigTimeout period has elapsed.
				// If the Future is not completed at that point, the thread will cancel it
				manageTimeOutWorkerThread = timeoutManagementExSer.submit(() -> {
					int timeoutCount = 1;
					while (!devicesExecutionPool.get(devicesExecutionPool.size() - LgWebOSConstants.ORDINAL_TO_INDEX_CONVERT_FACTOR).isDone() && timeoutCount <= defaultConfigTimeout) {
						try {
							Thread.sleep(100);

							// The thread waits until the controlProperty() method successfully controls.
							if (isEmergencyDelivery) {
								condition.wait();
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						timeoutCount++;
					}
					//If the Future is not completed after the defaultConfigTimeout =>  update the failedMonitor and destroy the connection.
					int lastIndex = devicesExecutionPool.size() - 1;
					if (!devicesExecutionPool.get(lastIndex).isDone()) {
						failedMonitor.add(controllingCommand.getName());
						destroyChannel();
						devicesExecutionPool.get(lastIndex).cancel(true);
					}
				});
				try {
					while (!manageTimeOutWorkerThread.isDone()) {
						Thread.sleep(100);
					}
					manageTimeOutWorkerThread.get();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}
		logger.debug("Get data success with getMultipleTime: " + currentGetMultipleInPollingInterval);
		currentGetMultipleInPollingInterval++;
		localCachedFailedMonitor = localCachedFailedMonitor + failedMonitor.size();
	}

	/**
	 * Get param request by command name
	 *
	 * @param commandName is the command names value
	 * @return commandNames is param value
	 */
	private commandNames getParamByCommandName(LgControllingCommand commandName) {
		commandNames param = commandNames.GET;
		if (LgControllingCommand.NATURAL_MODE.getName().equals(commandName.getName())) {
			String value = getValueByName(LgWebOSConstants.TILE_MODE);
			if (LgWebOSConstants.NA.equals(value) || !LgWebOSConstants.ON.equals(value)) {
				return null;
			}
		} else if (LgControllingCommand.NATURAL_SIZE.getName().equals(commandName.getName())) {
			String value = getValueByName(LgWebOSConstants.NATURAL_MODE);
			if (LgWebOSConstants.NA.equals(value) || LgWebOSConstants.NUMBER_ONE != Integer.parseInt(value)) {
				return null;
			}
			param = commandNames.NATURAL_SIZE_PARAM;
		} else if (LgControllingCommand.NETWORK_SETTING.getName().equals(commandName.getName())) {
			param = commandNames.NETWORK_SETTING_PARAM;
		} else if (LgControllingCommand.POWER_MANAGEMENT_MODE.getName().equals(commandName.getName())) {
			param = commandNames.POWER_MANAGEMENT_MODE_PARAM;
		} else if (LgControllingCommand.SYNC_STATUS.getName().equals(commandName.getName())) {
			param = commandNames.SYNC_STATUS_PARAM;
		}
		return param;
	}

	/**
	 * Check failed command when retrieving data
	 *
	 * @param statistics the statistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void checkFailedCommand(Map<String, String> statistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		if (!failedMonitor.isEmpty()) {
			for (String value : failedMonitor) {
				Optional<Entry<String, Integer>> cachingCurrentValue = localCachingLifeTimeOfMap.entrySet().stream().filter(item -> item.getKey().equalsIgnoreCase(value)).findFirst();
				if (cachingCurrentValue.isPresent()) {
					int currentCachingLifetime = cachingCurrentValue.get().getValue();
					LgControllingCommand controllingCommand = LgControllingCommand.getCommandByName(value);
					if (currentCachingLifetime >= this.currentCachingLifetime) {
						localCachingLifeTimeOfMap.put(value.toLowerCase(Locale.ROOT), 0);
						switch (controllingCommand) {
							case NETWORK_SETTING:
								localCacheMapOfPropertyNameAndValue.remove(LgWebOSConstants.IP_ADDRESS);
								localCacheMapOfPropertyNameAndValue.remove(LgWebOSConstants.GATEWAY);
								localCacheMapOfPropertyNameAndValue.remove(LgWebOSConstants.SUBNET_MASK);
								localCacheMapOfPropertyNameAndValue.remove(LgWebOSConstants.DNS_SERVER);
								break;
							case TILE_MODE_SETTINGS:
								String groupName = LgWebOSConstants.TILE_MODE_SETTINGS + LgWebOSConstants.HASH;
								if (String.valueOf(LgWebOSConstants.NUMBER_ONE).equalsIgnoreCase(statistics.get(groupName + LgWebOSConstants.TILE_MODE))) {
									if (String.valueOf(LgWebOSConstants.NUMBER_ONE).equalsIgnoreCase(statistics.get(groupName + LgWebOSConstants.NATURAL_MODE))) {
										updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.NATURAL_SIZE, LgWebOSConstants.NA);
									}
									updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.NATURAL_MODE, LgWebOSConstants.NA);
									advancedControllableProperties.removeIf(item -> item.getName().equals(groupName + LgWebOSConstants.NATURAL_MODE));
								}
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TILE_MODE_COLUMN, LgWebOSConstants.NA);
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TILE_MODE_ROW, LgWebOSConstants.NA);
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TILE_MODE, LgWebOSConstants.NA);
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TILE_MODE_ID, LgWebOSConstants.NA);
								advancedControllableProperties.removeIf(item -> item.getName().equals(groupName + LgWebOSConstants.TILE_MODE));
								break;
							case NATURAL_MODE:
								groupName = LgWebOSConstants.TILE_MODE_SETTINGS + LgWebOSConstants.HASH;
								if (String.valueOf(LgWebOSConstants.NUMBER_ONE).equalsIgnoreCase(statistics.get(groupName + LgWebOSConstants.NATURAL_MODE))) {
									updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.NATURAL_SIZE, LgWebOSConstants.NA);
								}
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.NATURAL_MODE, LgWebOSConstants.NA);
								advancedControllableProperties.removeIf(item -> item.getName().equals(groupName + LgWebOSConstants.NATURAL_MODE));
								break;
							case DATE:
							case TIME:
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.DATE, LgWebOSConstants.NA);
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TIME, LgWebOSConstants.NA);
								localCacheMapOfPropertyNameAndValue.remove(value);
								break;
							default:
								Entry<String, String> property = statistics.entrySet().stream().filter((item) -> {
									String key = item.getKey();
									String[] group = key.split(LgWebOSConstants.HASH);
									String propertyName = group[0];
									if (key.contains(LgWebOSConstants.HASH)) {
										propertyName = group[1];
									}
									return propertyName.equals(value);
								}).findFirst().orElse(null);
								if (property != null) {
									advancedControllableProperties.removeIf(item -> item.getName().equals(property.getKey()));
									localCacheMapOfPropertyNameAndValue.remove(value);
								}
								break;
						}
					} else {
						currentCachingLifetime = currentCachingLifetime + 1;
						localCachingLifeTimeOfMap.replace(cachingCurrentValue.get().getKey(), currentCachingLifetime);
					}
				} else {
					localCachingLifeTimeOfMap.put(value.toLowerCase(Locale.ROOT), 1);
				}
			}
		}
	}

	/**
	 * This method is used to convert from api value string to ui value in integer
	 *
	 * @param apiCurrentValue current api value of property
	 * @param apiMaxValue max api value of property
	 * @param apiMinValue min api value of property
	 * @return float ui value
	 */
	private float convertFromApiValueToUIValue(String apiCurrentValue, int apiMaxValue, int apiMinValue) {
		if (StringUtils.isNotNullOrEmpty(apiCurrentValue) && !LgWebOSConstants.NA.equals(apiCurrentValue)) {
			int a = Integer.parseInt(apiCurrentValue) - apiMinValue;
			int b = apiMaxValue - apiMinValue;
			return a * (LgWebOSConstants.COLOR_TEMPERATURE_UI_MAX_VALUE - LgWebOSConstants.COLOR_TEMPERATURE_UI_MIN_VALUE) / b + LgWebOSConstants.COLOR_TEMPERATURE_UI_MIN_VALUE;
		}
		return 0f;
	}

	/**
	 * This method is used to convert from ui value in integer to api hex string
	 *
	 * @param currentValue current ui value of property
	 * @param maxValue max api value of property
	 * @param minValue min api value of property
	 * @return Float api current value
	 */
	private float convertFromUIValueToApiValue(String currentValue, int maxValue, int minValue) {
		if (StringUtils.isNotNullOrEmpty(currentValue) && !LgWebOSConstants.NA.equals(currentValue)) {
			int a = Integer.parseInt(currentValue) - minValue;
			int b = maxValue - minValue;
			return a * (LgWebOSConstants.COLOR_TEMPERATURE_MAX_VALUE - LgWebOSConstants.COLOR_TEMPERATURE_MIN_VALUE) / b + LgWebOSConstants.COLOR_TEMPERATURE_MIN_VALUE;
		}
		return 0f;
	}

	/**
	 * Update the value for the control metric
	 *
	 * @param property is name of the metric
	 * @param value the value is value of properties
	 * @param extendedStatistics list statistics property
	 * @param advancedControllableProperties the advancedControllableProperties is list AdvancedControllableProperties
	 */
	private void updateValueForTheControllableProperty(String property, String value, Map<String, String> extendedStatistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		if (!advancedControllableProperties.isEmpty()) {
			for (AdvancedControllableProperty advancedControllableProperty : advancedControllableProperties) {
				if (advancedControllableProperty.getName().equals(property)) {
					extendedStatistics.put(property, value);
					advancedControllableProperty.setValue(value);
					break;
				}
			}
		}
	}

	/**
	 * Control property name by value
	 *
	 * @param command the command is command to send the request
	 * @param param the param is parameter of the request
	 * @param isDropdownControl whether a particular control is a dropdown control or not
	 * @param value the value is value of property
	 */
	private void sendRequestToControlValue(commandNames command, byte[] param, boolean isDropdownControl, String value) {
		try {
			byte[] response = send(LgWebOSUtils.buildSendString((byte) monitorID, LgWebOSConstants.commands.get(command), param));
			String result = digestResponse(response, command).toString();
			if (LgWebOSConstants.NA.equals(result)) {
				throw new IllegalArgumentException("The response NG reply ");
			}
		} catch (Exception e) {
			if (isDropdownControl) {
				throw new IllegalArgumentException(
						String.format("The property name %s is not supported. The current model does not support control with the value %s, and the device has responded with an error.", command.name(), value),
						e);
			}
			throw new IllegalArgumentException(String.format("Can't control property %s. The device has responded with an error.", command.name()), e);
		}
	}

	/**
	 * Populate controlling data
	 *
	 * @param controlStatistics the controlStatistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void populateControllingData(Map<String, String> controlStatistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		retrieveFailOverGroupValue(controlStatistics, advancedControllableProperties);
		retrieveDisplayAndSoundGroupValue(controlStatistics, advancedControllableProperties);
		retrieveTileModeGroupValue(controlStatistics, advancedControllableProperties);
		for (LgControllingCommand lgControllingCommand : LgControllingCommand.values()) {
			populateDisplayPropertyGroup(lgControllingCommand, controlStatistics, advancedControllableProperties);
		}
	}

	/**
	 * check Control Property Before Add New Property
	 *
	 * @param advancedControllableProperty the advancedControllableProperty is AdvancedControllableProperty instance
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void checkControlPropertyBeforeAddNewProperty(AdvancedControllableProperty advancedControllableProperty, List<AdvancedControllableProperty> advancedControllableProperties) {
		if (advancedControllableProperty != null) {
			advancedControllableProperties.add(advancedControllableProperty);
		}
	}

	/**
	 * Populate controlling property
	 *
	 * @param lgControllingCommand the lgControllingCommand is LgControllingCommand enum instance
	 * @param controlStatistics the controlStatistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void populateDisplayPropertyGroup(LgControllingCommand lgControllingCommand, Map<String, String> controlStatistics,
			List<AdvancedControllableProperty> advancedControllableProperties) {
		String displayGroupName = LgWebOSConstants.DISPLAY + LgWebOSConstants.HASH;
		String soundGroupName = LgWebOSConstants.SOUND + LgWebOSConstants.HASH;
		String powerManagementGroupName = LgWebOSConstants.POWER_MANAGEMENT + LgWebOSConstants.HASH;
		String value;
		switch (lgControllingCommand) {
			case POWER:
				value = getValueByName(LgWebOSConstants.POWER);
				if (!LgWebOSConstants.NA.equals(value)) {
					value = String.valueOf(LgWebOSConstants.ON.equalsIgnoreCase(value) ? 1 : 0);
				}
				AdvancedControllableProperty controlPower = controlSwitch(controlStatistics, LgWebOSConstants.POWER, value, LgWebOSConstants.OFF, LgWebOSConstants.ON);
				checkControlPropertyBeforeAddNewProperty(controlPower, advancedControllableProperties);

				addOrUpdateStatisticProperties(controlStatistics,LgWebOSConstants.REBOOT, LgWebOSConstants.EMPTY_STRING);
				advancedControllableProperties.add(createButton(LgWebOSConstants.REBOOT, LgWebOSConstants.REBOOT, LgWebOSConstants.PROCESSING, 0));
				break;
			case ASPECT_RATIO:
				value = getValueByName(LgWebOSConstants.ASPECT_RATIO);
				String[] aspectRatioDropdown = EnumTypeHandler.getEnumNames(AspectRatio.class);
				AdvancedControllableProperty aspectRatioControl = controlDropdown(controlStatistics, aspectRatioDropdown, displayGroupName + LgWebOSConstants.ASPECT_RATIO, value);
				checkControlPropertyBeforeAddNewProperty(aspectRatioControl, advancedControllableProperties);
				break;
			case BRIGHTNESS_CONTROL:
				value = getValueByName(LgWebOSConstants.BRIGHTNESS_CONTROL);
				String[] brightnessSizeDropdown = EnumTypeHandler.getEnumNames(BrightnessSize.class);
				AdvancedControllableProperty brightnessSizeControl = controlDropdown(controlStatistics, brightnessSizeDropdown, displayGroupName + LgWebOSConstants.BRIGHTNESS_CONTROL,
						value);
				checkControlPropertyBeforeAddNewProperty(brightnessSizeControl, advancedControllableProperties);
				break;
			case CONTRAST:
				value = getValueByName(LgWebOSConstants.CONTRAST);
				getDefaultValueForNullValue(value, controlStatistics, displayGroupName + LgWebOSConstants.CONTRAST_VALUE);
				AdvancedControllableProperty controlContrast = createControlSlider(displayGroupName + LgWebOSConstants.CONTRAST, value, controlStatistics, String.valueOf(LgWebOSConstants.ZERO),
						String.valueOf(LgWebOSConstants.MAX_RANGE_CONTRAST));
				checkControlPropertyBeforeAddNewProperty(controlContrast, advancedControllableProperties);
				break;
			case PICTURE_MODE:
				value = getValueByName(LgWebOSConstants.PICTURE_MODE);
				String[] pictureModeDropdown = EnumTypeHandler.getEnumNames(PictureMode.class);
				AdvancedControllableProperty pictureModeControl = controlDropdown(controlStatistics, pictureModeDropdown, displayGroupName + LgWebOSConstants.PICTURE_MODE, value);
				checkControlPropertyBeforeAddNewProperty(pictureModeControl, advancedControllableProperties);
				break;
			case BRIGHTNESS:
				value = getValueByName(LgWebOSConstants.BRIGHTNESS);
				getDefaultValueForNullValue(value, controlStatistics, displayGroupName + LgWebOSConstants.BRIGHTNESS_VALUE);
				AdvancedControllableProperty controlBrightness = createControlSlider(displayGroupName + LgWebOSConstants.BRIGHTNESS, value, controlStatistics, String.valueOf(LgWebOSConstants.ZERO),
						String.valueOf(LgWebOSConstants.MAX_RANGE_BRIGHTNESS));
				checkControlPropertyBeforeAddNewProperty(controlBrightness, advancedControllableProperties);
				break;
			case SHARPNESS:
				value = getValueByName(LgWebOSConstants.SHARPNESS);
				getDefaultValueForNullValue(value, controlStatistics, displayGroupName + LgWebOSConstants.SHARPNESS_VALUE);
				AdvancedControllableProperty controlSharpness = createControlSlider(displayGroupName + LgWebOSConstants.SHARPNESS, value, controlStatistics, String.valueOf(LgWebOSConstants.ZERO),
						String.valueOf(LgWebOSConstants.MAX_RANGE_SHARPNESS));
				checkControlPropertyBeforeAddNewProperty(controlSharpness, advancedControllableProperties);
				break;
			case SCREEN_COLOR:
				value = getValueByName(LgWebOSConstants.SCREEN_COLOR);
				getDefaultValueForNullValue(value, controlStatistics, displayGroupName + LgWebOSConstants.SCREEN_COLOR_VALUE);
				AdvancedControllableProperty controlScreenColor = createControlSlider(displayGroupName + LgWebOSConstants.SCREEN_COLOR, value, controlStatistics, String.valueOf(LgWebOSConstants.ZERO),
						String.valueOf(LgWebOSConstants.MAX_RANGE_SCREEN_COLOR));
				checkControlPropertyBeforeAddNewProperty(controlScreenColor, advancedControllableProperties);
				break;
			case TINT:
				value = getValueByName(LgWebOSConstants.TINT);
				String[] tintDropdown = EnumTypeHandler.getEnumNames(Tint.class);
				String tintValue = EnumTypeHandler.getNameEnumByValue(Tint.class, value);
				AdvancedControllableProperty controlTint = controlDropdown(controlStatistics, tintDropdown, displayGroupName + LgWebOSConstants.TINT, tintValue);
				checkControlPropertyBeforeAddNewProperty(controlTint, advancedControllableProperties);
				break;
			case COLOR_TEMPERATURE:
				value = getValueByName(LgWebOSConstants.COLOR_TEMPERATURE);
				Float colorTemperatureValue = convertFromApiValueToUIValue(value, LgWebOSConstants.COLOR_TEMPERATURE_MAX_VALUE, LgWebOSConstants.COLOR_TEMPERATURE_MIN_VALUE);
				if (colorTemperatureValue != 0f) {
					value = String.valueOf((int) Float.parseFloat(String.valueOf(colorTemperatureValue)));
				}
				getDefaultValueForNullValue(value, controlStatistics, displayGroupName + LgWebOSConstants.COLOR_TEMPERATURE_VALUE);
				AdvancedControllableProperty controlColorTemperature = createControlSlider(displayGroupName + LgWebOSConstants.COLOR_TEMPERATURE, value, controlStatistics,
						String.valueOf(LgWebOSConstants.MIN_RANGE_COLOR_TEMPERATURE),
						String.valueOf(LgWebOSConstants.MAX_RANGE_COLOR_TEMPERATURE));
				checkControlPropertyBeforeAddNewProperty(controlColorTemperature, advancedControllableProperties);
				break;
			case BALANCE:
				value = getValueByName(LgWebOSConstants.BALANCE);
				String[] balanceDropdown = EnumTypeHandler.getEnumNames(Balance.class);
				String balanceValue = EnumTypeHandler.getNameEnumByValue(Balance.class, value);
				AdvancedControllableProperty controlBalance = controlDropdown(controlStatistics, balanceDropdown, soundGroupName + LgWebOSConstants.BALANCE, balanceValue);
				checkControlPropertyBeforeAddNewProperty(controlBalance, advancedControllableProperties);
				break;
			case SOUND_MODE:
				value = getValueByName(LgWebOSConstants.SOUND_MODE);
				String[] soundModeDropdown = EnumTypeHandler.getEnumNames(SoundMode.class);
				AdvancedControllableProperty soundModeControl = controlDropdown(controlStatistics, soundModeDropdown, soundGroupName + LgWebOSConstants.SOUND_MODE, value);
				checkControlPropertyBeforeAddNewProperty(soundModeControl, advancedControllableProperties);
				break;
			case LANGUAGE:
				value = getValueByName(LgWebOSConstants.LANGUAGE);
				String[] languageDropdown = EnumTypeHandler.getEnumNames(Language.class);
				AdvancedControllableProperty languageControl = controlDropdown(controlStatistics, languageDropdown, LgWebOSConstants.LANGUAGE, value);
				checkControlPropertyBeforeAddNewProperty(languageControl, advancedControllableProperties);
				break;
			case POWER_ON_STATUS:
				value = getValueByName(LgWebOSConstants.POWER_ON_STATUS);
				String[] powerDropdown = EnumTypeHandler.getEnumNames(PowerStatus.class);
				AdvancedControllableProperty powerControl = controlDropdown(controlStatistics, powerDropdown, powerManagementGroupName + LgWebOSConstants.POWER_ON_STATUS, value);
				checkControlPropertyBeforeAddNewProperty(powerControl, advancedControllableProperties);
				break;
			case NO_SIGNAL_POWER_OFF:
				value = getValueByName(LgWebOSConstants.NO_SIGNAL_POWER_OFF);
				if (!LgWebOSConstants.NA.equals(value)) {
					value = String.valueOf(LgWebOSConstants.ON.equalsIgnoreCase(value) ? LgWebOSConstants.NUMBER_ONE : LgWebOSConstants.ZERO);
				}
				AdvancedControllableProperty controlNoSignalPower = controlSwitch(controlStatistics, powerManagementGroupName + LgWebOSConstants.NO_SIGNAL_POWER_OFF, value,
						LgWebOSConstants.OFF,
						LgWebOSConstants.ON);
				checkControlPropertyBeforeAddNewProperty(controlNoSignalPower, advancedControllableProperties);
				break;
			case NO_IR_POWER_OFF:
				value = getValueByName(LgWebOSConstants.NO_IR_POWER_OFF);
				if (!LgWebOSConstants.NA.equalsIgnoreCase(value)) {
					value = String.valueOf(LgWebOSConstants.ON.equalsIgnoreCase(value) ? LgWebOSConstants.NUMBER_ONE : LgWebOSConstants.ZERO);
				}
				AdvancedControllableProperty controlNoIRPower = controlSwitch(controlStatistics, powerManagementGroupName + LgWebOSConstants.NO_IR_POWER_OFF, value, LgWebOSConstants.OFF,
						LgWebOSConstants.ON);
				checkControlPropertyBeforeAddNewProperty(controlNoIRPower, advancedControllableProperties);
				break;
			default:
				logger.debug("the command name isn't supported" + lgControllingCommand.getName());
				break;
		}
	}

	/**
	 * Get default value for null value
	 *
	 * @param value the value is value of property
	 * @param stats the stats are list of statistics
	 * @param property the property is property name
	 */
	private void getDefaultValueForNullValue(String value, Map<String, String> stats, String property) {
		if (!StringUtils.isNullOrEmpty(value) && !LgWebOSConstants.NA.equals(value)) {
			stats.put(property, value);
		}
	}

	/**
	 * Populate monitoring data
	 *
	 * @param statistics the statistics are list of statistics
	 * @param dynamicStatistics the dynamicStatistics are list of dynamicStatistics
	 */
	private void populateMonitoringData(Map<String, String> statistics, Map<String, String> dynamicStatistics) {
		//The flow code is handled in the previous version
		String inputGroupName = LgWebOSConstants.INPUT + LgWebOSConstants.HASH;
		String signal = getValueByName(LgWebOSConstants.SIGNAL);
		if (LgWebOSConstants.NA.equals(signal)) {
			signal = syncStatusNames.NO_SYNC.name();
		}
		addOrUpdateStatisticProperties(statistics, LgWebOSConstants.SIGNAL, signal);
		addOrUpdateStatisticProperties(statistics, inputGroupName + LgWebOSConstants.SIGNAL, signal);
		String inputSignal = getValueByName(LgWebOSConstants.INPUT_SELECT);
		addOrUpdateStatisticProperties(statistics, LgWebOSConstants.INPUT_SELECT, inputSignal);

		String fan = getValueByName(LgWebOSConstants.FAN);
		if (LgWebOSConstants.NA.equals(fan)) {
			fan = fanStatusNames.NO_FAN.name();
		}
		addOrUpdateStatisticProperties(statistics, LgWebOSConstants.FAN, fan);

		String temperatureValue = getValueByName(LgWebOSConstants.TEMPERATURE);
		if (!historicalProperties.isEmpty() && historicalProperties.contains(LgWebOSConstants.TEMPERATURE)) {
			addOrUpdateStatisticProperties(dynamicStatistics,LgWebOSConstants.TEMPERATURE, temperatureValue);
		} else {
			addOrUpdateStatisticProperties(statistics,LgWebOSConstants.TEMPERATURE, temperatureValue);
		}
		//new feature retrieve device dashboard
		String software = getValueByName(LgWebOSConstants.SOFTWARE_VERSION);
		String failover = getValueByName(LgWebOSConstants.FAILOVER_MODE);
		String tileMode = getValueByName(LgWebOSConstants.TILE_MODE);
		String serialNumber = getValueByName(LgWebOSConstants.SERIAL_NUMBER);
		String standbyMode = getValueByName(LgWebOSConstants.DISPLAY_STAND_BY_MODE);
		String date = getValueByName(LgWebOSConstants.DATE);
		String time = getValueByName(LgWebOSConstants.TIME);

		String dateTimeValue = String.format("%s %s", date, time);
		if (LgWebOSConstants.NA.equals(date) || LgWebOSConstants.NA.equals(time)) {
			dateTimeValue = LgWebOSConstants.NA;
		}
		if (!LgWebOSConstants.OFF.equals(standbyMode) && !LgWebOSConstants.NA.equals(standbyMode)) {
			standbyMode = LgWebOSConstants.ON;
		}
		addOrUpdateStatisticProperties(statistics,LgWebOSConstants.DATE_TIME, dateTimeValue);
		addOrUpdateStatisticProperties(statistics,LgWebOSConstants.FAILOVER_MODE, failover);
		addOrUpdateStatisticProperties(statistics,LgWebOSConstants.SOFTWARE_VERSION, software);
		addOrUpdateStatisticProperties(statistics,LgWebOSConstants.TILE_MODE, tileMode);
		addOrUpdateStatisticProperties(statistics,LgWebOSConstants.SERIAL_NUMBER, serialNumber);
		addOrUpdateStatisticProperties(statistics,LgWebOSConstants.DISPLAY_STAND_BY_MODE, standbyMode);

		//populate Network information
		String ipAddress = getValueByName(LgWebOSConstants.IP_ADDRESS);
		String gateway = getValueByName(LgWebOSConstants.GATEWAY);
		String subnetMask = getValueByName(LgWebOSConstants.SUBNET_MASK);
		String dnsServer = getValueByName(LgWebOSConstants.DNS_SERVER);
		addOrUpdateStatisticProperties(statistics,LgWebOSConstants.GATEWAY, gateway);
		addOrUpdateStatisticProperties(statistics,LgWebOSConstants.SUBNET_MASK, subnetMask);
		addOrUpdateStatisticProperties(statistics,LgWebOSConstants.DNS_SERVER, dnsServer);
		addOrUpdateStatisticProperties(statistics,LgWebOSConstants.IP_ADDRESS, ipAddress);
	}

	/**
	 * Retrieve tile mode group value
	 *
	 * @param controlStatistics the controlStatistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void retrieveTileModeGroupValue(Map<String, String> controlStatistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		String groupName = LgWebOSConstants.TILE_MODE_SETTINGS + LgWebOSConstants.HASH;
		//populate tile settings
		String tileMode = getValueByName(LgWebOSConstants.TILE_MODE);
		String tileModeValue = LgWebOSConstants.NA;
		if (!LgWebOSConstants.NA.equals(tileMode)) {
			tileModeValue = String.valueOf(LgWebOSConstants.ON.equalsIgnoreCase(tileMode) ? 1 : 0);
		}
		AdvancedControllableProperty controlTileMode = controlSwitch(controlStatistics, groupName + LgWebOSConstants.TILE_MODE, tileModeValue, LgWebOSConstants.OFF, LgWebOSConstants.ON);
		checkControlPropertyBeforeAddNewProperty(controlTileMode, advancedControllableProperties);

		addOrUpdateStatisticProperties(controlStatistics,groupName + LgWebOSConstants.TILE_MODE_COLUMN, getValueByName(LgWebOSConstants.TILE_MODE_COLUMN));
		addOrUpdateStatisticProperties(controlStatistics,groupName + LgWebOSConstants.TILE_MODE_ROW, getValueByName(LgWebOSConstants.TILE_MODE_ROW));

		//NaturalMode
		if (LgWebOSConstants.ON.equals(tileMode)) {

			//Retrieve Tile ID
			String tileModeID = getValueByName(LgWebOSConstants.TILE_MODE_ID);
			if (!LgWebOSConstants.NA.equals(tileModeID)) {
				tileModeID = String.valueOf(Integer.parseInt(tileModeID));
			}
			addOrUpdateStatisticProperties(controlStatistics,groupName + LgWebOSConstants.TILE_MODE_ID, tileModeID);
			String naturalMode = getValueByName(LgWebOSConstants.NATURAL_MODE);
			if (!LgWebOSConstants.NA.equals(naturalMode)) {
				naturalMode = String.valueOf(LgWebOSConstants.ZERO == Integer.parseInt(naturalMode) ? 0 : 1);
			}
			AdvancedControllableProperty controlNaturalMode = controlSwitch(controlStatistics, groupName + LgWebOSConstants.NATURAL_MODE, naturalMode, LgWebOSConstants.OFF, LgWebOSConstants.ON);
			checkControlPropertyBeforeAddNewProperty(controlNaturalMode, advancedControllableProperties);
			if (String.valueOf(LgWebOSConstants.NUMBER_ONE).equals(naturalMode)) {
				addOrUpdateStatisticProperties(controlStatistics,groupName + LgWebOSConstants.NATURAL_SIZE, getValueByName(LgWebOSConstants.NATURAL_SIZE));
			}
		}
	}

	/**
	 * Retrieve fail over group value
	 *
	 * @param controlStatistics the controlStatistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void retrieveFailOverGroupValue(Map<String, String> controlStatistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		String groupName = LgWebOSConstants.FAILOVER + LgWebOSConstants.HASH;
		String failOver = getValueByName(LgWebOSConstants.FAILOVER_MODE);
		int failOverValue = LgWebOSConstants.NUMBER_ONE;
		if (LgWebOSConstants.NA.equals(failOver)) {
			addOrUpdateStatisticProperties(controlStatistics,groupName + LgWebOSConstants.INPUT_PRIORITY, failOver);
			return;
		}
		if (LgWebOSConstants.OFF.equalsIgnoreCase(failOver)) {
			failOverValue = LgWebOSConstants.ZERO;
		} else if (LgWebOSConstants.AUTO.equalsIgnoreCase(failOver)) {
			AdvancedControllableProperty controlInputPriority = controlSwitch(controlStatistics, groupName + LgWebOSConstants.INPUT_PRIORITY, String.valueOf(LgWebOSConstants.ZERO), LgWebOSConstants.AUTO,
					LgWebOSConstants.MANUAL);
			checkControlPropertyBeforeAddNewProperty(controlInputPriority, advancedControllableProperties);
		} else {
			// failover is Manual
			AdvancedControllableProperty controlInputPriority = controlSwitch(controlStatistics, groupName + LgWebOSConstants.INPUT_PRIORITY, String.valueOf(LgWebOSConstants.NUMBER_ONE), LgWebOSConstants.AUTO,
					LgWebOSConstants.MANUAL);
			checkControlPropertyBeforeAddNewProperty(controlInputPriority, advancedControllableProperties);
			for (Entry<String, String> entry : cacheMapOfPriorityInputAndValue.entrySet()) {
				if (LgWebOSConstants.PLAY_VIA_URL.equalsIgnoreCase(entry.getValue())) {
					continue;
				}
				addOrUpdateStatisticProperties(controlStatistics,groupName + entry.getKey(), entry.getValue());
			}
			String[] inputSelected = cacheMapOfPriorityInputAndValue.values().stream().filter(item -> !item.equalsIgnoreCase(LgWebOSConstants.PLAY_VIA_URL)).collect(Collectors.toList())
					.toArray(new String[0]);
			String priorityInput = getValueByName(LgWebOSConstants.PRIORITY_INPUT);
			if (LgWebOSConstants.NA.equals(priorityInput)) {
				Optional<Entry<String, String>> priorityInputOption = cacheMapOfPriorityInputAndValue.entrySet().stream().filter(item -> !item.getValue().equalsIgnoreCase(LgWebOSConstants.PLAY_VIA_URL))
						.findFirst();
				if (priorityInputOption.isPresent()) {
					priorityInput = priorityInputOption.get().getValue();
				}
				localCacheMapOfPropertyNameAndValue.put(LgWebOSConstants.PRIORITY_INPUT, priorityInput);
			}
			populatePriorityInput(controlStatistics, advancedControllableProperties, groupName, priorityInput);
			AdvancedControllableProperty controlInputSource = controlDropdown(controlStatistics, inputSelected, groupName + LgWebOSConstants.PRIORITY_INPUT, priorityInput);
			checkControlPropertyBeforeAddNewProperty(controlInputSource, advancedControllableProperties);
		}
		AdvancedControllableProperty controlFailover = controlSwitch(controlStatistics, groupName + LgWebOSConstants.FAILOVER_MODE, String.valueOf(failOverValue), LgWebOSConstants.OFF, LgWebOSConstants.ON);
		checkControlPropertyBeforeAddNewProperty(controlFailover, advancedControllableProperties);
	}

	/**
	 * Retrieve display and sound group value
	 *
	 * @param statistics the statistics are list of statistics
	 * @param advancedControllableProperties the advancedControllableProperties is advancedControllableProperties instance
	 */
	private void retrieveDisplayAndSoundGroupValue(Map<String, String> statistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		String displayGroupName = LgWebOSConstants.DISPLAY + LgWebOSConstants.HASH;
		String soundGroupName = LgWebOSConstants.SOUND + LgWebOSConstants.HASH;
		String inputGroupName = LgWebOSConstants.INPUT + LgWebOSConstants.HASH;
		String powerManagementGroupName = LgWebOSConstants.POWER_MANAGEMENT + LgWebOSConstants.HASH;
		String backlight = getValueByName(LgWebOSConstants.BACKLIGHT);
		String mute = getValueByName(LgWebOSConstants.MUTE);
		String volume = getValueByName(LgWebOSConstants.VOLUME);

		getDefaultValueForNullValue(backlight, statistics, displayGroupName + LgWebOSConstants.BACKLIGHT_VALUE);
		AdvancedControllableProperty controlBacklight = createControlSlider(displayGroupName + LgWebOSConstants.BACKLIGHT, backlight, statistics, String.valueOf(LgWebOSConstants.ZERO),
				String.valueOf(LgWebOSConstants.MAX_RANGE_BACKLIGHT));
		checkControlPropertyBeforeAddNewProperty(controlBacklight, advancedControllableProperties);

		if (!LgWebOSConstants.NA.equals(mute)) {
			mute = String.valueOf(Integer.parseInt(mute) == 0 ? LgWebOSConstants.NUMBER_ONE : LgWebOSConstants.ZERO);
		}
		AdvancedControllableProperty controlMute = controlSwitch(statistics, soundGroupName + LgWebOSConstants.MUTE, mute, LgWebOSConstants.OFF, LgWebOSConstants.ON);
		checkControlPropertyBeforeAddNewProperty(controlMute, advancedControllableProperties);

		getDefaultValueForNullValue(volume, statistics, soundGroupName + LgWebOSConstants.VOLUME_VALUE);
		AdvancedControllableProperty controlVolume = createControlSlider(soundGroupName + LgWebOSConstants.VOLUME, volume, statistics, String.valueOf(LgWebOSConstants.ZERO),
				String.valueOf(LgWebOSConstants.MAX_RANGE_VOLUME));
		checkControlPropertyBeforeAddNewProperty(controlVolume, advancedControllableProperties);

		String inputSourceValue = getValueByName(LgWebOSConstants.INPUT_SELECT);
		if (!LgWebOSConstants.NA.equals(inputSourceValue)) {
			cacheMapOfPriorityInputAndValue.put(LgWebOSConstants.PLAY_VIA_URL, LgWebOSConstants.PLAY_VIA_URL);
			String[] inputDropdown = cacheMapOfPriorityInputAndValue.values().stream().sorted().collect(Collectors.toList()).toArray(new String[0]);
			AdvancedControllableProperty controlInputSource = controlDropdown(statistics, inputDropdown, inputGroupName + LgWebOSConstants.INPUT_SELECT, inputSourceValue);
			checkControlPropertyBeforeAddNewProperty(controlInputSource, advancedControllableProperties);
			statistics.put(LgWebOSConstants.INPUT_SELECT, inputSourceValue);
		}
		String[] pmdDropdown = EnumTypeHandler.getEnumNames(PowerManagement.class);
		AdvancedControllableProperty controlPMD = controlDropdown(statistics, pmdDropdown, powerManagementGroupName + LgWebOSConstants.DISPLAY_STAND_BY_MODE,
				getValueByName(LgWebOSConstants.DISPLAY_STAND_BY_MODE));
		checkControlPropertyBeforeAddNewProperty(controlPMD, advancedControllableProperties);

		String pmdModeValue = getValueByName(LgWebOSConstants.POWER_MANAGEMENT_MODE);

		String[] pmdModeDropdown = EnumTypeHandler.getEnumNames(PowerManagementModeEnum.class);
		AdvancedControllableProperty controlPMDMode = controlDropdown(statistics, pmdModeDropdown, powerManagementGroupName + LgWebOSConstants.POWER_MANAGEMENT_MODE, pmdModeValue);
		checkControlPropertyBeforeAddNewProperty(controlPMDMode, advancedControllableProperties);
	}

	/**
	 * Get value of property by name
	 *
	 * @param name the name is name if property
	 * @return String is value of property or NA if the value is null or N/A
	 */
	private String getValueByName(String name) {
		String value = localCacheMapOfPropertyNameAndValue.get(name);
		if (StringUtils.isNullOrEmpty(value) || LgWebOSConstants.NA.equals(value)) {
			return LgWebOSConstants.NA;
		}
		return value;
	}

	/**
	 * Retrieve data by command name
	 *
	 * @param command the command is command to send the request get the data
	 * @param param the param is param to send the request get the data
	 * @return String is data response from the device or None if response fail
	 */
	private String retrieveDataByCommandName(commandNames command, commandNames param, LgControllingCommand lgControllingCommand) {
		try {
			byte[] response = send(LgWebOSUtils.buildSendString((byte) monitorID, LgWebOSConstants.commands.get(command), LgWebOSConstants.commands.get(param)));
			return digestResponse(response, command).toString();
		} catch (Exception ce) {
			failedMonitor.add(lgControllingCommand.getName());
			this.logger.warn(String.format("An error occurred when sending command %s to retrieve data", command.name()), ce);
			return LgWebOSConstants.NA;
		}
	}

	/**
	 * Control power on
	 */
	protected void powerON() {
		try {
			byte[] response = send(
					LgWebOSUtils.buildSendString((byte) monitorID, LgWebOSConstants.commands.get(LgWebOSConstants.commandNames.POWER), LgWebOSConstants.powerStatus.get(LgWebOSConstants.powerStatusNames.ON)));

			digestResponse(response, LgWebOSConstants.commandNames.POWER);
			updateCachedDeviceData(cacheMapOfPriorityInputAndValue, LgWebOSConstants.POWER, LgWebOSConstants.ON);
		} catch (Exception e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("error during power OFF send", e);
			}
			throw new IllegalArgumentException("An error occurred when control power on", e);
		}
	}

	/**
	 * Control power off
	 */
	protected void powerOFF() {
		try {
			byte[] response = send(
					LgWebOSUtils.buildSendString((byte) monitorID, LgWebOSConstants.commands.get(LgWebOSConstants.commandNames.POWER), LgWebOSConstants.powerStatus.get(LgWebOSConstants.powerStatusNames.OFF)));

			digestResponse(response, LgWebOSConstants.commandNames.POWER);
			updateCachedDeviceData(cacheMapOfPriorityInputAndValue, LgWebOSConstants.POWER, LgWebOSConstants.OFF);
		} catch (Exception e) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("error during power ON send", e);
			}
			throw new IllegalArgumentException("An error occurred when control power off", e);
		}
	}

	/**
	 * Update cache device data
	 *
	 * @param cacheMapOfPropertyNameAndValue the cacheMapOfPropertyNameAndValue are map key and value of it
	 * @param property the key is property name
	 * @param value the value is String value
	 */
	private void updateCachedDeviceData(Map<String, String> cacheMapOfPropertyNameAndValue, String property, String value) {
		cacheMapOfPropertyNameAndValue.remove(property);
		cacheMapOfPropertyNameAndValue.put(property, value);
		//Remove the caching lifetime after receiving new data
		localCachingLifeTimeOfMap.remove(property);
	}

	/**
	 * This method is used to digest the response received from the device
	 *
	 * @param response This is the response to be digested
	 * @param expectedResponse This is the expected response type to be compared with received
	 * @return Object This returns the result digested from the response.
	 */
	protected Object digestResponse(byte[] response, commandNames expectedResponse) {
		if (response[0] == LgWebOSConstants.commands.get(expectedResponse)[1]) {

			byte[] responseStatus = Arrays.copyOfRange(response, 5, 7);

			if (Arrays.equals(responseStatus, LgWebOSConstants.replyStatusCodes.get(replyStatusNames.OK))) {

				byte[] reply = Arrays.copyOfRange(response, 7, 9);

				switch (expectedResponse) {
					case NATURAL_MODE:
						String natural = convertByteToValue(reply);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.NATURAL_MODE, natural);
						return natural;
					case TILE_ID:
						String tileID = convertByteToValue(reply);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TILE_MODE_ID, tileID);
						return tileID;
					case TILE_MODE_CONTROL:
						String tileModeControl = convertByteToValue(reply);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TILE_MODE_SETTINGS, tileModeControl);
						return tileModeControl;
					case NATURAL_SIZE:
						int naturalSize = Integer.parseInt(convertByteToValue(Arrays.copyOfRange(response, 9, 11)), 16);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.NATURAL_SIZE, String.valueOf(naturalSize));
						return naturalSize;
					case BACKLIGHT:
						int backlight = Integer.parseInt(convertByteToValue(reply), 16);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.BACKLIGHT, String.valueOf(backlight));
						return backlight;
					case MUTE:
						int mute = Integer.parseInt(convertByteToValue(reply), 16);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.MUTE, String.valueOf(mute));
						return mute;
					case VOLUME:
						int volume = Integer.parseInt(convertByteToValue(reply), 16);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.VOLUME, String.valueOf(volume));
						return volume;
					case FAILOVER_INPUT_LIST:
						int len = response.length;
						reply = Arrays.copyOfRange(response, 7, len - 1);
						convertInputPriorityByValue(convertByteToValue(reply));
						return reply;
					case POWER_MANAGEMENT_MODE:
						reply = Arrays.copyOfRange(response, 9, 11);
						String powerManagement = convertByteToValue(reply);
						powerManagement = EnumTypeHandler.getNameEnumByValue(PowerManagementModeEnum.class, powerManagement);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.POWER_MANAGEMENT_MODE, powerManagement);
						return powerManagement;
					case POWER:
						for (Map.Entry<LgWebOSConstants.powerStatusNames, byte[]> entry : LgWebOSConstants.powerStatus.entrySet()) {
							if (Arrays.equals(reply, entry.getValue())) {
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.POWER, entry.getKey().toString());
								return entry.getKey();
							}
						}
						break;
					case NETWORK_SETTING:
						reply = Arrays.copyOfRange(response, 10, response.length - 1);
						convertNetworkSettingByValue(convertByteToValue(reply));
						return reply;
					case INPUT_SELECT:
					case INPUT:
						for (Map.Entry<LgWebOSConstants.inputNames, byte[]> entry : LgWebOSConstants.inputs.entrySet()) {
							if (Arrays.equals(reply, entry.getValue())) {
								String input = convertByteToValue(entry.getValue());
								String inputValue = EnumTypeHandler.getNameEnumByValue(FailOverInputSourceEnum.class, input);
								if (LgWebOSConstants.NA.equalsIgnoreCase(inputValue)) {
									inputValue = EnumTypeHandler.getNameEnumByValue(InputSourceDropdown.class, input);
								}
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.INPUT_SELECT, inputValue);
								return entry.getKey();
							}
						}
						break;
					case TEMPERATURE:
						int temperature = Integer.parseInt(new String(reply), 16);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TEMPERATURE, String.valueOf(temperature));
						return temperature;
					case FAN_STATUS:
						for (Map.Entry<LgWebOSConstants.fanStatusNames, byte[]> entry : LgWebOSConstants.fanStatusCodes.entrySet()) {
							if (Arrays.equals(reply, entry.getValue())) {
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.FAN, entry.getKey().name());
								return entry.getKey();
							}
						}
						break;
					case SYNC_STATUS:
						reply = Arrays.copyOfRange(response, 7, 11);
						for (Map.Entry<LgWebOSConstants.syncStatusNames, byte[]> entry : LgWebOSConstants.syncStatusCodes.entrySet()) {
							if (Arrays.equals(reply, entry.getValue())) {
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.SIGNAL, entry.getKey().toString());
								return entry.getKey();
							}
						}
						break;
					case SERIAL_NUMBER:
						byte[] data = Arrays.copyOfRange(response, 7, 19);
						String serialNumber = convertByteToValue(data);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.SERIAL_NUMBER, serialNumber);
						return serialNumber;
					case FAILOVER:
						String failOver = convertByteToValue(reply);
						for (FailOverEnum name : FailOverEnum.values()) {
							if (name.getValue().equals(failOver)) {
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.FAILOVER_MODE, name.getName());
								return name.getName();
							}
						}
						break;
					case SOFTWARE_VERSION:
						data = Arrays.copyOfRange(response, 7, 13);
						String softwareVersion = convertByteToValue(data);
						//Custom software with format xx.xx.xx
						StringBuilder stringBuilder = new StringBuilder();
						for (int i = 0; i < softwareVersion.length(); i = i + 2) {
							stringBuilder.append(softwareVersion, i, i + 2);
							if (i != softwareVersion.length() - 2) {
								stringBuilder.append(LgWebOSConstants.DOT);
							}
						}
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.SOFTWARE_VERSION, stringBuilder.toString());
						return stringBuilder.toString();
					case DISPLAY_STAND_BY_MODE:
						String pdm = convertByteToValue(reply);
						for (PowerManagement name : PowerManagement.values()) {
							if (name.getValue().equals(pdm)) {
								if (!localCacheMapOfPropertyNameAndValue.isEmpty()) {
									localCacheMapOfPropertyNameAndValue.remove(LgWebOSConstants.DISPLAY_STAND_BY_MODE);
								}
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.DISPLAY_STAND_BY_MODE, name.getName());
								if (PowerManagement.OFF.getName().equals(name.getName())) {
									return name.getName();
								}
								return LgWebOSConstants.ON;
							}
						}
						break;
					case DATE:
						data = Arrays.copyOfRange(response, 7, 13);
						String date = convertDateFormatByValue(data, false);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.DATE, date);
						return date;
					case TIME:
						data = Arrays.copyOfRange(response, 7, 13);
						String time = convertDateFormatByValue(data, true);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TIME, time);
						return time;
					case TILE_MODE_SETTINGS:
						byte[] typeModeStatus = Arrays.copyOfRange(response, 7, 9);
						byte[] typeModeColumn = Arrays.copyOfRange(response, 9, 11);
						byte[] typeModeRow = Arrays.copyOfRange(response, 11, 13);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TILE_MODE_COLUMN, String.valueOf(Integer.parseInt(convertByteToValue(typeModeColumn), 16)));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TILE_MODE_ROW, String.valueOf(Integer.parseInt(convertByteToValue(typeModeRow), 16)));
						String tileMode = convertByteToValue(typeModeStatus);
						for (TileMode name : TileMode.values()) {
							if (name.isStatus() && name.getValue().equals(tileMode)) {
								updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TILE_MODE, name.getName());
								return name.getName();
							}
						}
						break;
					case ASPECT_RATIO:
						String aspectRatio = EnumTypeHandler.getNameEnumByValue(AspectRatio.class, convertByteToValue(reply));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.ASPECT_RATIO, aspectRatio);
						return aspectRatio;
					case BRIGHTNESS_CONTROL:
						String brightness = EnumTypeHandler.getNameEnumByValue(BrightnessSize.class, convertByteToValue(reply));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.BRIGHTNESS_CONTROL, brightness);
						return brightness;
					case PICTURE_MODE:
						String pictureMode = EnumTypeHandler.getNameEnumByValue(PictureMode.class, convertByteToValue(reply));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.PICTURE_MODE, pictureMode);
						return pictureMode;
					case BRIGHTNESS:
						reply = Arrays.copyOfRange(response, 7, 9);
						String brightnessMode = String.valueOf(Integer.parseInt(convertByteToValue(reply), 16));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.BRIGHTNESS, brightnessMode);
						return brightnessMode;
					case CONTRAST:
						reply = Arrays.copyOfRange(response, 7, 9);
						String sharpness = String.valueOf(Integer.parseInt(convertByteToValue(reply), 16));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.CONTRAST, sharpness);
						return sharpness;
					case SHARPNESS:
						reply = Arrays.copyOfRange(response, 7, 9);
						String sharpnessValue = String.valueOf(Integer.parseInt(convertByteToValue(reply), 16));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.SHARPNESS, sharpnessValue);
						return sharpnessValue;
					case SCREEN_COLOR:
						reply = Arrays.copyOfRange(response, 7, 9);
						String tint = String.valueOf(Integer.parseInt(convertByteToValue(reply), 16));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.SCREEN_COLOR, tint);
						return tint;
					case TINT:
						reply = Arrays.copyOfRange(response, 7, 9);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.TINT, convertByteToValue(reply));
						return reply;
					case COLOR_TEMPERATURE:
						reply = Arrays.copyOfRange(response, 7, 9);
						String colorTemperature = String.valueOf(Integer.parseInt(convertByteToValue(reply), 16));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.COLOR_TEMPERATURE, colorTemperature);
						return colorTemperature;
					case BALANCE:
						reply = Arrays.copyOfRange(response, 7, 9);
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.BALANCE, convertByteToValue(reply));
						return reply;
					case SOUND_MODE:
						String soundModeValue = EnumTypeHandler.getNameEnumByValue(SoundMode.class, convertByteToValue(reply));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.SOUND_MODE, soundModeValue);
						return soundModeValue;
					case NO_SIGNAL_POWER_OFF:
						String noSignal = String.valueOf(Integer.parseInt(convertByteToValue(reply)));
						String noSignalValue = LgWebOSConstants.ON;
						if (String.valueOf(LgWebOSConstants.ZERO).equals(noSignal)) {
							noSignalValue = LgWebOSConstants.OFF;
						}
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.NO_SIGNAL_POWER_OFF, noSignalValue);
						return noSignalValue;
					case NO_IR_POWER_OFF:
						String noIRPower = String.valueOf(Integer.parseInt(convertByteToValue(reply)));
						String noIRPowerValue = LgWebOSConstants.ON;
						if (String.valueOf(LgWebOSConstants.ZERO).equals(noIRPower)) {
							noIRPowerValue = LgWebOSConstants.OFF;
						}
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.NO_IR_POWER_OFF, noIRPowerValue);
						return noIRPowerValue;
					case LANGUAGE:
						String languageValue = EnumTypeHandler.getNameEnumByValue(Language.class, convertByteToValue(reply));
						if (!LgWebOSConstants.NA.equals(languageValue)) {
							updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.LANGUAGE, languageValue);
						}
						return languageValue;
					case POWER_ON_STATUS:
						String powerOnStatus = EnumTypeHandler.getNameEnumByValue(PowerStatus.class, convertByteToValue(reply));
						updateCachedDeviceData(localCacheMapOfPropertyNameAndValue, LgWebOSConstants.POWER_ON_STATUS, powerOnStatus);
						return powerOnStatus;
					case REBOOT:
						reply = Arrays.copyOfRange(response, 7, 9);
						String rebootValue = convertByteToValue(reply);
						if (!LgWebOSConstants.REBOOT_VALUE.equals(rebootValue)) {
							throw new ResourceNotReachableException("NG reply");
						}
						return rebootValue;
					default:
						logger.debug("this command name is not supported" + expectedResponse);
				}
			} else if (Arrays.equals(responseStatus, LgWebOSConstants.replyStatusCodes.get(replyStatusNames.NG))) {
				switch (expectedResponse) {
					case FAN_STATUS: {
						return LgWebOSConstants.fanStatusNames.NOT_SUPPORTED;
					}
					default: {
						if (this.logger.isErrorEnabled()) {
							this.logger.error("error: NG reply: " + this.host + " port: " + this.getPort());
						}
						throw new ResourceNotReachableException("NG reply");
					}
				}
			}
		} else {
			if (this.logger.isErrorEnabled()) {
				this.logger.error("error: Unexpected reply: " + this.host + " port: " + this.getPort());
			}
			throw new RuntimeException("Error Unexpected reply");
		}

		return LgWebOSConstants.NA;
	}

	/**
	 * Convert input priority by value
	 *
	 * @param inputPriority the inputPriority is String value
	 */
	private void convertInputPriorityByValue(String inputPriority) {
		int index = 1;
		cacheMapOfPriorityInputAndValue = new HashMap<>();
		for (int i = 0; i < inputPriority.length(); i = i + 2) {
			String value = inputPriority.substring(i, i + 2);
			cacheMapOfPriorityInputAndValue.put(LgWebOSConstants.PRIORITY + index, EnumTypeHandler.getNameEnumByValue(FailOverInputSourceEnum.class, value));
			index++;
		}
	}

	/**
	 * Convert network setting by value
	 *
	 * @param networkResponse the networkResponse is String value
	 */
	private void convertNetworkSettingByValue(String networkResponse) {
		String[] networkArray = networkResponse.split(LgWebOSConstants.SPACE);
		StringBuilder stringBuilder = new StringBuilder();
		// value of network settings will be 172000001001 255255255000 172000001001 172000000003
		try {
			convertNetworkSettingToValue(stringBuilder, networkArray[networkArray.length - 4]);
			localCacheMapOfPropertyNameAndValue.put(LgWebOSConstants.IP_ADDRESS, stringBuilder.toString());

			stringBuilder = new StringBuilder();
			convertNetworkSettingToValue(stringBuilder, networkArray[networkArray.length - 3]);
			localCacheMapOfPropertyNameAndValue.put(LgWebOSConstants.SUBNET_MASK, stringBuilder.toString());

			stringBuilder = new StringBuilder();
			convertNetworkSettingToValue(stringBuilder, networkArray[networkArray.length - 2]);
			localCacheMapOfPropertyNameAndValue.put(LgWebOSConstants.GATEWAY, stringBuilder.toString());

			stringBuilder = new StringBuilder();
			convertNetworkSettingToValue(stringBuilder, networkArray[networkArray.length - 1]);
			localCacheMapOfPropertyNameAndValue.put(LgWebOSConstants.DNS_SERVER, stringBuilder.toString());
		} catch (Exception e) {
			localCacheMapOfPropertyNameAndValue.put(LgWebOSConstants.IP_ADDRESS, LgWebOSConstants.NA);
			localCacheMapOfPropertyNameAndValue.put(LgWebOSConstants.SUBNET_MASK, LgWebOSConstants.NA);
			localCacheMapOfPropertyNameAndValue.put(LgWebOSConstants.GATEWAY, LgWebOSConstants.NA);
			localCacheMapOfPropertyNameAndValue.put(LgWebOSConstants.DNS_SERVER, LgWebOSConstants.NA);
		}
	}

	/**
	 * Convert data of network settings by value
	 *
	 * @param propertyName the propertyName is name of property
	 * @param networkValue the networkValue is value as String
	 */
	private void convertNetworkSettingToValue(StringBuilder propertyName, String networkValue) {
		//The network example value would be 192168000001, we will convert it to 192.168.0.1
		for (int i = 0; i < networkValue.length(); i = i + 3) {
			String value = networkValue.substring(i, i + 3);
			propertyName.append(Integer.parseInt(value));
			if (i != networkValue.length() - 3) {
				propertyName.append(LgWebOSConstants.DOT);
			}
		}
	}

	/**
	 * Convert byte to value
	 *
	 * @param bytes is data represented as bytes
	 * @return String is data after converting byte to String
	 */
	private String convertByteToValue(byte[] bytes) {
		StringBuilder stringBuilder = new StringBuilder();
		for (byte byteValue : bytes) {
			stringBuilder.append((char) (byteValue));
		}
		return stringBuilder.toString();
	}

	/**
	 * Convert value to format month/day/year
	 *
	 * @param data the data is data of the response
	 * @param isTimeFormat the isTimeFormat is boolean value
	 * @return String is format of date
	 */
	private String convertDateFormatByValue(byte[] data, boolean isTimeFormat) {
		StringBuilder stringBuilder = new StringBuilder();
		StringBuilder dateValue = new StringBuilder();
		String year = LgWebOSConstants.EMPTY_STRING;
		for (byte byteValue : data) {
			stringBuilder.append((char) (byteValue));
		}

		//The value example 173B00 with 17 is hours, 3B is minutes, and 00 is seconds
		//convert Hex to decimal data to 173B00 to 23:59:00
		String defaultTime = LgWebOSConstants.AM;
		if (isTimeFormat) {
			for (int i = 0; i < stringBuilder.length() - 3; i = i + 2) {
				int hexValue = Integer.parseInt(stringBuilder.substring(i, i + 2), 16);
				if (i == 0) {
					if (hexValue == 0) {
						defaultTime = LgWebOSConstants.PM;
						hexValue = 12;
					} else if (hexValue > 12) {
						defaultTime = LgWebOSConstants.PM;
						hexValue = hexValue - 12;
					}
					dateValue.append(hexValue);
				} else {
					if (hexValue < 10) {
						dateValue.append(LgWebOSConstants.COLON + LgWebOSConstants.ZERO + hexValue);
					} else {
						dateValue.append(LgWebOSConstants.COLON + hexValue);
					}
					dateValue.append(LgWebOSConstants.SPACE + defaultTime);
				}
			}
			return dateValue.toString();
		}
		//The value example 0c011F with 0c is year, 01 is month, and 1F is day
		//convert Hex to decimal data to 0c011f to 1/31/2022
		//the year format = 2010 + 0c in(0c111F)
		for (int i = 0; i < stringBuilder.length() - 1; i = i + 2) {
			int hexValue = Integer.parseInt(stringBuilder.substring(i, i + 2), 16);
			if (i == 0) {
				year = String.valueOf(2010 + hexValue);
			} else {
				dateValue.append(hexValue + "/");
			}
		}
		return dateValue.append(year).toString();
	}

	/**
	 * Add switch is control property for metric
	 *
	 * @param stats list statistic
	 * @param name String name of metric
	 * @return AdvancedControllableProperty switch instance
	 */
	private AdvancedControllableProperty controlSwitch(Map<String, String> stats, String name, String value, String labelOff, String labelOn) {
		if (StringUtils.isNullOrEmpty(value) || LgWebOSConstants.NA.equals(value)) {
			return null;
		}
		stats.put(name, value);
		return createSwitch(name, Integer.parseInt(value), labelOff, labelOn);
	}

	/**
	 * Create switch is control property for metric
	 *
	 * @param name the name of property
	 * @param status initial status (0|1)
	 * @return AdvancedControllableProperty switch instance
	 */
	private AdvancedControllableProperty createSwitch(String name, int status, String labelOff, String labelOn) {
		AdvancedControllableProperty.Switch toggle = new AdvancedControllableProperty.Switch();
		toggle.setLabelOff(labelOff);
		toggle.setLabelOn(labelOn);

		AdvancedControllableProperty advancedControllableProperty = new AdvancedControllableProperty();
		advancedControllableProperty.setName(name);
		advancedControllableProperty.setValue(status);
		advancedControllableProperty.setType(toggle);
		advancedControllableProperty.setTimestamp(new Date());
		return advancedControllableProperty;
	}

	/**
	 * Create control slider is control property for the metric
	 *
	 * @param name the name of the metric
	 * @param value the value of the metric
	 * @param rangeStart is the starting number of the range
	 * @param rangeEnd is the end number of the range
	 * @return AdvancedControllableProperty slider instance
	 */
	private AdvancedControllableProperty createSlider(String name, Float value, String rangeStart, String rangeEnd) {
		AdvancedControllableProperty.Slider slider = new AdvancedControllableProperty.Slider();
		slider.setLabelEnd(String.valueOf(rangeEnd));
		slider.setLabelStart(String.valueOf(rangeStart));
		slider.setRangeEnd(Float.valueOf(rangeEnd));
		slider.setRangeStart(Float.valueOf(rangeStart));

		return new AdvancedControllableProperty(name, new Date(), slider, value);
	}

	/**
	 * Create control slider is control property for the metric
	 *
	 * @param name name of the slider
	 * @param stats list of statistics
	 * @param rangeStart is the starting number of the range
	 * @param rangeEnd is the end number of the range
	 * @return AdvancedControllableProperty slider instance if add slider success else will is null
	 */
	private AdvancedControllableProperty createControlSlider(String name, String value, Map<String, String> stats, String rangeStart, String rangeEnd) {
		if (StringUtils.isNullOrEmpty(value) || LgWebOSConstants.NA.equals(value)) {
			return null;
		}
		stats.put(name, value);
		return createSlider(name, Float.valueOf(value), rangeStart, rangeEnd);
	}

	/**
	 * Add dropdown is control property for metric
	 *
	 * @param stats list statistic
	 * @param options list select
	 * @param name String name of metric
	 * @return AdvancedControllableProperty dropdown instance if add dropdown success else will is null
	 */
	private AdvancedControllableProperty controlDropdown(Map<String, String> stats, String[] options, String name, String value) {
		if (StringUtils.isNullOrEmpty(value) || LgWebOSConstants.NA.equals(value)) {
			return null;
		}
		stats.put(name, value);
		return createDropdown(name, options, value);
	}

	/***
	 * Create dropdown advanced controllable property
	 *
	 * @param name the name of the control
	 * @param initialValue initial value of the control
	 * @return AdvancedControllableProperty dropdown instance
	 */
	private AdvancedControllableProperty createDropdown(String name, String[] values, String initialValue) {
		AdvancedControllableProperty.DropDown dropDown = new AdvancedControllableProperty.DropDown();
		dropDown.setOptions(values);
		dropDown.setLabels(values);

		return new AdvancedControllableProperty(name, new Date(), dropDown, initialValue);
	}

	/**
	 * Create a button.
	 *
	 * @param name name of the button
	 * @param label label of the button
	 * @param labelPressed label of the button after pressing it
	 * @param gracePeriod grace period of button
	 * @return This returns the instance of {@link AdvancedControllableProperty} type Button.
	 */
	private AdvancedControllableProperty createButton(String name, String label, String labelPressed, long gracePeriod) {
		AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
		button.setLabel(label);
		button.setLabelPressed(labelPressed);
		button.setGracePeriod(gracePeriod);
		return new AdvancedControllableProperty(name, new Date(), button, LgWebOSConstants.EMPTY_STRING);
	}

	/**
	 * This method is used to validate input config management from user
	 */
	private void convertConfigManagement() {
		isConfigManagement = StringUtils.isNotNullOrEmpty(this.configManagement) && this.configManagement.equalsIgnoreCase(LgWebOSConstants.IS_VALID_CONFIG_MANAGEMENT);
	}

	/**
	 * This method is used to convert or validate the user input
	 */
	private void convertCacheLifetime() {
		try {
			currentCachingLifetime = Integer.parseInt(this.cachingLifetime);
			if (currentCachingLifetime <= LgWebOSConstants.ZERO) {
				currentCachingLifetime = LgWebOSConstants.DEFAULT_CACHING_LIFETIME;
			}
		} catch (Exception e) {
			currentCachingLifetime = LgWebOSConstants.DEFAULT_CACHING_LIFETIME;
		}
	}

	/**
	 * This method is used to validate input delay time from user
	 */
	private void convertDelayTime() {
		try {
			commandsCoolDownDelay = Integer.parseInt(this.coolDownDelay);
			if (LgWebOSConstants.MIN_DELAY_TIME >= commandsCoolDownDelay) {
				commandsCoolDownDelay = LgWebOSConstants.MIN_DELAY_TIME;
			}
			if (LgWebOSConstants.MAX_DELAY_TIME <= commandsCoolDownDelay) {
				commandsCoolDownDelay = LgWebOSConstants.MAX_DELAY_TIME;
			}
		} catch (Exception e) {
			commandsCoolDownDelay = LgWebOSConstants.DEFAULT_DELAY_TIME;
		}
	}

	/**
	 * This method is used to validate input config timeout from user
	 */
	private void convertConfigTimeout() {
		int configTimeout;
		try {
			configTimeout = Integer.parseInt(this.configTimeout);
			if (LgWebOSConstants.DEFAULT_CONFIG_TIMEOUT >= configTimeout) {
				configTimeout = LgWebOSConstants.DEFAULT_CONFIG_TIMEOUT;
			}
			if (LgWebOSConstants.MAX_CONFIG_TIMEOUT <= configTimeout) {
				configTimeout = LgWebOSConstants.MAX_CONFIG_TIMEOUT;
			}
		} catch (Exception e) {
			configTimeout = LgWebOSConstants.DEFAULT_CONFIG_TIMEOUT;
		}
		defaultConfigTimeout = configTimeout / 100;
	}

	/**
	 * This method is used to validate input config timeout from user
	 */
	private void convertPollingInterval() {
		int pollingIntervalValue;
		try {
			pollingIntervalValue = Integer.parseInt(this.pollingInterval);
			if (pollingIntervalValue < LgWebOSConstants.DEFAULT_POLLING_INTERVAL) {
				pollingIntervalValue = LgWebOSConstants.DEFAULT_POLLING_INTERVAL;
			}
		} catch (Exception e) {
			pollingIntervalValue = LgWebOSConstants.DEFAULT_POLLING_INTERVAL;
		}
		pollingIntervalInIntValue = pollingIntervalValue;
	}

	/**
	 * Add or Modify statistic properties if values are not N/A
	 *
	 * @param statistics monitoring properties
	 * @param propertyName name of property
	 * @param propertyValue value of property
	 */
	private void addOrUpdateStatisticProperties(Map<String, String> statistics, String propertyName, String propertyValue) {
		if (!propertyValue.equals(LgWebOSConstants.NA)) {
			statistics.put(propertyName, propertyValue);
		}
	}
}