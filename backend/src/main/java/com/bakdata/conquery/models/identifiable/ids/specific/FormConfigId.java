package com.bakdata.conquery.models.identifiable.ids.specific;

import java.util.List;
import java.util.UUID;

import com.bakdata.conquery.apiv1.forms.FormConfig;
import com.bakdata.conquery.models.identifiable.ids.AId;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.IdIterator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@AllArgsConstructor @Getter @EqualsAndHashCode(callSuper=false)
public class FormConfigId extends AId<FormConfig> {
	

	private String formType;
	private UUID id;

	@Override
	public void collectComponents(List<Object> components) {
		components.add(formType);
		components.add(id);
		
	}
	
	public static enum Parser implements IId.Parser<FormConfigId> {
		INSTANCE;
		
		@Override
		public FormConfigId parseInternally(IdIterator parts) {
			UUID id = UUID.fromString(parts.next());
			String formType = parts.next();
			return new FormConfigId(formType, id);
		}
	}
}
