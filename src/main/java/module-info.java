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

	uses se.uu.ub.cora.spider.extended2.ExtendedFunctionalityForCreateBeforeMetadataValidation;

	exports se.uu.ub.cora.spider.authentication;
	exports se.uu.ub.cora.spider.authorization;
	exports se.uu.ub.cora.spider.consistency;
	exports se.uu.ub.cora.spider.data;
	exports se.uu.ub.cora.spider.dependency;
	exports se.uu.ub.cora.spider.extended;
	exports se.uu.ub.cora.spider.record;
	exports se.uu.ub.cora.spider.role;
}