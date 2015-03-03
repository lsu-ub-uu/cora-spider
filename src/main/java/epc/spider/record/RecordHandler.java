package epc.spider.record;

import java.util.Set;

import epc.beefeater.AuthorizationInputBoundary;
import epc.metadataformat.data.DataAtomic;
import epc.metadataformat.data.DataGroup;
import epc.spider.record.storage.RecordIdGenerator;
import epc.spider.record.storage.RecordStorageGateway;

public class RecordHandler implements RecordInputBoundary {

	private RecordStorageGateway recordStorageGateway;
	private AuthorizationInputBoundary authorization;
	private RecordIdGenerator idGenerator;
	private PermissionKeyCalculator keyCalculator;

	public RecordHandler(AuthorizationInputBoundary authorization,
			RecordStorageGateway recordStorageGateway,
			RecordIdGenerator idGenerator, PermissionKeyCalculator keyCalculator) {
		this.authorization = authorization;
		this.recordStorageGateway = recordStorageGateway;
		this.idGenerator = idGenerator;
		this.keyCalculator = keyCalculator;

	}

	@Override
	public DataGroup readRecord(String userId, String recordType,
			String recordId) {
		DataGroup readRecord = recordStorageGateway.read(recordType, recordId);

		// calculate permissionKey
		String accessType = "READ";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(
				accessType, recordType, readRecord);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException("User:" + userId
					+ " is not authorized to read record:" + recordId
					+ " of type:" + recordType);
		}
		return readRecord;
	}

	@Override
	public DataGroup createAndStoreRecord(String userId, String recordType,
			DataGroup record) {

		DataGroup recordInfo = new DataGroup("recordInfo");
		// id
		String id = idGenerator.getIdForType(recordType);
		DataAtomic idData = new DataAtomic("id", id);
		recordInfo.addChild(idData);

		// set more stuff, user, tscreated
		// set owning organisation

		// set recordInfo in record
		record.addChild(recordInfo);

		// validate

		// calculate permissionKey
		String accessType = "CREATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(
				accessType, recordType, record);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			throw new AuthorizationException("User:" + userId
					+ " is not authorized to create a record  of type:"
					+ recordType);
		}

		// send to storage
		recordStorageGateway.create(recordType, id, record);

		return record;
	}

}
