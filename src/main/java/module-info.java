import se.uu.ub.cora.spider.extended.apptoken.ApptokenExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extended.consistency.MetadataValidatorExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extended.workorder.WorkOrderExtendedFunctionalityFactory;
import se.uu.ub.cora.spider.extendedfunctionality.ExtendedFunctionalityFactory;

/**
 * The spider module sits in the center of a Cora based system, managing data flow to and from other
 * modules to bring the parts together as a working system.
 */
module se.uu.ub.cora.spider {
	requires transitive se.uu.ub.cora.beefeater;
	requires transitive se.uu.ub.cora.bookkeeper;
	requires transitive se.uu.ub.cora.storage;
	requires transitive se.uu.ub.cora.search;
	requires se.uu.ub.cora.logger;
	requires se.uu.ub.cora.data;
	requires se.uu.ub.cora.password;

	uses ExtendedFunctionalityFactory;

	exports se.uu.ub.cora.spider.authentication;
	exports se.uu.ub.cora.spider.authorization;
	exports se.uu.ub.cora.spider.data;
	exports se.uu.ub.cora.spider.dependency;
	exports se.uu.ub.cora.spider.extendedfunctionality;
	exports se.uu.ub.cora.spider.record;
	exports se.uu.ub.cora.spider.recordtype;
	exports se.uu.ub.cora.spider.recordtype.internal
			to se.uu.ub.cora.userstorage, se.uu.ub.cora.metadatastorage;

	provides ExtendedFunctionalityFactory with WorkOrderExtendedFunctionalityFactory,
			ApptokenExtendedFunctionalityFactory, MetadataValidatorExtendedFunctionalityFactory;
}