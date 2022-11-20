package com.zx.sms.handler.smgp;

import org.marre.sms.SmsMessage;

import com.zx.sms.codec.cmpp.wap.AbstractLongMessageHandler;
import com.zx.sms.codec.smgp.msg.SMGPSubmitMessage;
import com.zx.sms.connect.manager.EndpointEntity;

import io.netty.channel.ChannelHandler.Sharable;

public class SMGPSubmitLongMessageHandler extends AbstractLongMessageHandler<SMGPSubmitMessage> {

	public SMGPSubmitLongMessageHandler(EndpointEntity entity) {
		super(entity);
	}

	@Override
	protected void resetMessageContent(SMGPSubmitMessage t, SmsMessage content) {
		t.setMsgContent(content);
	}

}
