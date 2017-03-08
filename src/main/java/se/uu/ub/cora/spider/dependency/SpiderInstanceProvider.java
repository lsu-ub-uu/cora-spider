/*
 * Copyright 2015 Uppsala University Library
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
import se.uu.ub.cora.spider.record.SpiderRecordListReader;
import se.uu.ub.cora.spider.record.SpiderRecordReader;
import se.uu.ub.cora.spider.record.SpiderRecordSearcher;
import se.uu.ub.cora.spider.record.SpiderRecordUpdater;
import se.uu.ub.cora.spider.record.SpiderUploader;

public final class SpiderInstanceProvider {
	private static SpiderInstanceFactory factory;

	private SpiderInstanceProvider() {
		// not called
		throw new UnsupportedOperationException();
	}

	public static void setSpiderInstanceFactory(SpiderInstanceFactory factory) {
		SpiderInstanceProvider.factory = factory;

	}

	public static SpiderRecordReader getSpiderRecordReader() {
		return factory.factorSpiderRecordReader();
	}

	public static SpiderRecordListReader getSpiderRecordListReader() {
		return factory.factorSpiderRecordListReader();
	}

	public static SpiderRecordCreator getSpiderRecordCreator() {
		return factory.factorSpiderRecordCreator();
	}

	public static SpiderRecordUpdater getSpiderRecordUpdater() {
		return factory.factorSpiderRecordUpdater();
	}

	public static SpiderRecordDeleter getSpiderRecordDeleter() {
		return factory.factorSpiderRecordDeleter();
	}

	public static SpiderUploader getSpiderUploader() {
		return factory.factorSpiderUploader();
	}

	public static SpiderDownloader getSpiderDownloader() {
		return factory.factorSpiderDownloader();
	}

	public static SpiderRecordSearcher getSpiderRecordSearcher() {
		return factory.factorSpiderRecordSearcher();
	}

}
