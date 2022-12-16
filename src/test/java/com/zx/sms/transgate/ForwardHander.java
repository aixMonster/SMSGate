package com.zx.sms.transgate;

import org.marre.sms.SmsDcs;
import org.marre.sms.SmsMessage;
import org.marre.sms.SmsTextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.connect.manager.EndpointConnector;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.handler.api.AbstractBusinessHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

//实现请求转发，状态转发
public class ForwardHander extends AbstractBusinessHandler {
	private static final Logger logger = LoggerFactory.getLogger(ForwardHander.class);
	private String forwardEid;
	ForwardHander(String forwardEid){
		this.forwardEid = forwardEid;
	}
	
    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
       	
    	if (msg instanceof CmppDeliverRequestMessage) {
    		//作为客户端收report消息
    		//收到上行不处理
			
    	}else if (msg instanceof CmppSubmitRequestMessage) {
    		//作为服务端收submit消息
    		//短信合并完成了
    		CmppSubmitRequestMessage submit = (CmppSubmitRequestMessage)msg;
    		
//    		//重新设置最大拆分长度
//    		SmsTextMessage sms = (SmsTextMessage)submit.getSmsMessage();
//    		SmsDcs mydcs = new SmsDcs(sms.getDcs().getValue()) {
//    			public int getMaxMsglength() {
//    				return 16;
//    			}
//    		};
//    		submit.setMsg(new SmsTextMessage(sms.getText(),mydcs));
    		//转发给上游服务
    		EndpointConnector conn = EndpointManager.INS.getEndpointConnector(forwardEid);
    		Channel ch = conn.fetch(); //获取连接，保证必写成功
    		ch.writeAndFlush(submit);
			
    	}else if(msg instanceof CmppSubmitResponseMessage) {
    		//作为客户端收response 消息
    	
    	}
    	
    	ctx.fireChannelRead(msg);
    }

	@Override
	public String name() {
		return "ForwardHander";
	}

}
