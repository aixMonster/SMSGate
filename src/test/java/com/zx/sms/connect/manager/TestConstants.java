package com.zx.sms.connect.manager;

import com.zx.sms.config.PropertiesUtils;

public final class TestConstants {
	public static final Integer Count = Integer.parseInt(PropertiesUtils.getproperties("TestConstants.Count", "100000"));
	public static final Boolean isReSendFailMsg = Boolean.valueOf(System.getProperty("isReSendFailMsg", "false"));
}
