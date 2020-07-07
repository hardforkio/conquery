package com.bakdata.conquery.io.mina;

import java.io.InputStream;

public interface CQCoder<OUT> {

	public OUT decode(InputStream inputStream) throws Exception;

	public byte[] encode(OUT message) throws Exception;
}
