package com.zx.sms.handler.smgp;

import org.marre.sms.SmsMessage;

import com.zx.sms.codec.cmpp.wap.AbstractLongMessageHandler;
import com.zx.sms.codec.smgp.msg.SMGPDeliverMessage;
import com.zx.sms.connect.manager.EndpointEntity;

import io.netty.channel.ChannelHandler.Sharable;
public class SMGPDeliverLongMessageHandler extends AbstractLongMessageHandler<SMGPDeliverMessage> {

	public SMGPDeliverLongMessageHandler(EndpointEntity entity) {
		super(entity);
	}

	@Override
	protected void resetMessageContent(SMGPDeliverMessage t, SmsMessage content) {
		t.setMsgContent(content);
	}

}
