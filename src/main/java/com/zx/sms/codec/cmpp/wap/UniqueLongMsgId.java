package com.zx.sms.codec.cmpp.wap;

import java.io.Serializable;

public class UniqueLongMsgId implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String id;
	
	//限制外部业务类创建该ID,该ID只在长短信合并时生成
	UniqueLongMsgId(String id){
		this.id = id;
	}
	public String getId() {
		return id;
	}

}
