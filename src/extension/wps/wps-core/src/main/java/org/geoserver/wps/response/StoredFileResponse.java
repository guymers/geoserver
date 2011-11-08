/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.response;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wps.GetExecutionResultType;
import org.geoserver.wps.GetExecutionStatusType;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.WPSStorageCleaner;

/**
 * Stored file response (for the status and resource operations)
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class StoredFileResponse extends Response {
    WPSStorageCleaner cleaner;

    public StoredFileResponse(WPSStorageCleaner cleaner) {
        super(File.class);
        this.cleaner = cleaner;
    }

    @Override
    public boolean canHandle(Operation operation) {
        String operationId = operation.getId();
        return ("GetExecutionStatus".equalsIgnoreCase(operationId) || "GetExecutionResult"
                .equalsIgnoreCase(operationId))
                && operation.getService().getId().equals("wps");
    }

    public String getMimeType(Object value, Operation operation) {
        Object request = operation.getParameters()[0];
        if(request instanceof GetExecutionStatusType) {
            return "text/xml";
        } else if(request instanceof GetExecutionResultType) {
            GetExecutionResultType ger = (GetExecutionResultType) request;
            if(ger.getMimeType() != null) {
                return ger.getMimeType();
            } else {
                // generic binary output...
                return "application/octet-stream";
            }
        } else {
            throw new WPSException("Trying to get a mime type for a unknown operation, " +
            		"we should not have got here in the first place");
        }
    }
    
    @Override
    public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
        String filename = getAttachmentFileName(value, operation);
        return (String[][]) new String[][] { { "Content-Disposition",
             "inline; filename=" + filename  } };
    }
    
    public String getAttachmentFileName(Object value, Operation operation) {
        Object request = operation.getParameters()[0];
        if(request instanceof GetExecutionStatusType) {
            return "result.xml";
        } else if(request instanceof GetExecutionResultType) {
            GetExecutionResultType ger = (GetExecutionResultType) request;
            if(ger.getOutputId() != null) {
                return ger.getOutputId();
            } else {
                // we should really never get here, the request should fail before
                return "result.dat";
            }
        } else {
            throw new WPSException("Trying to get a file name for a unknown operation, " +
                    "we should not have got here in the first place");
        }
    }
    
    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        File file = (File) value;
        FileInputStream fos = null;
        try {
            cleaner.lock(file);
            fos = new FileInputStream(file);
            IOUtils.copy(fos, output);
        } finally {
            cleaner.unlock(file);
            IOUtils.closeQuietly(fos);
        }

    }
}