package com.zx.sms.codec.cmpp.wap;

import com.zx.sms.LongSMSMessage;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.handler.api.AbstractBusinessHandler;

import io.netty.channel.ChannelHandlerContext;

public class LongMessageMarkerHandler extends AbstractBusinessHandler {
	private EndpointEntity entity;

	public LongMessageMarkerHandler(EndpointEntity entity) {
		this.entity = entity;
	}

	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof LongSMSMessage && ((LongSMSMessage) msg).needHandleLongMessage()) {
			LongSMSMessage lmsg = (LongSMSMessage) msg;
			String srcIdAndDestId = lmsg.getSrcIdAndDestId();
			String channelId = (entity!=null && !entity.isRecvLongMsgOnMultiLink())? ctx.channel().id().asShortText(): "C";
			
			StringBuilder sb = entity == null ? new StringBuilder(channelId+"."+ srcIdAndDestId)  : new StringBuilder(entity.getId()).append(".").append(channelId).append(".").append(srcIdAndDestId);
			String key =  LongMessageFrameHolder.INS.parseFrameKey(sb.toString(), lmsg);
			lmsg.setUniqueLongMsgId(new UniqueLongMsgId(key));
		}

		ctx.fireChannelRead(msg);
	}

	@Override
	public String name() {
		return "_LongMessageMarkerHandler";
	}

}
