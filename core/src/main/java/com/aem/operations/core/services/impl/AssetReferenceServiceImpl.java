package com.aem.operations.core.services.impl;

import com.aem.operations.core.services.AssetReferenceService;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.AssetReferenceSearch;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.*;
import org.apache.sling.commons.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(service = AssetReferenceService.class, immediate = true, name = "Asset Reference Service")
public class AssetReferenceServiceImpl implements AssetReferenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AssetReferenceServiceImpl.class);
    public static final String ADMINISTRATIVE_SERVICE_USER = "aarp-administrative-service-user";

    public static final String JOB_STATUS = "jobStatus";
    public static final String STARTING_STATUS = "starting";
    public static final String RUNNING_STATUS = "running";
    public static final String UNSCHEDULED_STATUS = "running";
    public static final String COMPLETED_STATUS = "completed";
    private static final String DAM_ROOT = "/content/dam";

    private static final String MIME_TYPE = "text/csv";
    private static final String CSV_FILENAME = "result.csv";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Reference
    private Scheduler scheduler;

    @Override
    public void checkReferences(String assetReferenceReportPath){
        if(StringUtils.isNotEmpty(assetReferenceReportPath)) {
            try (ResourceResolver resourceResolver = resolverFactory.getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, ADMINISTRATIVE_SERVICE_USER))){
                @NotNull Resource reportResource = resourceResolver.resolve(assetReferenceReportPath);
                if(!ResourceUtil.isNonExistingResource(reportResource)) {
                    String status = reportResource.getValueMap().get(JOB_STATUS, StringUtils.EMPTY);
                    String reportPath = reportResource.getValueMap().get("reportPath", StringUtils.EMPTY);
                    if(StringUtils.equalsAnyIgnoreCase(status, STARTING_STATUS, RUNNING_STATUS, UNSCHEDULED_STATUS) && StringUtils.isNotEmpty(reportPath)) {
                        readCSV(resourceResolver, status, reportPath, reportResource);
                    }
                }
            } catch (LoginException e) {
                LOGGER.error("Error occured while access Asset Reference Report Path {}", e.getMessage());
            }
        }
    }

    private void readCSV(ResourceResolver resourceResolver, String status, String reportPath, @NotNull Resource reportResource) {
        @NotNull Resource reportRes = resourceResolver.resolve(reportPath);
        Iterator<Resource> child = reportRes.getChildren().iterator();
        if (!ResourceUtil.isNonExistingResource(reportRes) && child.hasNext()) {
            Resource childResource = child.next();
            @Nullable Node reportNode = childResource.adaptTo(Node.class);
            try (InputStream csvStream = JcrUtils.readFile(reportNode);
                 CSVReader csvReader = new CSVReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
                List<String[]> csvData = csvReader.readAll();
                if (StringUtils.equalsIgnoreCase(STARTING_STATUS, status)) {
                    proccessRunning(resourceResolver, reportResource, csvData, 0);
                } else if (StringUtils.equalsIgnoreCase(RUNNING_STATUS, status)) {
                    @Nullable Integer count = reportResource.getValueMap().get("count", Integer.class);
                    if (count < csvData.size()) {
                        proccessRunning(resourceResolver, reportResource, csvData, count);
                    }
                }
            } catch (IOException | RepositoryException | CsvException e) {
                LOGGER.error("Error occured while reading Report File {}", e.getMessage());
            }
        }
    }

    private void proccessRunning(ResourceResolver resourceResolver, Resource reportResource, List<String[]> csvData, @Nullable Integer count) throws PersistenceException {
        String[] row = csvData.get(count++);
        if (!ArrayUtils.isEmpty(row) && row.length > 3 && StringUtils.isNotEmpty(row[2]) && StringUtils.equalsIgnoreCase(row[2], "IMAGE")) {
            boolean hasReference = checkReferences(resourceResolver, row[1]);
            @Nullable Resource resultCsv = reportResource.getChild(CSV_FILENAME);
            if(null != resultCsv) {
                @Nullable Node resultCsvNode = resultCsv.adaptTo(Node.class);
                try (InputStream csvStream = JcrUtils.readFile(resultCsvNode);
                     CSVReader csvReader = new CSVReader(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
                    List<String[]> resultCsvData = csvReader.readAll();
                    row[row.length-1] = String.valueOf(hasReference);
                    writeResults(reportResource, row, resultCsvData);
                } catch (IOException | RepositoryException | CsvException e) {
                    LOGGER.error("Error occured while reading Result CSV File {}", e.getMessage());
                }
            } else {
                List<String[]> resultCsvData = new ArrayList<>();
                String[] header = csvData.get(0);
                header[header.length-1] = "Has References";
                resultCsvData.add(header);
                row[row.length-1] = String.valueOf(hasReference);
                resultCsvData.add(row);
                writeResults(reportResource, row, resultCsvData);
            }
            ModifiableValueMap map = reportResource.adaptTo(ModifiableValueMap.class);
            map.put(JOB_STATUS, RUNNING_STATUS);
            map.put("count", count);
            resourceResolver.commit();
        } else if(count < csvData.size()) {
            proccessRunning(resourceResolver, reportResource, csvData, count);
        } else {
            ModifiableValueMap map = reportResource.adaptTo(ModifiableValueMap.class);
            map.put(JOB_STATUS, COMPLETED_STATUS);
            resourceResolver.commit();
            removeScheduler("BrokenAssetServlet");
        }
    }

    private void writeResults(Resource reportResource, String[] row, List<String[]> resultCsvData){
        try {
            resultCsvData.add(row);
            StringBuilder sb = resultCsvData.stream().map(this::convertToCSV).collect(StringBuilder::new,
                    (x, y) -> x.append(y), (a, b) -> a.append(",").append(b));
            InputStream is = IOUtils.toInputStream(sb.toString(), StandardCharsets.UTF_8);
            JcrUtils.putFile(reportResource.adaptTo(Node.class), CSV_FILENAME, MIME_TYPE, is);
        } catch (RepositoryException e) {
            LOGGER.error("Error occurred while writing csv Results file {}", e.getMessage());
        }
    }

    private String convertToCSV(String[] data) {
        return Stream.of(data)
                .collect(Collectors.joining(",", "", "\n"));
    }

    private boolean checkReferences(ResourceResolver resourceResolver, String assetPath) {
        boolean foundReference = false;
        String querySt = "SELECT * FROM [nt:base] AS s WHERE ISDESCENDANTNODE([/content]) and CONTAINS(s.*, '"+Text.escapeIllegalXpathSearchChars(assetPath).replaceAll("'", "''")+"')";
        try {
            QueryManager queryManager = resourceResolver.adaptTo(Session.class).getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(querySt, "JCR-SQL2");
            query.setLimit(50);
            QueryResult queryResult = query.execute();
            NodeIterator resourceResults = queryResult.getNodes();
            while (resourceResults.hasNext() && !foundReference) {
                Node child = resourceResults.nextNode();
                AssetReferenceSearch search = new AssetReferenceSearch(child, DAM_ROOT, resourceResolver);
                Map<String, Asset> assetRefResult = search.search();
                foundReference = assetRefResult.entrySet().stream().anyMatch(r -> r.getKey().equalsIgnoreCase(assetPath));
            }
            return foundReference;
        } catch (RepositoryException e) {
            LOGGER.error("Error occured while running the query {}", e.getMessage());
        }
        return foundReference;
    }

    @Deactivate
    protected void deactivate() {
        removeScheduler("BrokenAssetServlet");
    }

    private void removeScheduler(String schedulerJobName) {
        LOGGER.info("Removing scheduler: {}", schedulerJobName);
        scheduler.unschedule(String.valueOf(schedulerJobName));
    }
}
