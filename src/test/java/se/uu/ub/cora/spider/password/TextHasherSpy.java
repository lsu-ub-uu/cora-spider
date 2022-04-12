/*
 * Copyright 2022 Uppsala University Library
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
package se.uu.ub.cora.spider.password;

import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class TextHasherSpy implements TextHasher {

	MethodCallRecorder MCR = new MethodCallRecorder();

	@Override
	public String hashText(String plainText) {
		MCR.addCall("plainText", plainText);

		return null;
	}

	@Override
	public boolean matches(String plainText, String hashedText) {
		MCR.addCall("plainText", plainText, "hashedText", hashedText);

		return false;
	}

}
