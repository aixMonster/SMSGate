package com.zx.sms.codec.cmpp.wap;

import com.zx.sms.LongSMSMessage;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.handler.api.AbstractBusinessHandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class LongMessageMarkerReadHandler extends AbstractBusinessHandler {
	private EndpointEntity entity;

	public LongMessageMarkerReadHandler(EndpointEntity entity) {
		this.entity = entity;
	}

	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof LongSMSMessage && ((LongSMSMessage) msg).needHandleLongMessage()) {
			setUniqueLongMsgId((LongSMSMessage)msg,ctx,true);
		}

		ctx.fireChannelRead(msg);
	}

	//长短信类型生成唯一ID
	private void setUniqueLongMsgId( LongSMSMessage lmsg,ChannelHandlerContext ctx,boolean read) {
		lmsg.setUniqueLongMsgId(new UniqueLongMsgId(entity,ctx.channel(),lmsg,read));
	}


	@Override
	public String name() {
		return "_LongMessageMarkerReadHandler";
	}

}