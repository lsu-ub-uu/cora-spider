/*
 * Copyright 2015 Uppsala University Library
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

package se.uu.ub.cora.spider.record;

import java.util.HashSet;
import java.util.Set;

import se.uu.ub.cora.bookkeeper.data.DataGroup;

public class KeyCalculatorSpy implements PermissionKeyCalculator {

	public boolean calculateKeysWasCalled = false;

	@Override
	public Set<String> calculateKeys(String accessType, String recordType, DataGroup record) {
		calculateKeysWasCalled = true;
		Set<String> keys = new HashSet<>();
		String key = String.join(":", accessType, recordType.toUpperCase(), "SYSTEM", "*");
		keys.add(key);
		return keys;
	}

	@Override
	public Set<String> calculateKeysForList(String accessType, String recordType) {
		// TODO Auto-generated method stub
		return null;
	}

}
