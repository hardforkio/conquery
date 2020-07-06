package com.bakdata.conquery.models.config;

import java.io.File;
import java.util.Optional;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.io.xodus.stores.SerializingStore;
import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StorageConfig {

	private File directory = new File("storage");

	private boolean validateOnWrite = false;
	@NotNull
	@Valid
	private XodusConfig xodus = new XodusConfig();

	private boolean useWeakDictionaryCaching = true;
	@NotNull
	private Duration weakCacheDuration = Duration.hours(48);

	@Min(1)
	private int nThreads = Runtime.getRuntime().availableProcessors();

	/**
	 * Flag for the {@link SerializingStore} whether to delete values from the
	 * underlying store, that cannot be mapped to an object anymore.
	 */
	private boolean removeUnreadablesFromStore = false;

	/**
	 * When set, all values that could not be deserialized from the persistent
	 * store, are dump into individual files.
	 */
	private Optional<File> unreadbleDataDumpDirectory = Optional.empty();
}
