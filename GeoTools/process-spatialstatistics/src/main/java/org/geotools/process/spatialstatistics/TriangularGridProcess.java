/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.process.spatialstatistics;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.hexagon.HexagonOrientation;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.geotools.process.ProcessFactory;
import org.geotools.process.spatialstatistics.core.Params;
import org.geotools.process.spatialstatistics.operations.TriangularGridOperation;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * Creates triangular grids from extent or bounds source features
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class TriangularGridProcess extends AbstractStatisticsProcess {
    protected static final Logger LOGGER = Logging.getLogger(TriangularGridProcess.class);

    private boolean started = false;

    public TriangularGridProcess(ProcessFactory factory) {
        super(factory);
    }

    public ProcessFactory getFactory() {
        return factory;
    }

    public static SimpleFeatureCollection process(ReferencedEnvelope extent,
            SimpleFeatureCollection boundsSource, Double size, HexagonOrientation orientation,
            ProgressListener monitor) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(TriangularGridProcessFactory.extent.key, extent);
        map.put(TriangularGridProcessFactory.boundsSource.key, boundsSource);
        map.put(TriangularGridProcessFactory.size.key, size);
        map.put(TriangularGridProcessFactory.orientation.key, orientation);

        Process process = new TriangularGridProcess(null);
        Map<String, Object> resultMap;
        try {
            resultMap = process.execute(map, monitor);
            return (SimpleFeatureCollection) resultMap.get(TriangularGridProcessFactory.RESULT.key);
        } catch (ProcessException e) {
            LOGGER.log(Level.FINER, e.getMessage(), e);
        }

        return null;
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener monitor)
            throws ProcessException {
        if (started)
            throw new IllegalStateException("Process can only be run once");
        started = true;

        try {
            ReferencedEnvelope gridBounds = (ReferencedEnvelope) Params.getValue(input,
                    TriangularGridProcessFactory.extent, null);
            SimpleFeatureCollection boundsSource = (SimpleFeatureCollection) Params.getValue(input,
                    TriangularGridProcessFactory.boundsSource, null);
            if (gridBounds == null) {
                throw new NullPointerException("extent parameters required");
            }

            Double size = (Double) Params.getValue(input, TriangularGridProcessFactory.size, null);
            if (size == null || size == 0) {
                throw new NullPointerException("sideLen parameter should be grater than 0");
            }
            HexagonOrientation orientation = (HexagonOrientation) Params.getValue(input,
                    TriangularGridProcessFactory.orientation,
                    TriangularGridProcessFactory.orientation.sample);

            // start process
            TriangularGridOperation operation = new TriangularGridOperation();
            operation.setBoundsSource(boundsSource);
            operation.setOrientation(orientation);
            SimpleFeatureCollection resultFc = operation.execute(gridBounds, size);
            // end process

            Map<String, Object> resultMap = new HashMap<String, Object>();
            resultMap.put(TriangularGridProcessFactory.RESULT.key, resultFc);
            return resultMap;
        } catch (Exception eek) {
            throw new ProcessException(eek);
        } finally {
            started = false;
        }
    }
}
