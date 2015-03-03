package uk.ac.soton.simulation.jsit.core;

public class BasicStochasticAccessInfo extends AbstractStochasticAccessInfo {


    // ************************* Static Fields *****************************************
    
    private static final long serialVersionUID = 1L;

    
    // ************************** Constructors *****************************************

    /**
     * Exposed for technical reasons; not for JSIT user use.
     */
    public BasicStochasticAccessInfo(Class<?> owner, String id) {
        
        super(owner, id);
        
    }

    
    // *********************** Public Instance Methods *********************************
    
    @Override
    public void removeMe(AbstractStochasticItem stochItem) {
        
        // Nothing to do; we have no link back to the stochastic item (cf. accessors)
        
    }


}
