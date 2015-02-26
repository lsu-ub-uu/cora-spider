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

		// calculate permissionKey (leave for later)
		String accessType = "CREATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(
				accessType, recordType, readRecord);
		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			// throw an exception
		}
		return readRecord;
	}

	@Override
	public DataGroup createAndStoreRecord(String userId, String type,
			DataGroup record) {

		DataGroup recordInfo = new DataGroup("recordInfo");
		// id
		String id = idGenerator.getIdForType(type);
		DataAtomic idData = new DataAtomic("id", id);
		recordInfo.addChild(idData);

		// set more stuff, user, tscreated
		// set owning organisation

		// set recordInfo in record
		record.addChild(recordInfo);

		// validate

		// calculate permissionKey (leave for later)
		String accessType = "CREATE";
		Set<String> recordCalculateKeys = keyCalculator.calculateKeys(
				accessType, type, record);

		if (!authorization.isAuthorized(userId, recordCalculateKeys)) {
			// throw an exception
		}

		// send to storage
		recordStorageGateway.create(type, id, record);

		return record;
	}

}
