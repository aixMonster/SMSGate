package com.zx.sms.codec.cmpp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.junit.Assert;
import org.junit.Test;

import com.zx.sms.codec.AbstractTestMessageCodec;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;

public class Test10085MsgDataSubmitRequestDecoder extends AbstractTestMessageCodec<CmppSubmitRequestMessage>{
	@Override
	protected int getVersion(){
		return 0x20;
	}
	
	@Test
	public void testDecode()
	{
		ByteBuf buf = Unpooled.wrappedBuffer(prepareMsgData());
		ch.writeInbound(buf);
		CmppSubmitRequestMessage result = null;
		boolean success = false;
		while(null!= ( result =  (CmppSubmitRequestMessage)ch.readInbound())){
			
			Assert.assertNotNull(result);
			Assert.assertNotNull(result.getUniqueLongMsgId().getId());
			Assert.assertEquals(super.EndPointID+"."+super.ch.id().asShortText()+".1816483568710085.46.3",result.getUniqueLongMsgId().getId());
			ch.writeOutbound(result);
			success = true;
		}
		Assert.assertTrue(success);
		ByteBuf bufread;
		while(( bufread = (ByteBuf)ch.readOutbound())!=null){
			bufread.release();
		}
		
	}
	
	//下面数据截取自现网10085的报文。cmppSubmit2.0协议
	private byte[] prepareMsgData()
	{
		return new byte[]{(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x2b,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x04,(byte)0x00,(byte)0x7c,(byte)0xe0,(byte)0x44,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x01,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x08,(byte)0x34,(byte)0x30,(byte)0x30,(byte)0x34,(byte)0x33
				,(byte)0x37,(byte)0x30,(byte)0x31,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x31,(byte)0x30,(byte)0x30,(byte)0x38,(byte)0x35
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x01,(byte)0x31,(byte)0x38,(byte)0x31,(byte)0x36,(byte)0x34,(byte)0x38,(byte)0x33,(byte)0x35,(byte)0x36,(byte)0x38,(byte)0x37,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x8c,(byte)0x05,(byte)0x00,(byte)0x03,(byte)0x2e,(byte)0x03,(byte)0x01,(byte)0x20,(byte)0x1c,(byte)0x00
				,(byte)0x23,(byte)0x75,(byte)0xaf,(byte)0x72,(byte)0xc2,(byte)0x59,(byte)0x27,(byte)0x96,(byte)0x4d,(byte)0x4e,(byte)0xf7,(byte)0xff,(byte)0x0c,(byte)0x65,(byte)0xb0,(byte)0x54
				,(byte)0xc1,(byte)0x62,(byte)0xa2,(byte)0x4e,(byte)0x0d,(byte)0x50,(byte)0x5c,(byte)0x20,(byte)0x1d,(byte)0x00,(byte)0x40,(byte)0x5c,(byte)0x0f,(byte)0x7c,(byte)0x73,(byte)0x79
				,(byte)0x5e,(byte)0x67,(byte)0x3a,(byte)0x60,(byte)0xca,(byte)0x55,(byte)0x9c,(byte)0x4e,(byte)0x0d,(byte)0x65,(byte)0xad,(byte)0x30,(byte)0x01,(byte)0x00,(byte)0x40,(byte)0x91
				,(byte)0x77,(byte)0x6d,(byte)0x3e,(byte)0x59,(byte)0x27,(byte)0x79,(byte)0x5e,(byte)0x56,(byte)0xfd,(byte)0x6c,(byte)0x11,(byte)0x00,(byte)0x34,(byte)0x00,(byte)0x47,(byte)0x79
				,(byte)0x5e,(byte)0x56,(byte)0x68,(byte)0x75,(byte)0xaf,(byte)0x72,(byte)0xc2,(byte)0x96,(byte)0x4d,(byte)0x4e,(byte)0xf7,(byte)0x30,(byte)0x01,(byte)0x00,(byte)0x40,(byte)0x9b
				,(byte)0x45,(byte)0x84,(byte)0xdd,(byte)0x00,(byte)0x4e,(byte)0x00,(byte)0x4f,(byte)0x00,(byte)0x54,(byte)0x00,(byte)0x45,(byte)0x00,(byte)0x32,(byte)0x65,(byte)0xb0,(byte)0x54
				,(byte)0xc1,(byte)0x5e,(byte)0x26,(byte)0x56,(byte)0xde,(byte)0x5b,(byte)0xb6,(byte)0xff,(byte)0x0c,(byte)0x5b,(byte)0x98,(byte)0x7f,(byte)0x51,(byte)0x53,(byte)0x9f,(byte)0x4e
				,(byte)0xf7,(byte)0x00,(byte)0x2c,(byte)0x78,(byte)0x6e,(byte)0x4f,(byte)0xdd,(byte)0x6b,(byte)0x63,(byte)0x54,(byte)0xc1,(byte)0x30,(byte)0x02,(byte)0x00,(byte)0x31,(byte)0x00
				,(byte)0x30,(byte)0x00,(byte)0x30,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x2b,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x04,(byte)0x00,(byte)0x7c,(byte)0xe0,(byte)0x45,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x01,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x08,(byte)0x34,(byte)0x30,(byte)0x30,(byte)0x34,(byte)0x33
				,(byte)0x37,(byte)0x30,(byte)0x31,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x31,(byte)0x35,(byte)0x30,(byte)0x37,(byte)0x33,(byte)0x31
				,(byte)0x32,(byte)0x32,(byte)0x34,(byte)0x38,(byte)0x32,(byte)0x30,(byte)0x30,(byte)0x33,(byte)0x32,(byte)0x2b,(byte)0x00,(byte)0x31,(byte)0x30,(byte)0x30,(byte)0x38,(byte)0x35
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x01,(byte)0x31,(byte)0x38,(byte)0x31,(byte)0x36,(byte)0x34,(byte)0x38,(byte)0x33,(byte)0x35,(byte)0x36,(byte)0x38,(byte)0x37,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x8c,(byte)0x05,(byte)0x00,(byte)0x03,(byte)0x2e,(byte)0x03,(byte)0x02,(byte)0x00,(byte)0x38,(byte)0x00
				,(byte)0x35,(byte)0x72,(byte)0x06,(byte)0x6b,(byte)0x3e,(byte)0x62,(byte)0x4b,(byte)0x67,(byte)0x3a,(byte)0x4e,(byte)0x13,(byte)0x7e,(byte)0xbf,(byte)0xff,(byte)0x0c,(byte)0x65
				,(byte)0xb9,(byte)0x4f,(byte)0xbf,(byte)0x8d,(byte)0x2d,(byte)0x4e,(byte)0x70,(byte)0x4e,(byte)0x0d,(byte)0x75,(byte)0x28,(byte)0x62,(byte)0xa2,(byte)0xff,(byte)0x0c,(byte)0x7b
				,(byte)0x49,(byte)0x4f,(byte)0x60,(byte)0x67,(byte)0x65,(byte)0x7e,(byte)0xa6,(byte)0x55,(byte)0x94,(byte)0xff,(byte)0x01,(byte)0x00,(byte)0x20,(byte)0x59,(byte)0x82,(byte)0x97
				,(byte)0x00,(byte)0x8d,(byte)0x2d,(byte)0x4e,(byte)0x70,(byte)0x66,(byte)0xf4,(byte)0x59,(byte)0x1a,(byte)0x72,(byte)0x06,(byte)0x6b,(byte)0x3e,(byte)0x62,(byte)0x4b,(byte)0x67
				,(byte)0x3a,(byte)0xff,(byte)0x0c,(byte)0x73,(byte)0x1b,(byte)0x62,(byte)0x33,(byte)0x00,(byte)0x20,(byte)0x00,(byte)0x68,(byte)0x00,(byte)0x74,(byte)0x00,(byte)0x74,(byte)0x00
				,(byte)0x70,(byte)0x00,(byte)0x3a,(byte)0x00,(byte)0x2f,(byte)0x00,(byte)0x2f,(byte)0x00,(byte)0x77,(byte)0x00,(byte)0x77,(byte)0x00,(byte)0x77,(byte)0x00,(byte)0x2e,(byte)0x00
				,(byte)0x31,(byte)0x00,(byte)0x30,(byte)0x00,(byte)0x30,(byte)0x00,(byte)0x38,(byte)0x00,(byte)0x35,(byte)0x00,(byte)0x2e,(byte)0x00,(byte)0x63,(byte)0x00,(byte)0x6e,(byte)0x00
				,(byte)0x2f,(byte)0x00,(byte)0x77,(byte)0x00,(byte)0x65,(byte)0x00,(byte)0x62,(byte)0x00,(byte)0x38,(byte)0x00,(byte)0x35,(byte)0x00,(byte)0x2f,(byte)0x00,(byte)0x68,(byte)0x00
				,(byte)0x35,(byte)0x00,(byte)0x2f,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0xdd,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x04,(byte)0x00,(byte)0x7c,(byte)0xe0,(byte)0x46,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x01,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x01,(byte)0x08,(byte)0x34,(byte)0x30,(byte)0x30,(byte)0x34,(byte)0x33
				,(byte)0x37,(byte)0x30,(byte)0x31,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x31,(byte)0x35,(byte)0x30,(byte)0x37,(byte)0x33,(byte)0x31
				,(byte)0x32,(byte)0x32,(byte)0x34,(byte)0x38,(byte)0x32,(byte)0x35,(byte)0x30,(byte)0x33,(byte)0x32,(byte)0x2b,(byte)0x00,(byte)0x31,(byte)0x30,(byte)0x30,(byte)0x38,(byte)0x35
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x01,(byte)0x31,(byte)0x38,(byte)0x31,(byte)0x36,(byte)0x34,(byte)0x38,(byte)0x33,(byte)0x35,(byte)0x36,(byte)0x38,(byte)0x37,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
				,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x3e,(byte)0x05,(byte)0x00,(byte)0x03,(byte)0x2e,(byte)0x03,(byte)0x03,(byte)0x00,(byte)0x73,(byte)0x00
				,(byte)0x68,(byte)0x00,(byte)0x6f,(byte)0x00,(byte)0x77,(byte)0x00,(byte)0x4c,(byte)0x00,(byte)0x69,(byte)0x00,(byte)0x73,(byte)0x00,(byte)0x74,(byte)0x00,(byte)0x2e,(byte)0x00
				,(byte)0x68,(byte)0x00,(byte)0x74,(byte)0x00,(byte)0x6d,(byte)0x00,(byte)0x6c,(byte)0x00,(byte)0x20,(byte)0x00,(byte)0x20,(byte)0x00,(byte)0x20,(byte)0x76,(byte)0xf4,(byte)0x63
				,(byte)0xa5,(byte)0x8d,(byte)0x2d,(byte)0x4e,(byte)0x70,(byte)0x30,(byte)0x02,(byte)0x8b,(byte)0xe6,(byte)0x8b,(byte)0xe2,(byte)0x00,(byte)0x31,(byte)0x00,(byte)0x30,(byte)0x00
				,(byte)0x30,(byte)0x00,(byte)0x38,(byte)0x00,(byte)0x35,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00
};
	}

}
