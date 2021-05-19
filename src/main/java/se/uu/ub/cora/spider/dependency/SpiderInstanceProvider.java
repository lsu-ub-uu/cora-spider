/*
 * Copyright 2015, 2018, 2019, 2021 Uppsala University Library
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

import java.util.Map;

import se.uu.ub.cora.spider.record.Downloader;
import se.uu.ub.cora.spider.record.IncomingLinksReader;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.spider.record.RecordListIndexer;
import se.uu.ub.cora.spider.record.RecordListReader;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.record.RecordSearcher;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.record.RecordValidator;
import se.uu.ub.cora.spider.record.Uploader;

public final class SpiderInstanceProvider {
	private static SpiderInstanceFactory factory;
	private static Map<String, String> initInfo;

	private SpiderInstanceProvider() {
		// not called
		throw new UnsupportedOperationException();
	}

	public static void setSpiderInstanceFactory(SpiderInstanceFactory factory) {
		SpiderInstanceProvider.factory = factory;
	}

	public static RecordReader getRecordReader() {
		return factory.factorRecordReader();
	}

	public static RecordListReader getRecordListReader() {
		return factory.factorRecordListReader();
	}

	public static RecordCreator getRecordCreator(String recordType) {
		return factory.factorRecordCreator();
	}

	public static RecordUpdater getRecordUpdater(String recordType) {
		return factory.factorRecordUpdater();
	}

	public static RecordDeleter getRecordDeleter() {
		return factory.factorRecordDeleter();
	}

	public static Uploader getUploader() {
		return factory.factorUploader();
	}

	public static Downloader getDownloader() {
		return factory.factorDownloader();
	}

	public static RecordSearcher getRecordSearcher() {
		return factory.factorRecordSearcher();
	}

	public static IncomingLinksReader getIncomingLinksReader() {
		return factory.factorIncomingLinksReader();
	}

	public static RecordValidator getRecordValidator() {
		return factory.factorRecordValidator();
	}

	public static RecordListIndexer getRecordListIndexer() {
		// TODO: not tested yet
		return factory.factorRecordListIndexer();
	}

	public static void setInitInfo(Map<String, String> initInfo) {
		SpiderInstanceProvider.initInfo = initInfo;
	}

	public static Map<String, String> getInitInfo() {
		return initInfo;
	}

	public static String getDependencyProviderClassName() {
		return factory.getDependencyProviderClassName();
	}

}
