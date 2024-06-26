package pl.allegro.tech.embeddedelasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.embeddedelasticsearch.HttpStatusCodes.OK;

class ElasticRestClient {

    private static final Logger logger = LoggerFactory.getLogger(ElasticRestClient.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final int elasticsearchHttpPort;
    private final HttpClient httpClient;
    private final IndicesDescription indicesDescription;
    private final TemplatesDescription templatesDescription;

    ElasticRestClient(int elasticsearchHttpPort, HttpClient httpClient, IndicesDescription indicesDescription, TemplatesDescription templatesDescription) {
        this.elasticsearchHttpPort = elasticsearchHttpPort;
        this.httpClient = httpClient;
        this.indicesDescription = indicesDescription;
        this.templatesDescription = templatesDescription;
    }

    void createIndices() {
        waitForClusterYellow();
        indicesDescription.getIndicesNames().forEach(this::createIndex);
    }

    void createIndex(String indexName) {
        if (!indexExists(indexName)) {
            HttpPut request = new HttpPut(url("/" + indexName));
            indicesDescription
                    .getIndexSettings(indexName)
                    .ifPresent(indexSettings -> setIndexSettingsAsEntity(request, indexSettings));
            httpClient.execute(request, response -> {
                if (response.getCode() != 200) {
                    String responseBody = readBodySafely(response);
                    throw new RuntimeException("Call to elasticsearch resulted in error:\n" + responseBody);
                }
            });
            waitForClusterYellow();
        }
    }

    private void setIndexSettingsAsEntity(HttpPut request, IndexSettings indexSettings) {
        request.setEntity(new StringEntity(indexSettings.toJson().toString(), ContentType.APPLICATION_JSON));
    }

    private boolean indexExists(String indexName) {
        HttpHead request = new HttpHead(url("/" + indexName));
        return httpClient.execute(request, response -> response.getCode() == OK);
    }

    void createTemplates() {
        templatesDescription.getTemplatesNames().forEach(this::createTemplate);
    }

    void createTemplate(String templateName) {
        if (!templateExists(templateName)) {
            HttpPut request = new HttpPut(url("/_template/" + templateName));
            request.setEntity(new StringEntity(templatesDescription.getTemplateSettings(templateName), ContentType.APPLICATION_JSON));
            httpClient.execute(request, response -> {
                if (response.getCode() != 200) {
                    String responseBody = readBodySafely(response);
                    throw new RuntimeException("Call to elasticsearch resulted in error:\n" + responseBody);
                }
            });
            waitForClusterYellow();
        }
    }

    private boolean templateExists(String templateName) {
        HttpHead request = new HttpHead(url("/" + templateName));
        return httpClient.execute(request, response ->
                response.getCode() == OK);
    }

    void deleteTemplates() {
        templatesDescription.getTemplatesNames().forEach(this::deleteTemplate);
    }

    void deleteTemplate(String templateName) {
        if (indexExists(templateName)) {
            HttpDelete request = new HttpDelete(url("/_template/" + templateName));
            httpClient.execute(request, (ClassicHttpResponse response) -> assertOk(response, "Delete request resulted in error"));
            waitForClusterYellow();
        } else {
            logger.warn("Template: {} does not exists so cannot be removed", templateName);
        }
    }

    private void waitForClusterYellow() {
        HttpGet request = new HttpGet(url("/_cluster/health?wait_for_status=yellow&timeout=60s"));
        httpClient.execute(request, (ClassicHttpResponse response) -> assertOk(response, "Cluster does not reached yellow status in specified timeout"));
    }

    void deleteIndices() {
        indicesDescription.getIndicesNames().forEach(this::deleteIndex);
    }

    void deleteIndex(String indexName) {
        if (indexExists(indexName)) {
            HttpDelete request = new HttpDelete(url("/" + indexName));
            httpClient.execute(request, (ClassicHttpResponse response) -> assertOk(response, "Delete request resulted in error"));
            waitForClusterYellow();
        } else {
            logger.warn("Index: {} does not exists so cannot be removed", indexName);
        }
    }

    void bulkIndex(Collection<IndexRequest> indexRequests) {
        String bulkRequestBody = indexRequests.stream()
                .flatMap(request ->
                    Stream.of(
                            indexMetadataJson(request.getIndexName(), request.getId(), request.getRouting()),
                            request.getJson()
                    )
                )
                .map((jsonNodes) -> jsonNodes.replace('\n', ' ').replace('\r', ' '))
                .collect(joining("\n")) + "\n";

        performBulkRequest(url("/_bulk"), bulkRequestBody);
    }

    private String indexMetadataJson(String indexName, String id, String routing) {
        StringJoiner joiner = new StringJoiner(",");

        if (indexName != null) {
            joiner.add("\"_index\": \"" + indexName + "\"");
        }

        if (id != null) {
            joiner.add("\"_id\": \"" + id + "\"");
        }

        if (routing != null) {
            if (newESVersion()) {
                joiner.add("\"routing\": \"" + routing + "\"");
            } else {
                joiner.add("\"_routing\": \"" + routing + "\"");
            }
        }
        return "{ \"index\": {" + joiner + "} }";
    }

    void refresh() {
        HttpPost request = new HttpPost(url("/_refresh"));
        try {
            httpClient.execute(request);
        } finally {
            request.reset();
        }
    }

    private boolean newESVersion() {
        HttpGet request = new HttpGet(url("/"));
        return httpClient.execute(request, response -> {
            JsonNode jsonNode;
            try {
                jsonNode = OBJECT_MAPPER.readTree(readBodySafely(response));
            } catch (IOException e) {
                return false;
            }
            String esV = jsonNode.get("version").get("number").asText();
            return Integer.parseInt(esV.substring(0, 1)) >= 7; //if version is 7 and above
        });
    }

    private void performBulkRequest(String requestUrl, String bulkRequestBody) {
        HttpPost request = new HttpPost(requestUrl);
        request.setHeader(new BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"));
        request.setEntity(new StringEntity(bulkRequestBody, UTF_8));
        httpClient.execute(request, (ClassicHttpResponse response) -> assertOk(response, "Request finished with error"));
        refresh();
    }

    private String url(String path) {
        return "http://localhost:" + elasticsearchHttpPort + path;
    }

    private void assertOk(ClassicHttpResponse response, String message) {
        if (response.getCode() != OK) {
            throw new IllegalStateException(message + "\nResponse body:\n" + readBodySafely(response));
        }
    }

    private String readBodySafely(ClassicHttpResponse response) {
        try {
            return IOUtils.toString(response.getEntity().getContent(), UTF_8);
        } catch (IOException e) {
            logger.error("Error during reading response body", e);
            return "";
        }
    }

    List<String> fetchAllDocuments(String... indices) {
        return fetchAllDocuments(null, indices);
    }

    List<String> fetchAllDocuments(String routing, String... indices) {
        if (indices.length == 0) {
            return searchForDocuments().collect(toList());
        } else {
            return Stream.of(indices)
                    .flatMap((index) -> searchForDocuments(index, routing))
                    .collect(toList());
        }
    }

    private Stream<String> searchForDocuments() {
        return searchForDocuments(null, null);
    }

    private Stream<String> searchForDocuments(String index, String routing) {
        String searchCommand = prepareQuery(index, routing);
        String body = fetchDocuments(searchCommand);
        return parseDocuments(body);
    }

    private String prepareQuery(String index, String routing) {
        StringBuilder sb = new StringBuilder();
        if (index != null)
            sb.append('/').append(index);
        sb.append("/_search");
        if (routing != null)
            sb.append("?routing=").append(routing);
        return sb.toString();
    }

    private String fetchDocuments(String searchCommand) {
        HttpGet request = new HttpGet(url(searchCommand));
        return httpClient.execute(request, response -> {
            assertOk(response, "Error during search (" + searchCommand + ")");
            return readBodySafely(response);
        });
    }

    private Stream<String> parseDocuments(String body) {
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(body);
            return StreamSupport.stream(jsonNode.get("hits").get("hits").spliterator(), false)
                    .map(hitNode -> hitNode.get("_source"))
                    .map(JsonNode::toString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
