/*
 * Copyright 2021 Uppsala University Library
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
package se.uu.ub.cora.spider.index.internal;

/**
 * BatchJobStorer is responsible for storing IndexBatchJob. <br>
 */
public interface BatchJobStorer {

	/**
	 * store method is responsible to store a IndexBatchJob on the specified storage on
	 * depependencyProvider.
	 * 
	 * @param indexBatchJob
	 *            An IndexBatchJob containing information about what to index, including RecordType,
	 *            the number of records to index, and Filter
	 */
	void store(IndexBatchJob indexBatchJob);

	void create(IndexBatchJob indexBatchJob);
}
