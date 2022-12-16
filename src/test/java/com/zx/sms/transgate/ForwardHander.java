package com.zx.sms.transgate;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.connect.manager.EndpointConnector;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.connect.manager.EventLoopGroupFactory;
import com.zx.sms.connect.manager.ExitUnlimitCirclePolicy;
import com.zx.sms.handler.api.AbstractBusinessHandler;
import com.zx.sms.session.cmpp.SessionState;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

//实现请求转发，状态转发
public class ForwardHander extends AbstractBusinessHandler {
	private static final Logger logger = LoggerFactory.getLogger(ForwardHander.class);
	private String forwardEid;
	ForwardHander(String forwardEid){
		this.forwardEid = forwardEid;
	}
	
	private int rate = 2;

	private AtomicLong cnt = new AtomicLong();
	private long lastNum = 0;
	private volatile static boolean inited = false;
	
	private static final Object lock = new Object();
	
	public synchronized void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {
		synchronized (lock) {
			if (evt == SessionState.Connect && !inited) {
				EventLoopGroupFactory.INS.submitUnlimitCircleTask(new Callable<Boolean>() {

					@Override
					public Boolean call() throws Exception {
							long nowcnt = cnt.get();
							EndpointConnector conn = getEndpointEntity().getSingletonConnector();
							
							logger.info("{} channels : {},Totle Receive Msg Num:{},   speed : {}/s",getEndpointEntity().getId(),
									conn == null ? 0 : conn.getConnectionNum(), nowcnt, (nowcnt - lastNum) / rate);
							lastNum = nowcnt;
							return true;
					}
				}, new ExitUnlimitCirclePolicy() {
					@Override
					public boolean notOver(Future future) {
						inited = getEndpointEntity().getSingletonConnector().getConnectionNum()>0;
						return inited;
					}
				}, rate * 1000);
				inited = true;
			}
		}
		ctx.fireUserEventTriggered(evt);
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
    		ChannelFuture future = ch.writeAndFlush(submit);
    		future.addListener(new GenericFutureListener() {
				@Override
				public void operationComplete(Future future) throws Exception {
						cnt.incrementAndGet();
				}
			});
			
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
