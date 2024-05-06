package pl.allegro.tech.embeddedelasticsearch

import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder

import static java.util.concurrent.TimeUnit.MINUTES
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.HTTP_PORT
import static pl.allegro.tech.embeddedelasticsearch.SampleIndices.*

class EmbeddedElasticSpec extends EmbeddedElasticCoreApiBaseSpec {

    static final ELASTIC_VERSION = "7.7.0"
    static final HTTP_PORT_VALUE = 9999

    static EmbeddedElastic embeddedElasticServer = EmbeddedElastic.builder()
            .withElasticVersion(ELASTIC_VERSION)
            .withSetting(HTTP_PORT, HTTP_PORT_VALUE)
            .withSetting("xpack.ml.enabled", "false") // This cause issues on mac os so disable in tests
            .withSetting("xpack.security.enabled", "true")
            .withEsJavaOpts("-Xms128m -Xmx512m")
            .withTemplate(CARS_TEMPLATE_NAME, CARS_TEMPLATE_7x)
            .withIndex(CARS_INDEX_NAME, CARS_INDEX_7x)
            .withIndex(BOOKS_INDEX_NAME, BOOKS_INDEX)
            .withStartTimeout(2, MINUTES)
            .build()
            .start()

    @Override
    EmbeddedElastic getEmbeddedElastic() {
        return embeddedElasticServer
    }
    static RestHighLevelClient client = createClient()

    def setup() {
        embeddedElastic.recreateIndices()
    }

    def cleanupSpec() {
        client.close()
        embeddedElastic.stop()
    }

    static RestHighLevelClient createClient() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider()
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", embeddedElasticServer.getPassword("elastic")))

        return new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", HTTP_PORT_VALUE)).setHttpClientConfigCallback(
                        httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                )
        )
    }

    @Override
    void index(IndexRequest indexRequest) {
        IndexRequest newIndexRequest = new IndexRequest.IndexRequestBuilder(indexRequest.getIndexName(), indexRequest.getJson()).build()
        index(Arrays.asList(newIndexRequest))
    }

    @Override
    void index(List<IndexRequest> indexRequests) {
        ArrayList<IndexRequest> newIndexRequests = new ArrayList<>()
        for (IndexRequest newIndexRequest : indexRequests) {
            newIndexRequests.add(new IndexRequest.IndexRequestBuilder(newIndexRequest.getIndexName(), newIndexRequest.getJson()).withId(newIndexRequest.getId()).withRouting(newIndexRequest.getRouting()).build())
        }
        embeddedElastic.index(newIndexRequests)
    }

    @Override
    void index(PaperBook book) {
        index(new IndexRequest.IndexRequestBuilder(BOOKS_INDEX_NAME, toJson(book)).build())
    }

    @Override
    void index(Car car) {
        index(new IndexRequest.IndexRequestBuilder(CARS_INDEX_NAME, toJson(car)).build())
    }

    @Override
    void index(String indexName, Map idJsonMap) {
        embeddedElastic.index(indexName, idJsonMap)
    }

    @Override
    List<String> fetchAllDocuments() {
        fetchAllDocuments(CARS_INDEX_NAME) + fetchAllDocuments(BOOKS_INDEX_NAME)
    }

    @Override
    List<String> fetchAllDocuments(String indexName) {
        final searchRequest = new SearchRequest(indexName)
                .source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()))

        client.search(searchRequest, RequestOptions.DEFAULT)
                .hits.hits.toList()
                .collect { it.sourceAsString }
    }

    @Override
    List<String> fetchAllDocuments(String indexName, String routing) {
        final searchRequest = new SearchRequest(indexName)
                .routing(routing)
                .source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()))

        client.search(searchRequest, RequestOptions.DEFAULT)
                .hits.hits.toList()
                .collect { it.sourceAsString }
    }

    @Override
    List<String> searchByTerm(String indexName, String fieldName, String value) {
        final searchRequest = new SearchRequest()
                .source(new SearchSourceBuilder().query(QueryBuilders.termQuery(fieldName, value)))

        client.search(searchRequest, RequestOptions.DEFAULT)
                .hits.hits.toList()
                .collect { it.sourceAsString }
    }

    @Override
    String getById(String indexName, String id) {
        final getRequest = new GetRequest(indexName, id)
        client.get(getRequest, RequestOptions.DEFAULT).sourceAsString
    }
}
