package com.aem.operations.core.services;

import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.Nullable;

public interface PackageHandlerService {

	
	/**
	 * @param resourceResolver
	 * @param inputPackage
	 * @return package Path
	 */
	public String uploadPack(ResourceResolver resourceResolver, @Nullable RequestParameter inputPackage);

	/**
	 * @param resourceResolver
	 * @param groupName
	 * @param packageName
	 * @param version
	 * @return
	 */
	public String buildPackage(ResourceResolver resourceResolver, final String groupName, final String packageName,
			final String version);

	/**
	 * @param resourceResolver
	 * @param groupName
	 * @param packageName
	 * @param version
	 * @param importMode
	 * @param aclHandling
	 * @return package Path
	 */
	public String installPackage(ResourceResolver resourceResolver, final String groupName, final String packageName,
			final String version, final ImportMode importMode, final AccessControlHandling aclHandling);

	/**
	 * @param resourceResolver
	 * @param inputPackage
	 * @return package Path
	 */
	public String uploadAndInstallPack(ResourceResolver resourceResolver, @Nullable RequestParameter inputPackage);

	/**
	 * @param resourceResolver
	 * @param groupName
	 * @param packageName
	 * @param version
	 * @return package Path
	 */
	public String deletePackage(ResourceResolver resourceResolver, final String groupName, final String packageName,
			final String version);
}
