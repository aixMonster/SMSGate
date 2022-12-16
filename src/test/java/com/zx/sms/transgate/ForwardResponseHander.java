package com.zx.sms.transgate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppDeliverResponseMessage;
import com.zx.sms.codec.cmpp.msg.CmppReportRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.codec.cmpp.wap.UniqueLongMsgId;
import com.zx.sms.common.util.DefaultSequenceNumberUtil;
import com.zx.sms.common.util.MsgId;
import com.zx.sms.connect.manager.EndpointConnector;
import com.zx.sms.connect.manager.EndpointManager;
import com.zx.sms.handler.api.AbstractBusinessHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;

@Sharable
public class ForwardResponseHander extends AbstractBusinessHandler{
	private static final Logger logger = LoggerFactory.getLogger(ForwardResponseHander.class);
	
	private Map<String ,ImmutablePair<AtomicInteger,ImmutablePair<UniqueLongMsgId,Map<Integer,MsgId>>>> uidMap;
	private Map<String ,UniqueLongMsgId> msgIdMap;
	
	ForwardResponseHander(Map<String ,ImmutablePair<AtomicInteger,ImmutablePair<UniqueLongMsgId,Map<Integer,MsgId>>>> uidMap,
			Map<String ,UniqueLongMsgId> msgIdMap){
		this.uidMap = uidMap;
		this.msgIdMap = msgIdMap;
		
	}

	//测试用例，不考虑状态报告先与response回来的情况
    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
    	
    	//作为客户端收report消息
    	if (msg instanceof CmppDeliverRequestMessage) {
    		CmppDeliverRequestMessage e = (CmppDeliverRequestMessage) msg;
    		CmppDeliverResponseMessage responseMessage = new CmppDeliverResponseMessage(e.getHeader().getSequenceId());
			responseMessage.setResult(0);
			responseMessage.setMsgId(e.getMsgId());
			ctx.channel().writeAndFlush(responseMessage);
			
			//状态报告要改写msgId
			if(e.isReport()) {
				CmppReportRequestMessage report = e.getReportRequestMessage();
				MsgId reportMsgId = report.getMsgId();
				//根据报告里的id获取uid
				UniqueLongMsgId uid = msgIdMap.remove(reportMsgId.toString());
				if(uid != null) {
					ImmutablePair<AtomicInteger,ImmutablePair<UniqueLongMsgId,Map<Integer,MsgId>>> t = uidMap.get(uid.getId());
					int cnt = t.left.decrementAndGet();
					//考虑这个短信拆分后发给上游的个数，与从下游接收的个数不一样
					if(cnt <= 0) {
						//状态收全了，这是最后一个状态了
						//获取早先回复给下游的msgId
						UniqueLongMsgId originUid = t.right.left;
						MsgId msgId = t.right.right.remove(Integer.valueOf(uid.getPknumber()));
						if(msgId!=null) {
							report.setMsgId(msgId); // 重写msgid
							//转发给下游
							writeToEntity(originUid.getEntityId(), msg);
						}
						
						//接收的分片，比发给上游的分片多，把剩下的都回复报告
						Iterator<Entry<Integer,MsgId>> itor = t.right.right.entrySet().iterator();
						while( itor.hasNext()) {
							Entry<Integer,MsgId> entry = itor.next();
							MsgId originMsgId = entry.getValue();
							CmppDeliverRequestMessage cloned = e.clone();
							cloned.setMsgId(new MsgId() );
							cloned.setSequenceNo(DefaultSequenceNumberUtil.getSequenceNo());
							
							//创建一个新的Report对象
							CmppReportRequestMessage newReport = new CmppReportRequestMessage();
							newReport.setDestterminalId(cloned.getSrcterminalId());
							newReport.setMsgId(originMsgId);
							newReport.setSubmitTime(cloned.getReportRequestMessage().getSubmitTime());
							newReport.setDoneTime(cloned.getReportRequestMessage().getDoneTime());
							newReport.setStat("DELIVRD");
							newReport.setSmscSequence(0);
							cloned.setReportRequestMessage(newReport);
							
							writeToEntity(originUid.getEntityId(), cloned);
							itor.remove();
						}
						//清除uidMap
						uidMap.remove(uid.getId());
						
					}else {
						//获取早先回复给下游的msgId
						UniqueLongMsgId originUid = t.right.left;
						MsgId msgId = t.right.right.remove(Integer.valueOf(uid.getPknumber()));
						if(msgId!=null) {
							report.setMsgId(msgId); // 重写msgid
							//转发给下游
							writeToEntity(originUid.getEntityId(), msg);
						}
					}
				}
			}
			
    	}else if (msg instanceof CmppSubmitRequestMessage) {
    		//作为服务端收submit消息
    		CmppSubmitRequestMessage e = (CmppSubmitRequestMessage) msg;
    		CmppSubmitResponseMessage resp = new CmppSubmitResponseMessage(e.getHeader().getSequenceId());
			resp.setResult(0);
			ctx.channel().writeAndFlush(resp);
			
			//如要记录状态报告的
			if(e.getRegisteredDelivery() == 1) {
				//记录response的Msgid ,用于状态报告回复
				UniqueLongMsgId uid = e.getUniqueLongMsgId(); //相同长短信分片uid.getId()相同
				Map<Integer,MsgId> l_msgid = new HashMap<Integer,MsgId>() ;
				l_msgid.put(Integer.valueOf(uid.getPknumber()), resp.getMsgId());
				
				//左值用于记录已接收到状态个数，状态收全了再给下游发
				ImmutablePair<AtomicInteger,ImmutablePair<UniqueLongMsgId,Map<Integer,MsgId>>> p =
						ImmutablePair.of(new AtomicInteger(),ImmutablePair.of(uid, l_msgid) );
				ImmutablePair<AtomicInteger,ImmutablePair<UniqueLongMsgId,Map<Integer,MsgId>>> old = uidMap.putIfAbsent(uid.getId(), p);
				if(old!=null) {
					old.right.right.put(Integer.valueOf(uid.getPknumber()), resp.getMsgId());
				}
			}

			
    	}else if(msg instanceof CmppSubmitResponseMessage) {
    		//作为客户端收response 消息
    		CmppSubmitRequestMessage req =(CmppSubmitRequestMessage) ((CmppSubmitResponseMessage) msg).getRequest();
    		if(req.getRegisteredDelivery() == 1) {
        		MsgId resMsgid = ((CmppSubmitResponseMessage) msg).getMsgId();
        		UniqueLongMsgId uid = req.getUniqueLongMsgId();
        		msgIdMap.putIfAbsent(resMsgid.toString(), uid);
        		ImmutablePair<AtomicInteger,ImmutablePair<UniqueLongMsgId,Map<Integer,MsgId>>> p = uidMap.get(uid.getId());
        		p.left.compareAndSet(0, uid.getPktotal());//收了几个response，就要有几个report
    		}
    	}
    	
    	ctx.fireChannelRead(msg);
    }
    
    private void writeToEntity(String forwardEid,Object msg) {
		EndpointConnector conn = EndpointManager.INS.getEndpointConnector(forwardEid);
		Channel ch = conn.fetch(); //获取连接，保证必写成功
		ch.writeAndFlush(msg);
    }
    
	@Override
	public String name() {
		return "ForwardResponseHander";
	}
}
