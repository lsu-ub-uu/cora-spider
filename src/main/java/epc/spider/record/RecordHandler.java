package epc.spider.record;

import epc.beefeater.AuthorizationInputBoundary;
import epc.metadataformat.data.DataGroup;
import epc.spider.record.storage.RecordStorageGateway;

public class RecordHandler implements RecordInputBoundary {

	private RecordStorageGateway recordStorageGateway;
	private AuthorizationInputBoundary authorization;

	public RecordHandler(AuthorizationInputBoundary authorization,
			RecordStorageGateway recordStorageGateway) {
		this.authorization = authorization;
		this.recordStorageGateway = recordStorageGateway;
	}

	@Override
	public DataGroup readRecord(String userId, String recordType, String recordId) {
		DataGroup readData = recordStorageGateway.read(recordType, recordId);
		
		//calculate permissionKey (leave for later)
		if(!authorization.isAuthorized(userId, "A GOOD KEY")){
			//throw an exception
		}
		return readData;
	}

}
