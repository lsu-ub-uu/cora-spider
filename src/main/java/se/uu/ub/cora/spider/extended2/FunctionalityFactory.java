package se.uu.ub.cora.spider.extended2;

import se.uu.ub.cora.spider.extended.ExtendedFunctionality;

/**
 * FunctionalityFactory defines the standard methods used for creating extended functionality for
 * use in various places in Spider.
 *
 */
public interface FunctionalityFactory {

	/**
	 * Factor is used by spider to get an instans of ExtendedFunctionality to use in predetermined
	 * places in Spider.
	 * 
	 * @return An instance of extended funtionality
	 */
	ExtendedFunctionality factor();

}