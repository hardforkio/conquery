package com.bakdata.conquery.resources.admin.ui.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Frontend Permission -- special type that allows easier handling of permission in Freemarker.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class FEPermission {
	private static final ZoneId TIMEZONE = TimeZone.getDefault().toZoneId();

	private final Set<String> domains;
	private final Set<String> abilities;
	private final Set<String> targets;
	private final String creationTime;
	
	public static FEPermission from(ConqueryPermission cPermission) {
		return new FEPermission(
			cPermission.getDomains(),
			cPermission.getAbilities(),
			cPermission.getInstances(),
			LocalDateTime.ofInstant(cPermission.getCreationTime(), TIMEZONE).format(DateTimeFormatter.ISO_DATE_TIME));
	}
	
	public static List<FEPermission> from(Collection<ConqueryPermission> cPermission) {
		List<FEPermission> fePerms = new ArrayList<>();
		for(ConqueryPermission perm : cPermission) {
			fePerms.add(from(perm));
		}
		return fePerms;
	}

}
