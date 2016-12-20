/*
 * Copyright 2016 Olov McKie
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

import se.uu.ub.cora.spider.record.SpiderDownloader;
import se.uu.ub.cora.spider.record.SpiderRecordCreator;
import se.uu.ub.cora.spider.record.SpiderRecordDeleter;
import se.uu.ub.cora.spider.record.SpiderRecordListReader;
import se.uu.ub.cora.spider.record.SpiderRecordReader;
import se.uu.ub.cora.spider.record.SpiderRecordUpdater;
import se.uu.ub.cora.spider.record.SpiderRecordUpdaterSpy;
import se.uu.ub.cora.spider.record.SpiderUploader;

public class SpiderInstanceFactorySpy2 implements SpiderInstanceFactory {
	public boolean readerFactoryWasCalled = false;
	public boolean listReaderFactoryWasCalled = false;
	public boolean creatorFactoryWasCalled = false;
	public boolean updaterFactoryWasCalled = false;
	public boolean deleterFactoryWasCalled = false;
	public boolean uploaderFactoryWasCalled = false;
	public boolean downloaderFactoryWasCalled = false;

	public List<SpiderRecordUpdaterSpy> createdUpdaters = new ArrayList<>();

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
	public SpiderRecordCreator factorSpiderRecordCreator() {
		creatorFactoryWasCalled = true;
		return null;
	}

	@Override
	public SpiderRecordUpdater factorSpiderRecordUpdater() {
		updaterFactoryWasCalled = true;

		SpiderRecordUpdaterSpy spiderRecordUpdaterSpy = new SpiderRecordUpdaterSpy();
		createdUpdaters.add(spiderRecordUpdaterSpy);
		return spiderRecordUpdaterSpy;
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

}
