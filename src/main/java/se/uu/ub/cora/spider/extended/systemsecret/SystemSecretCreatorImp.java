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

package se.uu.ub.cora.spider.extended.systemsecret;

import java.util.Collections;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataChild;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordLink;
import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;

public class SystemSecretCreatorImp implements SystemSecretCreator {

	private static final String SYSTEM_SECRET_TYPE = "systemSecret";
	private static final String RECORD_INFO = "recordInfo";
	private static final String SECRET = "secret";
	private SpiderDependencyProvider dependencyProvider;
	private TextHasher textHasher;

	public SystemSecretCreatorImp(SpiderDependencyProvider dependencyProvider,
			TextHasher textHasher) {
		this.dependencyProvider = dependencyProvider;
		this.textHasher = textHasher;
	}

	@Override
	public String createAndStoreSystemSecretRecord(String secret, String dataDivider) {
		String hashedSecret = textHasher.hashText(secret);
		String systemSecretId = generateSystemSecretRecordId();
		DataGroup systemSecret = createSystemSecretGroupWithRecordInfoAndHashedSecret(hashedSecret,
				systemSecretId);

		storeSystemSecretIntoStorage(dataDivider, systemSecretId, systemSecret);
		return systemSecretId;
	}

	private String generateSystemSecretRecordId() {
		RecordIdGenerator recordIdGenerator = dependencyProvider.getRecordIdGenerator();
		return recordIdGenerator.getIdForType(SYSTEM_SECRET_TYPE);
	}

	private void createAndAddRecordInfoForSystemSecret(DataGroup systemSecret,
			String systemSecretId) {
		DataGroup recordInfo = DataProvider.createGroupUsingNameInData(RECORD_INFO);
		systemSecret.addChild(recordInfo);

		DataRecordLink typeLink = DataProvider.createRecordLinkUsingNameInDataAndTypeAndId("type",
				"recordType", SYSTEM_SECRET_TYPE);
		recordInfo.addChild(typeLink);

		DataChild atomicId = DataProvider.createAtomicUsingNameInDataAndValue("id", systemSecretId);
		recordInfo.addChild(atomicId);
	}

	private DataGroup createSystemSecretGroupWithRecordInfoAndHashedSecret(String hashedSecret,
			String systemSecretId) {
		DataGroup systemSecret = DataProvider.createGroupUsingNameInData(SYSTEM_SECRET_TYPE);

		createAndAddRecordInfoForSystemSecret(systemSecret, systemSecretId);
		addHashedSecretToGroup(hashedSecret, systemSecret);
		return systemSecret;
	}

	private void addHashedSecretToGroup(String hashedSecret, DataGroup systemSecret) {
		DataAtomic hashedSecretAtomic = DataProvider.createAtomicUsingNameInDataAndValue(SECRET,
				hashedSecret);
		systemSecret.addChild(hashedSecretAtomic);
	}

	private void storeSystemSecretIntoStorage(String dataDivider, String systemSecretId,
			DataGroup systemSecret) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		recordStorage.create(SYSTEM_SECRET_TYPE, systemSecretId, systemSecret,
				Collections.emptySet(), Collections.emptySet(), dataDivider);
	}

}
