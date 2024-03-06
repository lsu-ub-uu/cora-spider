/*
 * Copyright 2016 Olov McKie
 * Copyright 2017, 2019, 2021, 2024 Uppsala University Library
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

import se.uu.ub.cora.spider.binary.Downloader;
import se.uu.ub.cora.spider.binary.Uploader;
import se.uu.ub.cora.spider.binary.iiif.IiifReader;
import se.uu.ub.cora.spider.record.IncomingLinksReader;
import se.uu.ub.cora.spider.record.RecordCreator;
import se.uu.ub.cora.spider.record.RecordDeleter;
import se.uu.ub.cora.spider.record.RecordListIndexer;
import se.uu.ub.cora.spider.record.RecordListReader;
import se.uu.ub.cora.spider.record.RecordReader;
import se.uu.ub.cora.spider.record.RecordSearcher;
import se.uu.ub.cora.spider.record.RecordUpdater;
import se.uu.ub.cora.spider.record.RecordValidator;

public interface SpiderInstanceFactory {

	String getDependencyProviderClassName();

	RecordReader factorRecordReader();

	IncomingLinksReader factorIncomingLinksReader();

	RecordListReader factorRecordListReader();

	RecordCreator factorRecordCreator();

	RecordUpdater factorRecordUpdater();

	RecordDeleter factorRecordDeleter();

	Uploader factorUploader();

	Downloader factorDownloader();

	RecordSearcher factorRecordSearcher();

	RecordValidator factorRecordValidator();

	RecordListIndexer factorRecordListIndexer();

	/**
	 * factorIiifReader should return a {@link IiifReader} implementation to use to access the iiif
	 * protocol
	 * 
	 * @return an {@link IiifReader}
	 */
	IiifReader factorIiifReader();
}