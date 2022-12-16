package com.zx.sms.connect.manager;

import com.zx.sms.config.PropertiesUtils;

public final class TestConstants {
	public static final Integer Count = Integer.parseInt(PropertiesUtils.getproperties("TestConstants.Count", "100000"));
}
