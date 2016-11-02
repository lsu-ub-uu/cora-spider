/*
 * Copyright 2016 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */

package se.uu.ub.cora.spider.authorization;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.bookkeeper.data.DataGroup;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.role.RulesProvider;

public final class SpiderAuthorizatorImp implements SpiderAuthorizator {

	private User user;
	private Authorizator authorizator;
	private PermissionRuleCalculator ruleCalculator;
	private RulesProvider rulesProvider;

	private SpiderAuthorizatorImp(SpiderDependencyProvider dependencyProvider,
			Authorizator authorizator, RulesProvider rulesProvider) {
		this.authorizator = authorizator;
		this.rulesProvider = rulesProvider;
		ruleCalculator = dependencyProvider.getPermissionRuleCalculator();
	}

	public static SpiderAuthorizatorImp usingSpiderDependencyProviderAndAuthorizatorAndRulesProvider(
			SpiderDependencyProvider dependencyProvider, Authorizator authorizator,
			RulesProvider rulesProvider) {
		return new SpiderAuthorizatorImp(dependencyProvider, authorizator, rulesProvider);
	}

	@Override
	public boolean userSatisfiesRequiredRules(User user,
			List<Map<String, Set<String>>> requiredRules) {
		this.user = user;
		List<Map<String, Set<String>>> providedRules = getActiveRulesForUser();

		if (!authorizator.providedRulesSatisfiesRequiredRules(providedRules, requiredRules)) {
			return false;
		}
		return true;
	}

	private List<Map<String, Set<String>>> getActiveRulesForUser() {
		List<Map<String, Set<String>>> providedRules = new ArrayList<>();
		user.roles.forEach(roleId -> providedRules.addAll(rulesProvider.getActiveRules(roleId)));
		// THIS IS A SMALL HACK UNTIL WE HAVE RECORDRELATIONS AND CAN READ FROM
		// USER, will be needed for userId, organisation, etc
		providedRules.forEach(rule -> {
			Set<String> userIdValues = new HashSet<>();
			userIdValues.add("system.");
			rule.put("createdBy", userIdValues);
		});
		return providedRules;
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		List<Map<String, Set<String>>> requiredRulesForActionAndRecordType = ruleCalculator
				.calculateRulesForActionAndRecordType(action, recordType);

		return userSatisfiesRequiredRules(user, requiredRulesForActionAndRecordType);
	}

	@Override
	public void checkUserIsAuthorizedForActionOnRecordType(User user, String action,
			String recordType) {
		if (!userIsAuthorizedForActionOnRecordType(user, action, recordType)) {
			throw new AuthorizationException("user:" + user.id
					+ " is not authorized to create a record  of type:" + recordType);
		}
	}

	@Override
	public boolean userIsAuthorizedForActionOnRecordTypeAndRecord(User user, String action,
			String recordType, DataGroup record) {
		List<Map<String, Set<String>>> requiredRules = ruleCalculator
				.calculateRulesForActionAndRecordTypeAndData(action, recordType, record);

		return userSatisfiesRequiredRules(user, requiredRules);
	}

	@Override
	public void checkUserIsAuthorizedForActionOnRecordTypeAndRecord(User user, String action,
			String recordType, DataGroup record) {
		if (!userIsAuthorizedForActionOnRecordTypeAndRecord(user, action, recordType, record)) {
			throw new AuthorizationException("user:" + user.id
					+ " is not authorized to create a record  of type:" + recordType);
		}

	}

}
