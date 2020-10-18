package se.uu.ub.cora.spider.extended2;

import java.util.List;

import se.uu.ub.cora.spider.extended.ExtendedFunctionality;

/**
 * ExtendedFunctionalityFactory is used to factor {@link ExtendedFunctionality}, to be used in
 * various places in Spider for different recordTypes.<br>
 */
public interface ExtendedFunctionalityFactory {

	/**
	 * Factor is used by spider to get an instans of ExtendedFunctionality to use in predetermined
	 * places in Spider. Factor should be implemented so that it creates an instance of
	 * {@link ExtendedFunctionality}
	 * 
	 * @return An instance of extended funtionality
	 */
	ExtendedFunctionality factor();

	/**
	 * getExtendedFunctionalityContexts should be implemented so that it returns a List with
	 * {@link ExtendedFunctionalityContext} for wich circumstances this factory produces
	 * ExtendedFunctionality instances.
	 * 
	 * @return A List of ExtendedFunctionalityContext to determin under what cirumstances, the
	 *         extendedFunctionalities that this factory produces, are intended to be called
	 */
	List<ExtendedFunctionalityContext> getExtendedFunctionalityContexts();
}