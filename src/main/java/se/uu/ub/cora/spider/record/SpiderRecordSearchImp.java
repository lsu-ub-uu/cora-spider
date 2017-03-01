/*
 * Copyright 2017 Uppsala University Library
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
package se.uu.ub.cora.spider.record;

import se.uu.ub.cora.beefeater.authentication.User;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.data.SpiderDataGroup;
import se.uu.ub.cora.spider.data.SpiderDataList;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProvider;
import se.uu.ub.cora.spider.dependency.SpiderDependencyProviderSpy;

public class SpiderRecordSearchImp implements SpiderRecordSearch {
	private Authenticator authenticator;
	private SpiderAuthorizator spiderAuthorizator;
	private SpiderDataList readRecordList;
	private String authToken;
	private User user;
	private DataGroupToRecordEnhancer dataGroupToRecordEnhancer;

	private SpiderRecordSearchImp(SpiderDependencyProvider dependencyProvider,
			DataGroupToRecordEnhancer dataGroupToRecordEnhancer) {
		this.dataGroupToRecordEnhancer = dataGroupToRecordEnhancer;
		this.authenticator = dependencyProvider.getAuthenticator();
		this.spiderAuthorizator = dependencyProvider.getSpiderAuthorizator();
	}

	public static SpiderRecordSearch usingDependencyProviderAndDataGroupToRecordEnhancer(
			SpiderDependencyProviderSpy dependencyProvider,
			DataGroupToRecordEnhancerSpy dataGroupToRecordEnhancer) {
		return new SpiderRecordSearchImp(dependencyProvider, dataGroupToRecordEnhancer);
	}

	@Override
	public SpiderDataList search(String authToken, String searchId, SpiderDataGroup searchData) {
		this.authToken = authToken;
		tryToGetActiveUser();
		// TODO: read search by searchId from storage, and check that we have
		// READ access on all
		// recordTypeToSearchIn from stored search

		// TODO: validate incoming search data against metadataId stored in
		// search

		// TODO: search

		// TODO: check read access and enhance records

		// TODO: return result

		return SpiderDataList.withContainDataOfType("mix");
	}

	private void tryToGetActiveUser() {
		user = authenticator.getUserForToken(authToken);
	}
}
