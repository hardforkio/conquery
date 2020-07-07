package com.bakdata.conquery.io.mina;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.worker.NamespaceCollection;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BinaryJacksonCoder implements CQCoder<NetworkMessage<?>> {

	private final Validator validator;
	private final ObjectWriter writer;
	private final ObjectReader reader;

	public BinaryJacksonCoder(NamespaceCollection namespaces, Validator validator) {
		this.validator = validator;
		this.writer = Jackson.BINARY_MAPPER
			.writerFor(NetworkMessage.class)
			.withView(InternalOnly.class);
		this.reader = namespaces
				.injectInto(Jackson.BINARY_MAPPER.readerFor(NetworkMessage.class))
				.without(Feature.AUTO_CLOSE_SOURCE)
				.withView(InternalOnly.class);
	}

	@Override
	public byte[] encode(NetworkMessage<?> message) throws Exception {
		ValidatorHelper.failOnError(log, validator.validate(message), "encoding " + message.getClass().getSimpleName());
		
		return writer.writeValueAsBytes(message);
	}

	@Override
	public NetworkMessage<?> decode(byte[] message) throws Exception {
		return reader.readValue(message);
	}
}
