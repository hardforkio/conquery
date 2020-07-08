package com.bakdata.conquery.io.mina;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.primitives.Ints;
import io.dropwizard.util.Size;
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
	
	private static final int SUB_BUFFER_SIZE = Ints.checkedCast(Size.megabytes(32).toBytes());
	
	private final CQCoder<?> coder;
	
	@Override
	public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
		SessionState state = (SessionState) session.getAttribute(STATE, new SessionState());
//		int bufStart = in.position();
		int bufEnd = in.limit();
		
		if (!state.isInMsg()) {
			// Start of message object
			state.setInMsg(true);
			state.setMsgTotalLength(in.getLong());
			state.setMsgBufInsertOffset(0);
			List<byte[]> buffers = state.getMessagebuffers();
			long toAlloc = state.getMsgTotalLength() - buffers.size()*SUB_BUFFER_SIZE;
			while (toAlloc > 0) {
				buffers.add(new byte[SUB_BUFFER_SIZE]);
				toAlloc -= SUB_BUFFER_SIZE;
			}
			state.setMessagebuffers(buffers);
			//session.setAttribute(STATE, state);
		}
		
		long nCopyBytes = Math.min(in.remaining(),state.getMsgTotalLength() - state.getMsgBufInsertOffset());
		int idx = state.getMsgBufInsertOffset();
		long finalIdx = idx + nCopyBytes;
		List<byte[]> singleMsgBuf = state.getMessagebuffers();
		
		while(idx < finalIdx) {
			int currentBufferIdx = state.getCurrentBuffer();
			byte[] currentBuffer = singleMsgBuf.get(currentBufferIdx);
			currentBuffer[idx - currentBufferIdx*SUB_BUFFER_SIZE] = in.get();
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
		
		state.invalidate();
		session.setAttribute(STATE, state);
	}
	
	@Data
	private static class SessionState {
		private boolean inMsg;
		private long msgTotalLength = 0;
		private int msgBufInsertOffset = 0;
		private int currentBuffer = 0;
		// We need this buffer because data send as CQPP might exceeds the cummulated IoBuffer 
		private List<byte[]> messagebuffers = new ArrayList<>();
		
		public void invalidate() {
			inMsg = false;
			msgTotalLength = 0;
			msgBufInsertOffset = 0;
			currentBuffer = 0;
			for( byte[] messagebuffer : messagebuffers) {
				Arrays.fill(messagebuffer, (byte) 0);
			}
			
		}
	}
	
}
