package com.bakdata.conquery.io.mina;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

@Slf4j @RequiredArgsConstructor
public class ChunkWriter extends ProtocolEncoderAdapter {

	@SuppressWarnings("rawtypes")
	private final CQCoder coder;

	@SuppressWarnings("unchecked")
	@Override
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		coder.encode(message, out);
	}
}