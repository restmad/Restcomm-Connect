package org.mobicents.servlet.restcomm.rvd.packaging;


import java.io.File;
import java.io.InputStream;

import org.mobicents.servlet.restcomm.rvd.exceptions.ProjectDoesNotExist;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.packaging.model.Rapp;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public interface PackagingStorage {
    Rapp loadRapp(String projectName) throws StorageException;
    Rapp loadRapp(File rapp) throws StorageException;
    void storeRappBinary(String projectName, File packageFile) throws RvdException;
    InputStream getRappBinary(String projectName) throws RvdException;
    boolean hasPackaging(String projectName) throws ProjectDoesNotExist;
    boolean binaryAvailable(String projectName);
    void storeRapp(Rapp rapp, String projectName) throws StorageException;
}
