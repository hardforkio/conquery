package com.bakdata.conquery.io.mina;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

@Slf4j @RequiredArgsConstructor
public class ChunkReader extends ProtocolDecoderAdapter {
	
	private static final AttributeKey STATE = new AttributeKey(SessionState.class, "sessionState");
	
	private final CQCoder<?> coder;
	
	@Override
	public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
		SessionState state = (SessionState) session.getAttribute(STATE);
//		int bufStart = in.position();
		int bufEnd = in.limit();
		
		if (state == null) {
			// Start of message object
			state = new SessionState();
			state.setMsgTotalLength(in.getInt());
			state.setMsgBufInsertOffset(0);
			state.setMessagebuf(new byte[state.getMsgTotalLength()]);
			session.setAttribute(STATE, state);
		}
		
		int nCopyBytes = Math.min(in.remaining(),state.getMsgTotalLength() - state.getMsgBufInsertOffset());
		int idx = state.getMsgBufInsertOffset();
		int finalIdx = idx + nCopyBytes;
		byte[] singleMsgBuf = state.getMessagebuf();
		
		while(idx < finalIdx) {
			singleMsgBuf[idx] = in.get();
			idx++;
		}
		
		state.setMsgBufInsertOffset(idx);
		
		in.limit(bufEnd);
		
		if(state.getMsgBufInsertOffset() < state.getMsgTotalLength()) {
			// Wait for more bytes
			return;
		}

		try {
			out.write(coder.decode(state.getMessagebuf()));			
		}
		catch (Exception e) {
			log.error("Could not decode cumulated message",e);
		}
		
		session.removeAttribute(STATE);		
	}
	
	@Data
	private static class SessionState {
		private int msgTotalLength = 0;
		private int msgBufInsertOffset = 0;
		// We need this buffer because data send as CQPP might exceeds the cummulated IoBuffer 
		private byte[] messagebuf;
	}
	
}
