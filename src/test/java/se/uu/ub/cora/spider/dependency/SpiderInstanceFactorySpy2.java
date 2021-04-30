/*
 * Copyright 2016 Olov McKie
 * Copyright 2019 Uppsala University Library
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

package se.uu.ub.cora.spider.dependency;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.spider.record.Downloader;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.spider.record.IncomingLinksReader;
import se.uu.ub.cora.spider.record.RecordListReader;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.record.RecordSearcher;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.record.SpiderRecordUpdaterSpy;
import se.uu.ub.cora.spider.record.RecordValidator;
import se.uu.ub.cora.spider.record.Uploader;

public class SpiderInstanceFactorySpy2 implements SpiderInstanceFactory {
	public boolean readerFactoryWasCalled = false;
	public boolean incomingLinksReaderFactoryWasCalled = false;
	public boolean listReaderFactoryWasCalled = false;
	public boolean creatorFactoryWasCalled = false;
	public boolean updaterFactoryWasCalled = false;
	public boolean deleterFactoryWasCalled = false;
	public boolean uploaderFactoryWasCalled = false;
	public boolean downloaderFactoryWasCalled = false;
	public boolean searcherFactoryWasCalled = false;

	public List<SpiderRecordUpdaterSpy> createdUpdaters = new ArrayList<>();
	public String recordType;

	@Override
	public RecordReader factorRecordReader() {
		readerFactoryWasCalled = true;
		return null;
	}

	@Override
	public RecordListReader factorRecordListReader() {
		listReaderFactoryWasCalled = true;
		return null;
	}

	@Override
	public RecordCreator factorRecordCreator(String recordType) {
		creatorFactoryWasCalled = true;
		return null;
	}

	@Override
	public RecordUpdater factorRecordUpdater(String recordType) {
		this.recordType = recordType;
		updaterFactoryWasCalled = true;

		SpiderRecordUpdaterSpy spiderRecordUpdaterSpy = new SpiderRecordUpdaterSpy();
		createdUpdaters.add(spiderRecordUpdaterSpy);
		return spiderRecordUpdaterSpy;
	}

	@Override
	public RecordDeleter factorRecordDeleter() {
		deleterFactoryWasCalled = true;
		return null;
	}

	@Override
	public Uploader factorUploader() {
		uploaderFactoryWasCalled = true;
		return null;
	}

	@Override
	public Downloader factorDownloader() {
		downloaderFactoryWasCalled = true;
		return null;
	}

	@Override
	public RecordSearcher factorRecordSearcher() {
		searcherFactoryWasCalled = true;
		return null;
	}

	@Override
	public IncomingLinksReader factorIncomingLinksReader() {
		incomingLinksReaderFactoryWasCalled = true;
		return null;
	}

	@Override
	public RecordValidator factorRecordValidator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDependencyProviderClassName() {
		// TODO Auto-generated method stub
		return null;
	}
}
