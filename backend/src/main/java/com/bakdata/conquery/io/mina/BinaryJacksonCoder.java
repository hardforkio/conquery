package com.bakdata.conquery.io.mina;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.InternalOnly;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.messages.network.NetworkMessage;
import com.bakdata.conquery.models.worker.NamespaceCollection;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.primitives.Ints;
import io.dropwizard.util.Size;
import lombok.extern.slf4j.Slf4j;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

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
	public void encode(NetworkMessage<?> message, ProtocolEncoderOutput out) throws Exception {
		ValidatorHelper.failOnError(log, validator.validate(message), "encoding " + message.getClass().getSimpleName());
		OutputStream stream = new OutputStream() {
			
			public static final int HEADER_SIZE = Long.BYTES;
			private int bufferSize = Ints.checkedCast(Size.megabytes(32).toBytes());
			private IoBuffer firstBuffer = null;
			private IoBuffer currentBuffer = null;
			private long messageLength = 0;
			
			@Override
			public void write(int b) throws IOException {
				if(currentBuffer == null) {
					currentBuffer = IoBuffer.allocate(bufferSize);
				}
				
				if(firstBuffer == null) {
					firstBuffer = currentBuffer.position(HEADER_SIZE);
				}

				if(!currentBuffer.hasRemaining()) {
					renewBuffer();
				}
				currentBuffer.put((byte) b);
				messageLength++;
				
			}
			
			private void renewBuffer() {
				// submit current Buffer
				out.write(currentBuffer.flip());
				currentBuffer = IoBuffer.allocate(bufferSize);
			}
			
			
			@Override
			public void close() throws IOException {
				if(firstBuffer == null) {
					throw new IllegalStateException("There is no first Buffer. No value was written.");
				}
				firstBuffer.putLong(0, messageLength);
				out.write(currentBuffer.flip());
			}
		};
		writer.writeValue(stream,message);
		stream.close();
	}

	@Override
	public void decode(List<byte[]> message, ProtocolDecoderOutput out) throws Exception {
		Object decoded = reader.readValue(new InputStream() {
			private int listIdx = 0;
			private int bufferIdx = 0;
			@Override
			public int read() throws IOException {
				if(listIdx >= message.size()) {
					throw new IllegalStateException("Requested more bytes than present");
				}
				if(listIdx == message.size()-1 && message.get(listIdx).length == bufferIdx) {
					return -1;
				}
				byte val = message.get(listIdx)[bufferIdx];
				bufferIdx++;
				if(bufferIdx >= Integer.MAX_VALUE) {
					bufferIdx = 0;
					listIdx++;
				}
				return val;
			}
			
		});
		
		out.write(decoded);
	}
}
