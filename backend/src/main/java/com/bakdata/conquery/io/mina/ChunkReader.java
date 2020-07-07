package com.bakdata.conquery.io.mina;

import java.util.ArrayList;
import java.util.List;

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
			state.setMsgTotalLength(in.getLong());
			state.setMsgBufInsertOffset(0);
			List<byte[]> buffers = new ArrayList<>();
			long toAlloc = state.getMsgTotalLength();
			while (toAlloc > Integer.MAX_VALUE) {
				buffers.add(new byte[Integer.MAX_VALUE]);
				toAlloc -= Integer.MAX_VALUE;
			}
			buffers.add(new byte[(int) toAlloc]);
			state.setMessagebuffers(buffers);
			session.setAttribute(STATE, state);
		}
		
		long nCopyBytes = Math.min(in.remaining(),state.getMsgTotalLength() - state.getMsgBufInsertOffset());
		int idx = state.getMsgBufInsertOffset();
		long finalIdx = idx + nCopyBytes;
		List<byte[]> singleMsgBuf = state.getMessagebuffers();
		
		while(idx < finalIdx) {
			int currentBufferIdx = state.getCurrentBuffer();
			byte[] currentBuffer = singleMsgBuf.get(currentBufferIdx);
			currentBuffer[idx - currentBufferIdx*Integer.MAX_VALUE] = in.get();
			idx++;
			if(idx % Integer.MAX_VALUE == 0) {
				state.setCurrentBuffer(currentBufferIdx + 1);
			}
		}
		
		state.setMsgBufInsertOffset(idx);
		
		in.limit(bufEnd);
		
		if(state.getMsgBufInsertOffset() < state.getMsgTotalLength()) {
			// Wait for more bytes
			return;
		}

		try {
			coder.decode(state.getMessagebuffers(), out);			
		}
		catch (Exception e) {
			log.error("Could not decode cumulated message",e);
		}
		
		session.removeAttribute(STATE);		
	}
	
	@Data
	private static class SessionState {
		private long msgTotalLength = 0;
		private int msgBufInsertOffset = 0;
		// We need this buffer because data send as CQPP might exceeds the cummulated IoBuffer 
		private List<byte[]> messagebuffers;
		private int currentBuffer = 0;
	}
	
}
