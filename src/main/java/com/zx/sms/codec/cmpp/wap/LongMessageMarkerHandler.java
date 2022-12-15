package com.zx.sms.codec.cmpp.wap;

import com.zx.sms.LongSMSMessage;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.handler.api.AbstractBusinessHandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class LongMessageMarkerHandler extends AbstractBusinessHandler {
	private EndpointEntity entity;

	public LongMessageMarkerHandler(EndpointEntity entity) {
		this.entity = entity;
	}

	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof LongSMSMessage && ((LongSMSMessage) msg).needHandleLongMessage()) {
			setUniqueLongMsgId((LongSMSMessage)msg,ctx);
		}

		ctx.fireChannelRead(msg);
	}

	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof LongSMSMessage && ((LongSMSMessage) msg).needHandleLongMessage()) {
			setUniqueLongMsgId((LongSMSMessage)msg,ctx);
		}
		ctx.write(msg, promise);
	}
	
	//长短信类型生成唯一ID
	private void setUniqueLongMsgId( LongSMSMessage lmsg,ChannelHandlerContext ctx) {
		lmsg.setUniqueLongMsgId(new UniqueLongMsgId(entity,ctx.channel(),lmsg));
	}


	@Override
	public String name() {
		return "_LongMessageMarkerHandler";
	}

}