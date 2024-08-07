/*
 * Copyright 2024 Uppsala University Library
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
package se.uu.ub.cora.spider.extended.apptoken;

import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AppTokenGeneratorTest {

	private AppTokenGenerator appTokenGenerator;

	@BeforeMethod
	public void beforeMethod() {
		appTokenGenerator = new AppTokenGeneratorImp();
	}

	@Test
	public void testInit() throws Exception {
		assertTrue(appTokenGenerator instanceof AppTokenGenerator);
	}

	@Test
	public void testGenerateAppToken() {
		String token1 = appTokenGenerator.generateAppToken();
		String token2 = appTokenGenerator.generateAppToken();

		assertTrue(token1.length() > 30);
		assertTrue(token2.length() > 30);
		assertNotEquals(token1, token2);
	}
}
