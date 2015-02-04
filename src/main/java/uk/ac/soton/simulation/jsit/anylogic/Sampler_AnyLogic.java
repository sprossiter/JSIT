/*  
    Copyright 2015 University of Southampton
    
    This file is part of JSIT.

    JSIT is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    JSIT is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with JSIT.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.ac.soton.simulation.jsit.anylogic;

import java.io.Serializable;

import com.anylogic.engine.Utilities;

import uk.ac.soton.simulation.jsit.core.Distribution;
import uk.ac.soton.simulation.jsit.core.Sampler;

/**
 * AnyLogic helper library concrete Sampler subclass.
 * 
 * @author Stuart Rossiter
 * @since 0.1
 */	
public class Sampler_AnyLogic extends Sampler implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Utilities alSampler;

    /*
     * Default constructor
     */
    public Sampler_AnyLogic(Utilities alSampler) {

        this.alSampler = alSampler;

    }

    @Override
    protected boolean distIsSupported(Distribution dist) {

        return true;			// Always true for current set of dists

    }

    /*
     * Standard mean, sd ordering
     */
    @Override
    protected double sampleNormal(double mean, double sd) {

        assert sd > 0.0;
        return alSampler.normal(sd, mean);

    }

    /*
     * Mean is 1/lambda; AnyLogic samples according to lambda
     */
    @Override
    protected double sampleExponential(double mean) {

        assert mean > 0.0;
        return alSampler.exponential(1.0d / mean);

    }

    /*
     * Continuous uniform
     */
    @Override
    protected double sampleUniform(double min, double max) {

        return alSampler.uniform(min, max);

    }

    /*
     * Discrete uniform. AnyLogic samples from [0, max]
     * whereas we want [1, max]
     */
    @Override
    protected int sampleUniformDiscrete(int max) {

        return alSampler.uniform_discr(max - 1) + 1;

    }

    /*
     * Bernoulli (probabilistic trial). To fit 1-K raw
     * scheme, the 0/1 returned by AnyLogic is translated
     * into 1/2
     */
    @Override
    protected int sampleBernoulli(double p) {

        int sample = alSampler.bernoulli(p);
        assert (sample == 0 || sample == 1);
        return sample + 1;

    }

}
