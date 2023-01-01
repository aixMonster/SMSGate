package com.zx.sms.handler.smpp;

import com.chinamobile.cmos.sms.SmsMessage;
import com.zx.sms.LongSMSMessage;
import com.zx.sms.codec.cmpp.wap.AbstractLongMessageHandler;
import com.zx.sms.codec.cmpp.wap.LongMessageFrame;
import com.zx.sms.codec.smpp.msg.BaseSm;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.connect.manager.smpp.SMPPEndpointEntity;

public class SMPPLongMessageHandler extends AbstractLongMessageHandler<BaseSm> {
	
	
	public SMPPLongMessageHandler(EndpointEntity entity) {
		super(entity);
	}

	@Override
	protected void resetMessageContent(BaseSm t, SmsMessage content) {
		t.setSmsMsg(content);
	}
	
}