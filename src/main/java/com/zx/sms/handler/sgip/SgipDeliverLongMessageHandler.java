package com.zx.sms.handler.sgip;

import org.marre.sms.SmsMessage;

import com.zx.sms.codec.cmpp.wap.AbstractLongMessageHandler;
import com.zx.sms.codec.sgip12.msg.SgipDeliverRequestMessage;
import com.zx.sms.connect.manager.EndpointEntity;

import io.netty.channel.ChannelHandler.Sharable;
public class SgipDeliverLongMessageHandler extends AbstractLongMessageHandler<SgipDeliverRequestMessage> {
	public SgipDeliverLongMessageHandler(EndpointEntity entity) {
		super(entity);
	}

	@Override
	protected void resetMessageContent(SgipDeliverRequestMessage t, SmsMessage content) {
		t.setMsgContent(content);
	}

}
