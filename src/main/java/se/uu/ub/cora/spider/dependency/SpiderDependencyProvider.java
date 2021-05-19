package se.uu.ub.cora.spider.dependency;

import se.uu.ub.cora.bookkeeper.linkcollector.DataRecordLinkCollector;
import se.uu.ub.cora.bookkeeper.recordpart.DataRedactor;
import se.uu.ub.cora.bookkeeper.termcollector.DataGroupTermCollector;
import se.uu.ub.cora.bookkeeper.validator.DataValidator;
import se.uu.ub.cora.search.RecordIndexer;
import se.uu.ub.cora.search.RecordSearch;
import se.uu.ub.cora.spider.authentication.Authenticator;
import se.uu.ub.cora.spider.authorization.PermissionRuleCalculator;
import se.uu.ub.cora.spider.authorization.SpiderAuthorizator;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityProvider;
import se.uu.ub.cora.spider.record.DataGroupToRecordEnhancer;
import se.uu.ub.cora.spider.recordtype.RecordTypeHandler;
import se.uu.ub.cora.storage.RecordIdGenerator;
import se.uu.ub.cora.storage.RecordStorage;
import se.uu.ub.cora.storage.StreamStorage;

public interface SpiderDependencyProvider {

	RecordStorage getRecordStorage();

	StreamStorage getStreamStorage();

	RecordIdGenerator getRecordIdGenerator();

	SpiderAuthorizator getSpiderAuthorizator();

	DataValidator getDataValidator();

	DataRecordLinkCollector getDataRecordLinkCollector();

	DataGroupTermCollector getDataGroupTermCollector();

	PermissionRuleCalculator getPermissionRuleCalculator();

	DataRedactor getDataRedactor();

	ExtendedFunctionalityProvider getExtendedFunctionalityProvider();

	Authenticator getAuthenticator();

	RecordSearch getRecordSearch();

	RecordIndexer getRecordIndexer();

	RecordTypeHandler getRecordTypeHandler(String recordTypeId);

	DataGroupToRecordEnhancer getDataGroupToRecordEnhancer();

}