/*
 * Copyright 2016, 2025 Uppsala University Library
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

package se.uu.ub.cora.spider.authorization.internal;

import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.beefeater.Authorizator;
import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class BeefeaterAuthorizatorSpy implements Authorizator {

	MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public BeefeaterAuthorizatorSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("getUserIsAuthorizedForPemissionUnit", () -> false);
	}

	/**
	 * providedRulesSatisfiesRequiredRules is default true, is used as return value for
	 * {@link #providedRulesSatisfiesRequiredRules(List, List)}
	 */
	public boolean providedRulesSatisfiesRequiredRules = true;

	/**
	 * returnNoMatchedRules is default false, then
	 * {@link #providedRulesMatchRequiredRules(List, List)} returns a created List with
	 * providedRules, when the flag is set to true, is an empty list returned instead.
	 */
	public boolean returnNoMatchedRules = false;

	@Override
	public boolean providedRulesSatisfiesRequiredRules(List<Rule> providedRules,
			List<Rule> requiredRules) {
		MCR.addCall("providedRules", providedRules, "requiredRules", requiredRules);
		MCR.addReturned(providedRulesSatisfiesRequiredRules);
		return providedRulesSatisfiesRequiredRules;
	}

	@Override
	public List<Rule> providedRulesMatchRequiredRules(List<Rule> providedRules,
			List<Rule> requiredRules) {
		MCR.addCall("providedRules", providedRules, "requiredRules", requiredRules);
		List<Rule> matchedRules = providedRules;
		if (returnNoMatchedRules) {
			matchedRules = Collections.emptyList();
		}
		MCR.addReturned(matchedRules);
		return matchedRules;
	}

	@Override
	public boolean getUserIsAuthorizedForPemissionUnit(User user, String recordPermissionUnit) {
		return (boolean) MCR.addCallAndReturnFromMRV("user", user, "recordPermissionUnit",
				recordPermissionUnit);
	}
}
