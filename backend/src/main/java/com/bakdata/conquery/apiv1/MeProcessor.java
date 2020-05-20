package com.bakdata.conquery.apiv1;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.GroupId;
import com.bakdata.conquery.resources.admin.ui.model.FEPermission;
import com.bakdata.conquery.resources.api.MeResource;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * This class holds the logic to back the endpoints provided by {@link MeResource}.
 */
@AllArgsConstructor
public class MeProcessor {

	private final MasterMetaStorage storage;
	
	/**
	 * Generates a summary of a user. It contains its name, the groups it belongs to and its permissions on a dataset.
	 * @param user The user object to gather informations about
	 * @return The information about the user
	 */
	public FEMeInformation getUserInformation(@NonNull User user){
		for(Dataset dataset : storage.getNamespaces().getAllDatasets()) {
			
		}
		
		return FEMeInformation.builder()
			.userName(user.getLabel())
			.groups(FEGroup.from(AuthorizationHelper.getGroupsOf(user, storage)))
			.abitities(FEAbilities.builder()
				.onDataset(null).build()
			.build();
	}

	
	/**
	 * Front end (API) data container to describe a single group.
	 */
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@Data
	public static class FEGroup {

		private GroupId groupId;
		private String label;

		public static FEGroup from(@NonNull Group group) {
			return new FEGroup(group.getId(), group.getLabel());
		}
		
		public static List<FEGroup> from(List<Group> groups) {
			return groups.stream().map(FEGroup::from).collect(Collectors.toList());
		}
	}
	
	/**
	 * Front end (API) data container to describe a single user.
	 */
	@Data
	@Builder
	public static class FEMeInformation {
		private String userName;
		private FEAbilities abilities;
		private List<FEGroup> groups;
	}

	@Data
	@Builder
	public static class FEAbilities {
		private Map<DatasetId,FEDatasetAbilities> onDataset;
	}

	@Data
	@Builder
	public static class FEDatasetAbilities {
		boolean canDownload;
		boolean canUpload;
	}

}
