package com.bakdata.conquery.io.mina;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

@Slf4j @RequiredArgsConstructor
public class ChunkReader extends CumulativeProtocolDecoder {
	
	private static final AttributeKey STATE = new AttributeKey(SessionState.class, "sessionState");
	
	private final CQCoder<?> coder;
	
	@Override
	protected boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) {
		SessionState state = (SessionState) session.getAttribute(STATE, new SessionState());
		int bufStart = in.position();
		int bufEnd = in.limit();
		if (!state.isInMessage()) {
			// Start of message object
			state.setInMessage(true);
			state.setMsgTotalLength(in.getInt());
			state.setMsgStartMarker(in.position());
		}
		
		if (in.remaining() < state.getMsgTotalLength()) {
			// We need more bytes for accummulation
			in.position(bufStart);
			in.limit(bufEnd);
			return false;
		}
		
		in.position(state.getMsgStartMarker());
		in.limit(state.getMsgStartMarker() + state.msgTotalLength);
		
		try {
			out.write(coder.decode(in.slice().asInputStream()));			
		}
		catch (Exception e) {
			log.error("Could not decode cumulated message",e);
		} finally {
			in.position(in.limit());
			state.setInMessage(false);
		}
		return true;
	}
	
	@Data
	private static class SessionState {
		private boolean inMessage = false;
		private int msgTotalLength = 0;
		private int msgStartMarker = 0;
	}
	
}
