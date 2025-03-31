/*
 * Copyright 2024, 2025 Uppsala University Library
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
package se.uu.ub.cora.spider.systemsecret;

import java.util.Collections;

import se.uu.ub.cora.data.DataAtomic;
import se.uu.ub.cora.data.DataGroup;
import se.uu.ub.cora.data.DataParent;
import se.uu.ub.cora.data.DataProvider;
import se.uu.ub.cora.data.DataRecordGroup;
import se.uu.ub.cora.password.texthasher.TextHasher;
import se.uu.ub.cora.spider.cache.DataChangedSender;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.idgenerator.RecordIdGenerator;

public class SystemSecretOperationsImp implements SystemSecretOperations {

	private static final String SYSTEM_SECRET_TYPE = "systemSecret";
	private static final String SYSTEM_SECRET_VALIDATION_TYPE = "systemSecret";
	private static final String SYSTEM_SECRET_NAME_IN_DATA = "systemSecret";
	private static final String SECRET = "secret";

	public static SystemSecretOperationsImp usingDependencyProviderAndTextHasher(
			SpiderDependencyProvider dependencyProvider, TextHasher textHasher) {
		return new SystemSecretOperationsImp(dependencyProvider, textHasher);
	}

	private SpiderDependencyProvider dependencyProvider;
	private TextHasher textHasher;

	private SystemSecretOperationsImp(SpiderDependencyProvider dependencyProvider,
			TextHasher textHasher) {
		this.dependencyProvider = dependencyProvider;
		this.textHasher = textHasher;
	}

	@Override
	public String createAndStoreSystemSecretRecord(String secret, String dataDivider) {
		String systemSecretId = generateSystemSecretRecordId();
		DataRecordGroup systemSecretRecordGroup = createSystemSecretRecordGroupWithHashedSecret(
				secret, systemSecretId, dataDivider);
		storeSystemSecretIntoStorage(dataDivider, systemSecretId, systemSecretRecordGroup);
		sendDataChanged(systemSecretId, "create");
		return systemSecretId;
	}

	private String generateSystemSecretRecordId() {
		RecordIdGenerator recordIdGenerator = dependencyProvider.getRecordIdGenerator();
		return recordIdGenerator.getIdForType(SYSTEM_SECRET_TYPE);
	}

	private DataRecordGroup createSystemSecretRecordGroupWithHashedSecret(String secret,
			String systemSecretId, String dataDivider) {
		DataRecordGroup systemSecretRecordGroup = DataProvider
				.createRecordGroupUsingNameInData(SYSTEM_SECRET_NAME_IN_DATA);

		setRecordInfo(systemSecretRecordGroup, systemSecretId, dataDivider);
		hashSecretAndAddToGroup(systemSecretRecordGroup, secret);
		return systemSecretRecordGroup;
	}

	private void setRecordInfo(DataRecordGroup systemSecret, String systemSecretId,
			String dataDivider) {
		systemSecret.setType(SYSTEM_SECRET_TYPE);
		systemSecret.setId(systemSecretId);
		systemSecret.setValidationType(SYSTEM_SECRET_VALIDATION_TYPE);
		systemSecret.setDataDivider(dataDivider);
	}

	private void hashSecretAndAddToGroup(DataParent systemSecret, String secret) {
		String hashedSecret = textHasher.hashText(secret);
		DataAtomic hashedSecretAtomic = DataProvider.createAtomicUsingNameInDataAndValue(SECRET,
				hashedSecret);
		systemSecret.addChild(hashedSecretAtomic);
	}

	private void storeSystemSecretIntoStorage(String dataDivider, String systemSecretId,
			DataRecordGroup systemSecretRecordGroup) {
		DataGroup systemSecretGroup = DataProvider
				.createGroupFromRecordGroup(systemSecretRecordGroup);
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		recordStorage.create(SYSTEM_SECRET_TYPE, systemSecretId, systemSecretGroup,
				Collections.emptySet(), Collections.emptySet(), dataDivider);
	}

	private void sendDataChanged(String recordId, String action) {
		DataChangedSender dataChangedSender = dependencyProvider.getDataChangeSender();
		dataChangedSender.sendDataChanged(SYSTEM_SECRET_TYPE, recordId, action);
	}

	@Override
	public void updateSecretForASystemSecret(String systemSecretId, String dataDivider,
			String secret) {
		DataRecordGroup systemSecretRecordGroup = updateSecretForASystemSecret(systemSecretId,
				secret);
		updateSystemSecretRecordIntoStorage(dataDivider, systemSecretId, systemSecretRecordGroup);
		sendDataChanged(systemSecretId, "update");
	}

	private DataRecordGroup updateSecretForASystemSecret(String systemSecretId, String secret) {
		DataRecordGroup existingSystemSecret = readSystemSecretFromStorage(systemSecretId);
		existingSystemSecret.removeAllChildrenWithNameInData(SECRET);
		hashSecretAndAddToGroup(existingSystemSecret, secret);
		return existingSystemSecret;
	}

	private DataRecordGroup readSystemSecretFromStorage(String systemSecretId) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		return recordStorage.read(SYSTEM_SECRET_TYPE, systemSecretId);
	}

	private void updateSystemSecretRecordIntoStorage(String dataDivider, String systemSecretId,
			DataRecordGroup existingSystemSecret) {
		DataGroup existingSystemSecretTogGroup = DataProvider
				.createGroupFromRecordGroup(existingSystemSecret);
		updateSystemSecretIntoStorage(dataDivider, systemSecretId, existingSystemSecretTogGroup);
	}

	private void updateSystemSecretIntoStorage(String dataDivider, String systemSecretId,
			DataGroup existingSystemSecretTogGroup) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		recordStorage.update(SYSTEM_SECRET_TYPE, systemSecretId, existingSystemSecretTogGroup,
				Collections.emptySet(), Collections.emptySet(), dataDivider);
	}

	@Override
	public void deleteSystemSecretFromStorage(String systemSecretId) {
		RecordStorage recordStorage = dependencyProvider.getRecordStorage();
		recordStorage.deleteByTypeAndId(SYSTEM_SECRET_TYPE, systemSecretId);
		sendDataChanged(systemSecretId, "delete");
	}

	public SpiderDependencyProvider onlyForTestGetDependencyProvider() {
		return dependencyProvider;
	}

	public TextHasher onlyForTestGetTextHasher() {
		return textHasher;
	}
}
