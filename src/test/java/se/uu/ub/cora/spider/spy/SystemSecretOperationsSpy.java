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
package se.uu.ub.cora.spider.spy;

import se.uu.ub.cora.spider.systemsecret.SystemSecretOperations;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

public class SystemSecretOperationsSpy implements SystemSecretOperations {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();
	private int systemSecretNo = 0;

	public SystemSecretOperationsSpy() {
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("createAndStoreSystemSecretRecord",
				() -> generateSystemSecretId());
	}

	private String generateSystemSecretId() {
		systemSecretNo++;
		return "someSystemSecretRecordId" + systemSecretNo;
	}

	@Override
	public String createAndStoreSystemSecretRecord(String secret, String dataDivider) {
		return (String) MCR.addCallAndReturnFromMRV("secret", secret, "dataDivider", dataDivider);
	}

	@Override
	public void updateSecretForASystemSecret(String systemSecretId, String dataDivider,
			String secret) {
		MCR.addCall("systemSecretId", systemSecretId, "dataDivider", dataDivider, "secret", secret);
	}

	@Override
	public void deleteSystemSecretFromStorage(String systemSecretId) {
		MCR.addCall("systemSecretId", systemSecretId);
	}
}
