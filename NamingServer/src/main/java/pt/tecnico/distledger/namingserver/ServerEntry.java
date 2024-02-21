package pt.tecnico.distledger.namingserver;

public class ServerEntry {

    private String host;
    private String qualifier;

    public ServerEntry(String host_port, String qualifier) {
        this.host = host_port;
        this.qualifier = qualifier;
    }

    public void setHost(String host_port) {
        this.host = host_port;
    }

    public String getHost() {
        return this.host;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public String getQualifier() {
        return this.qualifier;
    }

    
    public boolean equals(String host_port, String qualifier){
        return this.host.equals(host_port) && this.qualifier.equals(qualifier);

    }
}
