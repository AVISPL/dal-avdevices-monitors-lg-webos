# LG WebOS TV Integration - Capabilities & Configuration
This document covers LG WebOS TV Adapter Capabilities and Configuration.

Symphony integrates with LG WebOS TVs to provide comprehensive monitoring and control of LG display devices.
Main features are: real-time device health monitoring, display and input management, power management, sound configuration, failover control, and tile mode settings.

## Main use cases for LG WebOS TV
- **Monitor** device status, network information, input signal, temperature, and display standby mode
- **Control** display settings - brightness, backlight, contrast, picture mode, color temperature, and more
- **Manage** power management behavior, failover configuration, and input priority
- **Configure** sound settings, tile mode layout, and device-level display parameters

## Prerequisites for setting up connection to LG WebOS TV adapter
The LG WebOS TV adapter communicates directly with the device over TELNET on port 9761. No OAuth or external API registration is required.

Supported model series: UH5J, UH7J, UL3J, EW5, XE4F 
Supported LG WebOS versions: 4.0 - 6.0

Ensure the LG device is reachable on the network from the Symphony Cloud Connector, and that TELNET access on port 9761 is not blocked by firewalls or proxies.

**Recommended pre-configuration on the device:**
- Set PowerManagementMode to "Network Ready" so the device responds to Symphony commands (including ping) even when powered off
- For Play Via URL feature: enable the URL Loader in the device's webUI under Control Manager > Device Control > URL Setting

## LG WebOS TV Device Connection Setup and Provisioning

Note: The connection configuration below describe a successfull LG WebOS integration setup, these should not be confused with the adapter configuration properties. They are not to be infered as troubleshooting checks and should not be used when diagnosing specific errors unless a troubleshooting entry (provided in the Troubleshooting section) explicitly references them.

Once the device is network-accessible, use the following settings to configure it in Symphony:

| Field | Description |
|---|---|
| Device Type | AV Device |
| Category | Monitors |
| Manufacturer | LG |
| Model | Exact device model (e.g. 49UH5EB) |
| Monitoring Service | Advanced Monitoring |
| Monitoring Source | Direct |
| Management IP | The IP address of the device |
| Protocol | TELNET |
| Username | - (not required) |
| Password | - (not required) |
| Port Number | 9761 |

When the device is configured, saved, and set active, Symphony will begin communicating with the LG device to retrieve status and property data.

Adapter behavior and performance can be tuned via the following Adapter configuration Properties:

| Property | Description |
| --- | --- |
| configManagement | Whether user control of controllable properties is allowed. |
| pollingInterval | How often (in minutes) device information is refreshed in Symphony. Increasing this value reduces request frequency. Default: 2 (minimum value). |
| cachingLifetime | Number of polling intervals to retain cached values. Default: 5 minutes. Symphony displays 'N/A' when the cache expires. |
| coolDownDelay | Delay, in milliseconds, between consecutive device requests to limit the request rate. Range: 100-500 ms; default: 400. |
| configTimeout | Timeout, in milliseconds, for a request to complete. If exceeded, the connection is terminated and re-established for the next request. Range: 2000-3000 ms; default: 2000. |
| historicalProperties | CSV list of properties to be monitored continuously. |

Note: Invalid values for numeric properties will cause the adapter to automatically revert to the default value.

For detailed information on aggregator and its configuration, please refer to our knowledgebase -> https://symphony.knowledgeowl.com/help/lg-webos-tv-technical-breakdown

## Available Monitored Data for LG WebOS TV adapter
The LG WebOS TV adapter exposes device properties across the following groups:

**Monitoring-only properties** (read-only status information):
- DateTime, DisplayStandbyMode, DNSServer, Fan, Gateway, InputSelect, InputSignal, IPAddress, SerialNumber, SoftwareVersion, SubnetMask, Temperature(C), TileMode

**Controllable properties** are organized into groups on the Extended Properties tab:

| Controllable Property Groups | Description |
|---|---|
| Device Dashboard | Language, Power, Reboot |
| Display | AspectRatio, Backlight, Brightness, BrightnessControl, ColorTemperature, Contrast, PictureMode, ScreenColor, Sharpness, Tint |
| Fail Over | FailOverMode, InputPriority, PriorityInput, PriorityDown, PriorityUp |
| Input | InputSelect |
| Power Management | DisplayStandbyMode, No IR Power Off, No Signal Power Off, PowerManagementMode, PowerOnStatus |
| Sound Mode | Balance, Mute, SoundMode, Volume |
| Tile Mode Settings | TileMode, NaturalMode, NaturalSize, TileID, Column, Row |

**Notes:**
- When FailOverMode is On, the InputPriority property appears. When InputPriority is set to Manual, additional properties are shown: Priority1-3, PriorityDown, PriorityInput, PriorityUp.
- When TileMode is On, TileID and NaturalMode properties appear. When NaturalMode is also On, NaturalSize is additionally shown.

## Troubleshooting checks for LG WebOS TV

**Troubleshooting guidance**
- If an error occurs, focus only on troubleshooting steps that are provided in the section below.
- Do not include prerequisite/setup information.
- Do not add unrelated configuration details from other sections.
- If the document does not provide a direct error troubleshooting step, state that the document does not contain enough guidance for that specific issue.

**Login Error**
- Verify the LG device IP address is correct and reachable
- Ensure port 9761 (TELNET) is open and not blocked by network policies or proxies

**API Error**
- Check the API error description.
- Ensure the device is online and responding to TELNET connections.

**Link Error / Ping Timeout**
- Verify the LG device IP address is reachable from the Symphony Cloud Connector
- Ensure port 9761 (TELNET) is open and not blocked by network policies or proxies
- Try switching between ICMP/TCP ping modes in the Symphony device configuration

**Device Not Responding to Commands**
- Confirm PowerManagementMode is set to "Network Ready" on the device
- LG devices running older firmware (e.g. LG WebOS v4.0) refuse parallel connections from multiple clients. If Symphony cannot connect while another client is connected, the property ControlProtocolStatus will appear with value UNAVAILABLE. This resolves automatically in the next monitoring cycle once the competing connection is released.

**Unsupported Control Value**
- For drop-down properties (e.g. PictureMode, SoundMode): if the selected value is unsupported by the model, Symphony displays: "The property name <property-name> is not supported. The current model does not support control with the value <property-value>, and the device has responded with an error."
- For slider/button properties: Symphony displays "Can't control property <property-name>. The device has responded with an error."

Note: Monitoring capabilities may differ when using an unsupported firmware version.

If none of the recommended steps help, please enter an SOS ticket at {https://avi-spl.atlassian.net/servicedesk/customer/portals}

## What AI Assistant can do with LG WebOS TV integration:
- Find LG WebOS TV devices in Symphony (AV Device | Monitors | LG)
- Verify LG WebOS TV adapter configuration and connectivity status

## What AI Assistant cannot do with LG WebOS TV integration:
- Provision the devices
- Modify device-side settings (e.g. enabling URL Loader, setting PowerManagementMode) - these must be configured directly on the LG device via its webUI
