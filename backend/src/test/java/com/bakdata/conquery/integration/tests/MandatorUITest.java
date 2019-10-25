package com.bakdata.conquery.integration.tests;

import static com.bakdata.conquery.resources.ResourceConstants.ROLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.bakdata.conquery.integration.IntegrationTest;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.subjects.Role;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.resources.admin.ui.RoleUIResource;
import com.bakdata.conquery.resources.hierarchies.HierarchyHelper;
import com.bakdata.conquery.util.support.StandaloneSupport;

/**
 * Tests the mandator UI interface. Before the request is done, a mandator, a
 * user and a permission is created and stored. Then the request response is
 * tested against the created entities.
 *
 */
public class MandatorUITest implements ProgrammaticIntegrationTest, IntegrationTest.Simple {


	private MasterMetaStorage storage;
	private Role mandator = new Role("testMandatorName", "testMandatorLabel");
	private RoleId mandatorId = mandator.getId();
	private User user = new User("testUser@test.de", "testUserName");
	private UserId userId = user.getId();
	private ConqueryPermission permission = new DatasetPermission(Ability.READ.asSet(), new DatasetId("testDatasetId"));

	@Override
	public void execute(StandaloneSupport conquery) throws Exception {
		try {
	
			storage = conquery.getStandaloneCommand().getMaster().getStorage();
			try {
				storage.addRole(mandator);
				storage.addUser(user);
				// override permission object, because it might have changed by the subject
				// owning the permission
				permission = mandator.addPermission(storage, permission);
				user.addRole(storage, mandator);
			}
			catch (JSONException e) {
				fail("Failed when adding to storage.",e);
			}
			
			String base = String.format("http://localhost:%d/admin/", conquery.getAdminPort());
			URI classBase = HierarchyHelper.fromHierachicalPathResourceMethod(base, RoleUIResource.class, "getRole")
			.buildFromMap(Map.of(ROLE_NAME, mandatorId.toString()));
	
			Response response = conquery
				.getClient()
				.target(classBase)
				.request()
				.get();
	
			assertThat(response.getStatus()).isEqualTo(200);
			assertThat(response.readEntity(String.class))
				// check permission
				.contains(permission.getClass().getSimpleName(), permission.getTarget().toString())
				.containsSubsequence((Iterable<String>) () -> permission.getAbilities().stream().map(Enum::name).iterator())
				// check user
				.contains(user.getLabel());

		}
		finally {
			storage.removeRole(mandatorId);
			storage.removeUser(userId);
		}
	}

}
