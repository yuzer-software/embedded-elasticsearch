package pl.allegro.tech.embeddedelasticsearch;

public interface PopularProperties {
    String HTTP_PORT = "http.port";
    /** Set the client port, starting Elastic 8. */
    String TRANSPORT_PORT = "transport.port";
    /** Set the client port, before Elastic 7. */
    String TRANSPORT_TCP_PORT = "transport.tcp.port";
    String CLUSTER_NAME = "cluster.name";
}
