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

package se.uu.ub.cora.spider.record.storage;

import org.testng.Assert;
import org.testng.annotations.Test;

import se.uu.ub.cora.storage.RecordIdGenerator;

public class RecordIdGeneratorTest {
	@Test
	public void testGenerateId() {
		RecordIdGenerator idGenerator = new TimeStampIdGenerator();
		String keyType = idGenerator.getIdForType("type");
		String keyType2 = idGenerator.getIdForType("type2");
		Assert.assertNotEquals(keyType, keyType2,
				"The generated keys should not be equal for two different types");
	}
}
