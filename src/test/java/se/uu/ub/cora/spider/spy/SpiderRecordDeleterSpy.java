package se.uu.ub.cora.spider.spy;

import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.record.SpiderRecordDeleter;
import se.uu.ub.cora.spider.record.storage.RecordNotFoundException;

import java.util.ArrayList;
import java.util.List;

public class SpiderRecordDeleterSpy implements SpiderRecordDeleter {

    public List<String> deletedTypes = new ArrayList<>();
    public List<String> deletedIds = new ArrayList<>();

    @Override
    public void deleteRecord(String authToken, String type, String id) {
        if(id.equals("someGeneratedIdDeleteNotAllowed")){
            throw new AuthorizationException("AuthorizationException from SpiderRecordDeleterSpy");
        } else if("nonExistingId".equals(id)){
            throw new RecordNotFoundException("RecordnotFoundException from SpiderRecordDeleterSpy");
        }
        deletedTypes.add(type);
        deletedIds.add(id);
    }
}
