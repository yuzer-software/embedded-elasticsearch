package pl.allegro.tech.embeddedelasticsearch;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.embeddedelasticsearch.InstallationDescription.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.embeddedelasticsearch.Require.require;

public final class EmbeddedElastic {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedElastic.class);

    private final String esJavaOpts;
    private final InstanceSettings instanceSettings;
    private final IndicesDescription indicesDescription;
    private final TemplatesDescription templatesDescription;
    private final InstallationDescription installationDescription;
    private final long startTimeoutInMs;
    private final boolean withSecurity;
    private ElasticServer elasticServer;
    private ElasticRestClient elasticRestClient;
    private volatile boolean started = false;
    private final JavaHomeOption javaHome;

    public static Builder builder() {
        return new Builder();
    }

    private EmbeddedElastic(String esJavaOpts, InstanceSettings instanceSettings,
                            IndicesDescription indicesDescription, TemplatesDescription templatesDescription,
                            InstallationDescription installationDescription, long startTimeoutInMs, JavaHomeOption javaHome,
                            boolean withSecurity) {
        this.esJavaOpts = esJavaOpts;
        this.instanceSettings = instanceSettings;
        this.indicesDescription = indicesDescription;
        this.templatesDescription = templatesDescription;
        this.installationDescription = installationDescription;
        this.startTimeoutInMs = startTimeoutInMs;
        this.javaHome = javaHome;
        this.withSecurity = withSecurity;
    }

    /**
     * Downloads Elasticsearch with specified plugins, setups them and starts.
     *
     * @throws IOException if the installation directory cannot be created or the file already exists but is not a directory.
     * @throws InterruptedException if the current thread is interrupted by another thread while it is waiting, then the wait is ended and an InterruptedException is thrown.
     */
    public synchronized EmbeddedElastic start() throws IOException, InterruptedException {
        if (!started) {
            logger.info("Starting embedded Elastic.");
            started = true;
            installElastic();
            startElastic();
            createRestClient();
            createTemplates();
            createIndices();
        }
        return this;
    }

    public String getPassword(String user) {
        return this.elasticServer.getPassword(user);
    }

    private void installElastic() throws IOException, InterruptedException {
        ElasticSearchInstaller elasticSearchInstaller = new ElasticSearchInstaller(instanceSettings, installationDescription);
        logger.info("Installing elasticsearch to " + elasticSearchInstaller.getInstallationDirectory());
        elasticSearchInstaller.install();
        File executableFile = elasticSearchInstaller.getExecutableFile();
        File executableSetupPasswordFile = elasticSearchInstaller.getPasswordSetupExecutableFile();
        File installationDirectory = elasticSearchInstaller.getInstallationDirectory();
        elasticServer = new ElasticServer(esJavaOpts, installationDirectory, executableFile, executableSetupPasswordFile, startTimeoutInMs,
                installationDescription.isCleanInstallationDirectoryOnStop(), javaHome);
    }

    private void startElastic() throws InterruptedException {
        if (!elasticServer.isStarted()) {
            elasticServer.start();
        }
    }

    private void createRestClient() {
        HttpClient httpClient;
        if (withSecurity) {
            httpClient = new HttpClient("elastic", elasticServer.getPassword("elastic"));
        } else {
            httpClient = new HttpClient();
        }

        elasticRestClient = new ElasticRestClient(elasticServer.getHttpPort(), httpClient, indicesDescription, templatesDescription);
    }

    /**
     * Stops Elasticsearch instance and removes data
     */
    public synchronized void stop() {
        if (elasticServer != null && started) {
            started = false;
            elasticServer.stop();
        }
    }

    /**
     * Index documents
     *
     * @param indexName target index
     * @param idJsonMap map where keys are documents ids and values are documents represented as JSON
     */
    public void index(String indexName, Map<CharSequence, CharSequence> idJsonMap) {
        index(
                idJsonMap.entrySet().stream()
                        .map(entry -> new IndexRequest.IndexRequestBuilder(indexName, entry.getValue().toString())
                                .withId(entry.getKey().toString()).build()
                        )
                        .collect(toList())
        );
    }

    /**
     * Index documents
     *
     * @param indexName target index
     * @param json      document represented as JSON
     */
    public void index(String indexName, String... json) {
        index(
                Arrays.stream(json)
                        .map(item -> new IndexRequest.IndexRequestBuilder(indexName, item).build())
                        .collect(toList())
        );
    }

    /**
     * Index documents
     *
     * @param indexName target index
     * @param jsons     documents represented as JSON
     */
    public void index(String indexName, List<CharSequence> jsons) {
        index(
                jsons.stream()
                        .map(json -> new IndexRequest.IndexRequestBuilder(indexName, json.toString()).build())
                        .collect(toList())
        );
    }

    /**
     * Index single document with routing
     *
     * @param indexRequests document to be indexed along with metadata
     */
    public void index(List<IndexRequest> indexRequests) {
        elasticRestClient.bulkIndex(indexRequests);
    }

    /**
     * Recreates all instances (i.e. deletes and creates them again)
     */
    public void recreateIndices() {
        deleteIndices();
        createIndices();
    }

    /**
     * Recreates specified index (i.e. deletes and creates it again)
     *
     * @param indexName index to recreate
     */
    public void recreateIndex(String indexName) {
        deleteIndex(indexName);
        createIndex(indexName);
    }

    /**
     * Delete all indices
     */
    public void deleteIndices() {
        elasticRestClient.deleteIndices();
    }

    /**
     * Delete specified index
     *
     * @param indexName index do delete
     */
    public void deleteIndex(String indexName) {
        elasticRestClient.deleteIndex(indexName);
    }

    /**
     * Create all indices
     */
    public void createIndices() {
        elasticRestClient.createIndices();
    }

    /**
     * Create specified index. Note that you can specify only index from list of indices specified during EmbeddedElastic creation
     *
     * @param indexName index to create
     */
    public void createIndex(String indexName) {
        elasticRestClient.createIndex(indexName);
    }

    public void createTemplates() {
        elasticRestClient.createTemplates();
    }


    /**
     * Recreates all templates (i.e. deletes and creates them again)
     */
    public void recreateTemplates() {
        deleteTemplates();
        createTemplates();
    }

    /**
     * Recreates specified template (i.e. deletes and creates it again)
     *
     * @param templateName index to recreate
     */
    public void recreateTemplate(String templateName) {
        deleteTemplate(templateName);
        createTemplate(templateName);
    }

    /**
     * Delete all templates
     */
    public void deleteTemplates() {
        elasticRestClient.deleteTemplates();
    }

    /**
     * Delete specified template
     *
     * @param templateName template do delete
     */
    public void deleteTemplate(String templateName) {
        elasticRestClient.deleteTemplate(templateName);
    }

    /**
     * Create specified template. Note that you can specify only template from list of templates specified during EmbeddedElastic creation
     *
     * @param templateName template to create
     */
    public void createTemplate(String templateName) {
        elasticRestClient.createTemplate(templateName);
    }

    /**
     * Refresh indices. Can be useful in tests that uses multiple threads
     */
    public void refreshIndices() {
        elasticRestClient.refresh();
    }

    /**
     * Fetch all documents from specified indices. Useful for logging and debugging
     *
     * @throws HttpClient.HttpRequestException in case of a problem or the connection was aborted
     * @return list containing documents sources represented as JSON
     */
    public List<String> fetchAllDocuments(String... indices) {
        return elasticRestClient.fetchAllDocuments(indices);
    }

    /**
     * Get transport tcp port number used by Elasticsearch
     */
    public int getTransportTcpPort() {
        return elasticServer.getTransportTcpPort();
    }

    /**
     * Get http port number
     */
    public int getHttpPort() {
        return elasticServer.getHttpPort();
    }

    /**
     * Builder for EmbeddedElastic.
     */
    public static final class Builder {

        private InstallationSource installationSource = null;
        private final List<Plugin> plugins = new ArrayList<>();
        private final Map<String, Optional<IndexSettings>> indices = new HashMap<>();
        private final Map<String, String> templates = new HashMap<>();
        private InstanceSettings settings = new InstanceSettings();
        private String esJavaOpts = "";
        private long startTimeoutInMs = 15_000;
        private boolean cleanInstallationDirectoryOnStop = true;
        private File installationDirectory = null;
        private File downloadDirectory = null;
        private int downloaderConnectionTimeoutInMs = 3_000;
        private int downloaderReadTimeoutInMs = 300_000;
        private Proxy downloadProxy = null;
        private JavaHomeOption javaHome = JavaHomeOption.useSystem();
        private boolean withSecurity = false;

        private Builder() {
        }

        public Builder withSetting(String name, Object value) {
            settings = settings.withSetting(name, value);
            if (name.equals("xpack.security.enabled") && value.equals("true")) {
                withSecurity = true;
            }
            return this;
        }

        public Builder withEsJavaOpts(String javaOpts) {
            this.esJavaOpts = javaOpts;
            return this;
        }

        public Builder withInstallationDirectory(File installationDirectory) {
            this.installationDirectory = installationDirectory;
            return this;
        }

        public Builder withDownloadDirectory(File downloadDirectory) {
            this.downloadDirectory = downloadDirectory;
            return this;
        }

        public Builder withCleanInstallationDirectoryOnStop(boolean cleanInstallationDirectoryOnStop) {
            this.cleanInstallationDirectoryOnStop = cleanInstallationDirectoryOnStop;
            return this;
        }

        /**
         * Desired version of Elasticsearch. It will be used to generate download URL to official mirrors
         */
        public Builder withElasticVersion(String version) {
            this.installationSource = new InstallFromVersion(version);
            return this;
        }

        /**
         * <p>Elasticsearch download URL. Will overwrite download url generated by withElasticVersion method.</p>
         * <p><strong>Specify urls only to locations that you trust!</strong></p>
         */
        public Builder withDownloadUrl(URL downloadUrl) {
            this.installationSource = new InstallFromDirectUrl(downloadUrl);
            return this;
        }

        /**
         * In resource path to Elasticsearch zip archive.
         */
        public Builder withInResourceLocation(String inResourcePath) {
            this.installationSource = new InstallFromResources(inResourcePath);
            return this;
        }

        /**
         * Plugin that should be installed with created instance. Treat invocation of this method as invocation of elasticsearch-plugin install command:
         * <pre>./elasticsearch-plugin install EXPRESSION</pre>
         */
        public Builder withPlugin(String expression) {
            this.plugins.add(new Plugin(expression));
            return this;
        }

        /**
         * Index that will be created when EmbeddedElastic is started.
         */
        public Builder withIndex(String indexName, IndexSettings indexSettings) {
            this.indices.put(indexName, Optional.of(indexSettings));
            return this;
        }

        /**
         * Index that will be created when EmbeddedElastic is started.
         */
        public Builder withIndex(String indexName) {
            this.indices.put(indexName, Optional.empty());
            return this;
        }

        /**
         * Add a template that will be created after Elasticsearch cluster started.
         */
        public Builder withTemplate(String name, String templateBody) {
            this.templates.put(name, templateBody);
            return this;
        }

        /**
         * Add a template that will be created after Elasticsearch cluster started
         *
         * @throws IOException if an I/O error occurs when getting the contents of templateBody.
         */
        public Builder withTemplate(String name, InputStream templateBody) throws IOException {
            return withTemplate(name, IOUtils.toString(templateBody, UTF_8));
        }

        /**
         * How long should embedded-elasticsearch wait for elasticsearch to startup. Defaults to 15 seconds
         */
        public Builder withStartTimeout(long value, TimeUnit unit) {
            startTimeoutInMs = unit.toMillis(value);
            return this;
        }

        /**
         * Set connection timeout for HTTP client used by downloader
         */
        public Builder withDownloaderConnectionTimeout(long value, TimeUnit unit) {
            downloaderConnectionTimeoutInMs = (int) unit.toMillis(value);
            return this;
        }

        /**
         * Set read timeout for HTTP client used by downloader
         */
        public Builder withDownloaderReadTimeout(long value, TimeUnit unit) {
            downloaderReadTimeoutInMs = (int) unit.toMillis(value);
            return this;
        }

        /**
         * Set proxy that should be used to download elastic package
         */
        public Builder withDownloadProxy(Proxy proxy) {
            downloadProxy = proxy;
            return this;
        }

        public Builder withJavaHome(JavaHomeOption javaHome) {
            this.javaHome = javaHome;
            return this;
        }

        public EmbeddedElastic build() {
            require(installationSource != null, "You must specify elasticsearch version, or download url");
            return new EmbeddedElastic(
                    esJavaOpts,
                    settings,
                    new IndicesDescription(indices),
                    new TemplatesDescription(templates),
                    new InstallationDescription(installationSource, downloadDirectory, installationDirectory, cleanInstallationDirectoryOnStop, plugins, downloaderConnectionTimeoutInMs, downloaderReadTimeoutInMs, downloadProxy),
                    startTimeoutInMs,
                    javaHome,
                    withSecurity);
        }

    }
}

