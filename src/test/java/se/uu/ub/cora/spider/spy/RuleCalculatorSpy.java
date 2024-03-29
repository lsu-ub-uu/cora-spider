/*
 * Copyright 2015, 2018, 2020 Uppsala University Library
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

package se.uu.ub.cora.spider.spy;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.beefeater.authorization.Rule;
import se.uu.ub.cora.beefeater.authorization.RuleImp;
import se.uu.ub.cora.beefeater.authorization.RulePartValuesImp;
import se.uu.ub.cora.data.collected.PermissionTerm;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class RuleCalculatorSpy implements PermissionRuleCalculator {
	public MethodCallRecorder MCR = new MethodCallRecorder();

	public List<Rule> returnedRules;

	public RuleCalculatorSpy() {
		RulePartValuesImp set = new RulePartValuesImp();
		set.add("noValue");

		Rule map = new RuleImp();
		map.addRulePart("NoRulesCalculator", set);

		returnedRules = new ArrayList<>();
		returnedRules.add(map);
	}

	@Override
	public List<Rule> calculateRulesForActionAndRecordType(String action, String recordType) {
		MCR.addCall("action", action, "recordType", recordType);
		MCR.addReturned(returnedRules);
		return returnedRules;
	}

	@Override
	public List<Rule> calculateRulesForActionAndRecordTypeAndCollectedData(String action,
			String recordType, List<PermissionTerm> permissionTerms) {
		MCR.addCall("action", action, "recordType", recordType, "permissionTerms", permissionTerms);
		MCR.addReturned(returnedRules);
		return returnedRules;
	}

}
