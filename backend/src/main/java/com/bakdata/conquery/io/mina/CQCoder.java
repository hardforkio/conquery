package com.bakdata.conquery.io.mina;

import java.util.List;

import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public interface CQCoder<OUT> {

	public void decode(List<byte[]> list, ProtocolDecoderOutput out) throws Exception;

	public void encode(OUT message, ProtocolEncoderOutput out) throws Exception;
}
