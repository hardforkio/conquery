package com.bakdata.conquery.models.preproc.outputs;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.models.exceptions.ParsingException;
import com.bakdata.conquery.models.types.MajorTypeId;
import com.bakdata.conquery.models.types.parser.Parser;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import lombok.Data;
import lombok.ToString;

/**
 * Outputs the current line in the file.
 */
@Data
@ToString
@CPSType(id="LINE", base= OutputDescription.class)
public class LineOutput extends OutputDescription {
	
	private static final long serialVersionUID = 1L;

	@Override
	public boolean isRequired() {
		return true;
	}

	@Override
	public Output createForHeaders(Object2IntArrayMap<String> headers) {
		return new Output() {
			@Override
			protected Object parseLine(String[] row, Parser<?> type, long sourceLine) throws ParsingException {
				return sourceLine;
			}
		};
	}
	@Override
	public MajorTypeId getResultType() {
		return MajorTypeId.INTEGER;
	}
}
