package com.bakdata.conquery.io.mina;

public interface CQCoder<OUT> {

	public OUT decode(byte[] bs) throws Exception;

	public byte[] encode(OUT message) throws Exception;
}
