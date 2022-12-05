package com.aem.operations.core.services.impl;

import com.adobe.acs.commons.genericlists.GenericList;
import com.adobe.acs.commons.genericlists.GenericList.Item;
import com.aem.operations.core.services.PackageHandlerService;
import com.aem.operations.core.utils.VltUtils;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ImportMode;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.packaging.*;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Component(service = PackageHandlerService.class, immediate = true, name = "Package Handler Service")
public class PackageHandlerServiceImpl implements PackageHandlerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PackageHandlerServiceImpl.class);

	@Reference
	private Packaging packaging;

	@Override
	public String uploadPack(ResourceResolver resourceResolver, @Nullable RequestParameter inputPackage) {
		if (null != inputPackage) {
			try (InputStream inputPack = inputPackage.getInputStream()) {
				if (null != inputPack) {
					final JcrPackageManager packageManager = packaging
							.getPackageManager(resourceResolver.adaptTo(Session.class));
					JcrPackage jcrPackage = packageManager.upload(inputPack, true);
					return jcrPackage.getNode().getPath();
				}
			} catch (IOException | RepositoryException e) {
				LOGGER.error("Could not upload package {}", e.getMessage());
				return StringUtils.EMPTY;
			}
		}
		return StringUtils.EMPTY;
	}

	@Override
	public String buildPackage(ResourceResolver resourceResolver, String groupName, String packageName,
							   String version) {
		if (StringUtils.isNotEmpty(packageName) && StringUtils.isNotEmpty(groupName)) {
			final JcrPackageManager packageManager = packaging
					.getPackageManager(resourceResolver.adaptTo(Session.class));
			final PackageId packageId = new PackageId(groupName, packageName, version);
			try (JcrPackage jcrPackage = packageManager.open(packageId)) {
				if(null != jcrPackage) {
					packageManager.assemble(jcrPackage, null);
					return jcrPackage.getNode().getPath();
				}
			} catch (RepositoryException | PackageException | IOException e) {
				LOGGER.error("Could not build package {}", e.getMessage());
				return StringUtils.EMPTY;
			}
		}
		return StringUtils.EMPTY;
	}

	@Override
	public String installPackage(ResourceResolver resourceResolver, String groupName, String packageName,
								 String version, ImportMode importMode, AccessControlHandling aclHandling) {
		if (StringUtils.isNotEmpty(packageName) && StringUtils.isNotEmpty(groupName)) {
			final JcrPackageManager packageManager = packaging
					.getPackageManager(resourceResolver.adaptTo(Session.class));
			final PackageId packageId = new PackageId(groupName, packageName, version);
			try (JcrPackage jcrPackage = packageManager.open(packageId)) {
				if(null != jcrPackage) {
					final ImportOptions opts = VltUtils.getImportOptions(aclHandling, importMode);
					jcrPackage.install(opts);
					return jcrPackage.getNode().getPath();
				}
			} catch (RepositoryException | PackageException | IOException e) {
				LOGGER.error("Could not install package {}", e.getMessage());
				return StringUtils.EMPTY;
			}
		}
		return StringUtils.EMPTY;
	}

	@Override
	public String uploadAndInstallPack(ResourceResolver resourceResolver, @Nullable RequestParameter inputPackage) {
		if (null != inputPackage) {
			try (InputStream inputPack = inputPackage.getInputStream()) {
				if (null != inputPack) {
					final JcrPackageManager packageManager = packaging
							.getPackageManager(resourceResolver.adaptTo(Session.class));
					try (JcrPackage jcrPackage = packageManager.upload(inputPack, true)) {
						installPackage(jcrPackage, ImportMode.REPLACE, AccessControlHandling.IGNORE);
						return jcrPackage.getNode().getPath();
					} catch (RepositoryException e) {
						LOGGER.error("Could not Upload and Install package due to Repository Exception {}", e.getMessage());
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				LOGGER.error("Could not Upload and Install package due to IO Exception {}", e.getMessage());
				return StringUtils.EMPTY;
			}
		}
		return StringUtils.EMPTY;
	}

	public String installPackage(JcrPackage jcrPackage, final ImportMode importMode,
								 final AccessControlHandling aclHandling) {
		try {
			final ImportOptions opts = VltUtils.getImportOptions(aclHandling, importMode);
			jcrPackage.install(opts);
			return jcrPackage.getNode().getPath();
		} catch (RepositoryException | PackageException | IOException e) {
			LOGGER.error("Could not install built package {}", e.getMessage());
			return StringUtils.EMPTY;
		}
	}

	@Override
	public String deletePackage(ResourceResolver resourceResolver, String groupName, String packageName,
								String version) {
		if (StringUtils.isNotEmpty(packageName) && StringUtils.isNotEmpty(groupName)) {
			final JcrPackageManager packageManager = packaging
					.getPackageManager(resourceResolver.adaptTo(Session.class));
			final PackageId packageId = new PackageId(groupName, packageName, version);
			try (JcrPackage jcrPackage = packageManager.open(packageId)) {
				if(null != jcrPackage) {
					String path = jcrPackage.getNode().getPath();
					packageManager.remove(jcrPackage);
					return path;
				}
			} catch (RepositoryException e) {
				LOGGER.error("Could not delete package {}", e.getMessage());
				return StringUtils.EMPTY;
			}
		}
		return StringUtils.EMPTY;
	}

	@Override
	public JsonArray listPackages(ResourceResolver resourceResolver) {
		JsonArray jsonResponse = new JsonArray();
		Set<String> packGroups = prepareGenericList(resourceResolver);
		Set<JcrPackage> packageSet =  new TreeSet<>();
		if(!packGroups.isEmpty()) {
			final JcrPackageManager packageManager = packaging.getPackageManager(resourceResolver.adaptTo(Session.class));
			packGroups.forEach(r -> {
				try {
					packageSet.addAll(packageManager.listPackages(r, false));
				} catch (RepositoryException e) {
					LOGGER.error("Error occured while listing the packages {}", e.getMessage());
				}
			});
			if(!packageSet.isEmpty()) {
				packageSet.forEach(pack -> {
					JsonObject jsonObj = new JsonObject();
					try {
						jsonObj.addProperty("packageName", pack.getDefinition().getId().getName());
						jsonObj.addProperty("packageGroup", pack.getDefinition().getId().getGroup());
						jsonObj.addProperty("packageVersion", pack.getDefinition().getId().getVersionString());
						jsonObj.addProperty("packagePath", pack.getNode().getPath());
						jsonResponse.add(jsonObj);
					} catch (RepositoryException e) {
						LOGGER.error("Error occured while reading the package {}", e.getMessage());
					}
				});
			}
		}
		return jsonResponse;
	}

	private Set<String> prepareGenericList(ResourceResolver resourceResolver) {
		PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
		Page listPage = pageManager.getPage("/etc/acs-commons/lists/package-settings-list/package-settings");
		GenericList genericList = listPage.adaptTo(GenericList.class);
		return genericList.getItems().stream().map(Item::getValue).collect(Collectors.toSet());
	}
}