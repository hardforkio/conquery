package com.bakdata.conquery.io.mina;

import com.google.common.primitives.Ints;
import io.dropwizard.util.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

@Slf4j @RequiredArgsConstructor
public class ChunkWriter extends ProtocolEncoderAdapter {

	public static final int HEADER_SIZE = Integer.BYTES;
	
	@Getter @Setter
	private int bufferSize = Ints.checkedCast(Size.megabytes(32).toBytes());
	@SuppressWarnings("rawtypes")
	private final CQCoder coder;

	@SuppressWarnings("unchecked")
	@Override
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		byte[] ch = coder.encode(message);
		int msgLength = ch.length;
		int msgChunkLength = Math.min(msgLength, bufferSize - HEADER_SIZE);
		int remainingLength = msgLength;
		int srcOffset = 0;
		
		// First buffer contains the message length at the beginning 
		IoBuffer buf = IoBuffer.allocate(msgChunkLength + HEADER_SIZE);
		buf.putInt(msgLength).put(ch, srcOffset, msgChunkLength).flip();
		out.write(buf);
		while(true) {
			// Package remaining bytes
			remainingLength -= msgChunkLength;
			srcOffset += msgChunkLength;
			
			if(remainingLength <= 0) {
				return;
			}
			
			msgChunkLength = Math.min(remainingLength, bufferSize);			
			buf = IoBuffer.allocate(msgChunkLength);
			buf.put(ch, srcOffset, msgChunkLength);
			out.write(buf);
			
		}
	}
}