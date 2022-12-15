package com.zx.sms.codec.cmpp.wap;

import java.io.Serializable;
import java.net.SocketAddress;

import com.zx.sms.BaseMessage;
import com.zx.sms.LongSMSMessage;
import com.zx.sms.connect.manager.EndpointEntity;

import io.netty.channel.Channel;

public class UniqueLongMsgId implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String id;
	private String entityId;
	private SocketAddress remoteAddr;
	private SocketAddress localAddr;
	private long timestamp;
	private int sequenceNo;
	private short pkseq ;
	private byte pktotal ;
	private byte pknumber ;
	
	//限制外部业务类创建该ID,该ID只在长短信合并时生成
	UniqueLongMsgId(String id){
		this.id = id;
	}
	
	//一个长短信的标识，从哪里来的：什么时间，从哪个账号的哪个连接来的
	UniqueLongMsgId(EndpointEntity entity,Channel ch ,LongSMSMessage lmsg){
		String srcIdAndDestId = lmsg.getSrcIdAndDestId();
		String channelId = (entity!=null && !entity.isRecvLongMsgOnMultiLink())? ch.id().asShortText(): "";
		
		StringBuilder sb = entity == null ? new StringBuilder(srcIdAndDestId)  : new StringBuilder(entity.getId()).append(".").append(channelId).append(".").append(srcIdAndDestId);
		StringBuilder mapKeyBuilder = new StringBuilder(sb.toString());
		LongMessageFrame frame = lmsg.generateFrame();
		FrameHolder fh =  LongMessageFrameHolder.INS.parseFrameKey(frame);
		if(fh != null) {
			mapKeyBuilder.append(".").append(fh.frameKey).append(".").append(fh.getTotalLength());
		}
		this.id = mapKeyBuilder.toString();
		this.entityId = entity!=null?entity.getId():null;
		this.remoteAddr = ch.remoteAddress();
		this.localAddr = ch.localAddress();
		this.timestamp = ((BaseMessage)lmsg).getTimestamp();
		this.sequenceNo = ((BaseMessage)lmsg).getSequenceNo();
		this.pknumber = frame.getPknumber();
		this.pktotal = frame.getPktotal();
		this.pkseq  = frame.getPkseq();
	}
	
	UniqueLongMsgId(UniqueLongMsgId id ,LongMessageFrame frame ){
		this.id = id.getId();
		this.entityId = id.getEntityId();
		this.remoteAddr = id.getRemoteAddr();
		this.localAddr = id.getLocalAddr();
		this.timestamp = frame.getTimestamp();
		this.sequenceNo = frame.getSequence();
		this.pknumber = frame.getPknumber();
		this.pktotal = frame.getPktotal();
		this.pkseq  = frame.getPkseq();
	}
	
	public String getId() {
		return id;
	}

	public String getEntityId() {
		return entityId;
	}

	public SocketAddress getRemoteAddr() {
		return remoteAddr;
	}

	public SocketAddress getLocalAddr() {
		return localAddr;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public int getSequenceNo() {
		return sequenceNo;
	}

	public short getPkseq() {
		return pkseq;
	}

	public byte getPktotal() {
		return pktotal;
	}

	public byte getPknumber() {
		return pknumber;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UniqueLongMsgId other = (UniqueLongMsgId) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UniqueLongMsgId [id=" + id + ", entityId=" + entityId + ", remoteAddr=" + remoteAddr + ", localAddr="
				+ localAddr + ", timestamp=" + timestamp + ", sequenceNo=" + sequenceNo + ", pkseq=" + pkseq
				+ ", pktotal=" + pktotal + ", pknumber=" + pknumber + "]";
	}

}
