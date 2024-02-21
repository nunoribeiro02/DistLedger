package pt.tecnico.distledger.namingserver;

import pt.tecnico.distledger.namingserver.exception.ServerNotRegisteredException;

import java.util.ArrayList;
import java.util.List;

public class ServiceEntry {

    private String serviceName;
    private List<ServerEntry> serverEntries = new ArrayList<>();

    public ServiceEntry(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setServiceName (String name) {
        this.serviceName = name;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public List<ServerEntry> getServerEntries() {
        return this.serverEntries;
    }

    public void addServerEntry(ServerEntry server) {
        serverEntries.add(server);
    }

    public void removeServerEntry(String server) throws ServerNotRegisteredException{
        int size = serverEntries.size();
        boolean removed = false;

        for(int i=0; i<size; i++) {
            if (serverEntries.get(i).getHost().equals(server)) {
                serverEntries.remove(i);
                removed = true;
                break;
            }
        }

        if (removed==false) {
            throw new ServerNotRegisteredException();
        }
    }
}
