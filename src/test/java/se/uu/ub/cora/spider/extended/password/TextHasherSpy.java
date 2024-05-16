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
package se.uu.ub.cora.spider.extended.password;

import java.util.function.Supplier;

import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class TextHasherSpy implements TextHasher {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public TextHasherSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("hashText", ()->"someHashedText");
		MRV.setDefaultReturnValuesSupplier("matches", () -> false);
	}

	@Override
	public String hashText(String plainText) {
		return (String) MCR.addCallAndReturnFromMRV("plainText", plainText);
	}

	@Override
	public boolean matches(String plainText, String hashedText) {
		return (boolean) MCR.addCallAndReturnFromMRV("plainText", plainText, "hashedText",
				hashedText);
	}

}
