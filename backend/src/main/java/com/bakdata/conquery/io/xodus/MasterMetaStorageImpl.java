package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.IdentifiableStore;
import com.bakdata.conquery.io.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespaces;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterMetaStorageImpl extends ConqueryStorageImpl implements MasterMetaStorage, ConqueryStorage {
	
	private final SingletonStore<Namespaces> meta;
	private final IdentifiableStore<ManagedQuery> queries;
	private final IdentifiableStore<User> authUser;
	private final IdentifiableStore<ConqueryPermission> authPermissions;
	private final IdentifiableStore<Mandator> authMandator;

	public MasterMetaStorageImpl(Namespaces namespaces, Validator validator, StorageConfig config) {
		super(
			validator,
			config,
			new File(config.getDirectory(), "meta")
		);
		this.meta = StoreInfo.NAMESPACES.singleton(this);
		this.queries = StoreInfo.QUERIES.identifiable(this, namespaces);
		
		MasterMetaStorage storage = this;
		this.authMandator = new IdentifiableStore<>(
					storage.getCentralRegistry(),
					StoreInfo.AUTH_MANDATOR.cached(storage)
				){
			@Override
			protected void addToRegistry(CentralRegistry centralRegistry, Mandator value) throws Exception {
				value.setStorage(storage);
			}
		};
		this.authUser = new IdentifiableStore<>(
				storage.getCentralRegistry(),
				StoreInfo.AUTH_USER.cached(storage)
			){
			@Override
			protected void addToRegistry(CentralRegistry centralRegistry, User value) throws Exception {
				value.setStorage(storage);
			}
		};
		this.authPermissions = new IdentifiableStore<>(
				storage.getCentralRegistry(),
				StoreInfo.AUTH_PERMISSIONS.cached(storage)
			){
			@Override
			protected void addToRegistry(CentralRegistry centralRegistry, ConqueryPermission value) throws Exception {
				value.getOwnerId().getOwner(storage).addPermissionLocal(value);
			}
			
			@Override
			protected void removeFromRegistry(CentralRegistry centralRegistry, ConqueryPermission value) {
				value.getOwnerId().getOwner(storage).removePermissionLocal(value);
			}
		};
	}

	@Override
	public void stopStores() throws IOException {
		super.stopStores();
		authMandator.close();
		authPermissions.close();
		authUser.close();
		queries.close();
		meta.close();
	}

	@Override
	public void addQuery(ManagedQuery query) throws JSONException {
		queries.add(query);
	}

	@Override
	public ManagedQuery getQuery(ManagedQueryId id) {
		return queries.get(id);
	}

	@Override
	public Collection<ManagedQuery> getAllQueries() {
		return queries.getAll();
	}

	@Override
	public void updateQuery(ManagedQuery query) throws JSONException {
		queries.update(query);
	}

	@Override
	public void removeQuery(ManagedQueryId id) {
		queries.remove(id);
	}
	
	/*
	@Override
	public Namespaces getMeta() {
		return meta.get();
	}

	@Override
	public void updateMeta(Namespaces meta) throws JSONException {
		this.meta.update(meta);
		//see #147 ?
		/*
		if(blockManager != null) {
			blockManager.init(slaveInfo);
		}
		*/
	//}
	
	public void addPermission(ConqueryPermission permission) throws JSONException{
		authPermissions.add(permission);
	}
	
	public Collection<ConqueryPermission> getAllPermissions(){
		return authPermissions.getAll();
	}
	
	public void removePermission(PermissionId permissionId){
		authPermissions.remove(permissionId);
	}
	
	public void removePermissionAll() {
		for(ConqueryPermission p :authPermissions.getAll()) {
			authPermissions.remove(p.getId());
		}
	}
	
	public void addUser(User user) throws JSONException {
		authUser.add(user);
	}
	
	public Optional<User> getUser(UserId userId) {
		return Optional.ofNullable(authUser.get(userId));
	}
	
	public Collection<User> getAllUsers(){
		return authUser.getAll();
	}
	
	public void removeUser(UserId userId) {
		authUser.remove(userId);
	}
	
	public void removeUserAll() {
		for(User u :authUser.getAll()) {
			authUser.remove(u.getId());
		}
	}

	public void addMandator(Mandator mandator) throws JSONException {
		authMandator.add(mandator);
	}
	
	public Optional<Mandator> getMandator(MandatorId mandatorId) {
		return Optional.ofNullable(authMandator.get(mandatorId));
	}
	
	@Override
	public Collection<Mandator> getAllMandators() {
		return authMandator.getAll();
	}
	
	public void removeMandator(MandatorId mandatorId)  {
		authMandator.remove(mandatorId);
	}
	
	public void removeMandatorAll() {
		for(Mandator m :authMandator.getAll()) {
			authMandator.remove(m.getId());
		}
	}

	@Override
	public void updateUser(User user) throws JSONException {
		authUser.update(user);
	}

	@Override
	public ConqueryPermission getPermission(PermissionId id) {
		return authPermissions.get(id);
	}

	@Override
	public Set<ConqueryPermission> getPermissions(PermissionOwnerId<?> ownerId) {
		return ownerId.getOwner(this).getPermissions();
	}

	@Override
	public void updateMandator(Mandator mandator) throws JSONException {
		authMandator.update(mandator);
	}
}
