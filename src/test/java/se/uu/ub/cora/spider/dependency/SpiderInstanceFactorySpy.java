/*
 * Copyright 2016 Olov McKie
 * Copyright 2017, 2019, 2021 Uppsala University Library
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

import se.uu.ub.cora.spider.record.SpiderDownloader;
import se.uu.ub.cora.spider.record.SpiderRecordCreator;
import se.uu.ub.cora.spider.record.SpiderRecordDeleter;
import se.uu.ub.cora.spider.record.SpiderRecordIncomingLinksReader;
import se.uu.ub.cora.spider.record.SpiderRecordListReader;
import se.uu.ub.cora.spider.record.SpiderRecordReader;
import se.uu.ub.cora.spider.record.SpiderRecordSearcher;
import se.uu.ub.cora.spider.record.SpiderRecordUpdater;
import se.uu.ub.cora.spider.record.SpiderRecordValidator;
import se.uu.ub.cora.spider.record.SpiderUploader;

public class SpiderInstanceFactorySpy implements SpiderInstanceFactory {
	public boolean readerFactoryWasCalled = false;
	public boolean incomingLinksReaderFactoryWasCalled = false;
	public boolean listReaderFactoryWasCalled = false;
	public boolean creatorFactoryWasCalled = false;
	public boolean updaterFactoryWasCalled = false;
	public boolean deleterFactoryWasCalled = false;
	public boolean uploaderFactoryWasCalled = false;
	public boolean downloaderFactoryWasCalled = false;
	public boolean searcherFactoryWasCalled = false;
	public boolean validatorFactoryWasCalled = false;
	public String recordType;

	@Override
	public SpiderRecordReader factorSpiderRecordReader() {
		readerFactoryWasCalled = true;
		return null;
	}

	@Override
	public SpiderRecordListReader factorSpiderRecordListReader() {
		listReaderFactoryWasCalled = true;
		return null;
	}

	@Override
	public SpiderRecordCreator factorSpiderRecordCreator(String recordType) {
		creatorFactoryWasCalled = true;
		this.recordType = recordType;
		return null;
	}

	@Override
	public SpiderRecordUpdater factorSpiderRecordUpdater(String recordType) {
		updaterFactoryWasCalled = true;
		this.recordType = recordType;
		return null;
	}

	@Override
	public SpiderRecordDeleter factorSpiderRecordDeleter() {
		deleterFactoryWasCalled = true;
		return null;
	}

	@Override
	public SpiderUploader factorSpiderUploader() {
		uploaderFactoryWasCalled = true;
		return null;
	}

	@Override
	public SpiderDownloader factorSpiderDownloader() {
		downloaderFactoryWasCalled = true;
		return null;
	}

	@Override
	public SpiderRecordSearcher factorSpiderRecordSearcher() {
		searcherFactoryWasCalled = true;
		return null;
	}

	@Override
	public SpiderRecordIncomingLinksReader factorSpiderRecordIncomingLinksReader() {
		incomingLinksReaderFactoryWasCalled = true;
		return null;
	}

	@Override
	public SpiderRecordValidator factorSpiderRecordValidator() {
		validatorFactoryWasCalled = true;
		return null;
	}

	@Override
	public String getDependencyProviderClassName() {
		return "someDependencyProviderClassNameFromSpy";
	}

}
