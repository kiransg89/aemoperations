package com.aem.operations.core.utils;

import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;

/**
 * Utility class for creating vlt filters and import/export options
 */
public class VltUtils {

    public static ImportOptions getImportOptions(AccessControlHandling aclHandling, ImportMode importMode) {
        ImportOptions opts = new ImportOptions();
        if (aclHandling != null) {
            opts.setAccessControlHandling(aclHandling);
        } else {
            // default to overwrite
            opts.setAccessControlHandling(AccessControlHandling.OVERWRITE);
        }
        if (importMode != null) {
            opts.setImportMode(importMode);
        } else {
            // default to update
            opts.setImportMode(ImportMode.UPDATE);
        }

        return opts;
    }
}