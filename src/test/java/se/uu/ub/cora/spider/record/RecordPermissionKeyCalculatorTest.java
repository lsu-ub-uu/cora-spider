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

import java.util.Optional;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;
import se.uu.ub.cora.bookkeeper.data.DataGroup;

public class RecordPermissionKeyCalculatorTest {
	@Test
	public void testGeneratePermissionKey() {
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();

		DataGroup recordInfo = DataGroup.withNameInData("recordInfo");

		Set<String> keys = keyCalculator.calculateKeys("READ", "recordType", recordInfo);
		Optional<String> key = keys.stream().findFirst();
		Assert.assertEquals(key.get(), "READ:RECORDTYPE:SYSTEM:*",
				"Key should be calculated to match example");
	}

	@Test
	public void testCalculateKeyForList() {
		PermissionKeyCalculator keyCalculator = new RecordPermissionKeyCalculator();
		Set<String> keys = keyCalculator.calculateKeysForList("READ", "recordType");
		Optional<String> key = keys.stream().findFirst();
		Assert.assertEquals(key.get(), "READ:RECORDTYPE:SYSTEM:*",
				"Key should be calculated to match example");

	}
}
