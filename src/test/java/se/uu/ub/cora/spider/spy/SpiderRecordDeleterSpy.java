/*
 * Copyright 2018 Uppsala University Library
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

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.spider.authorization.AuthorizationException;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.storage.RecordNotFoundException;

public class SpiderRecordDeleterSpy implements RecordDeleter {

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
