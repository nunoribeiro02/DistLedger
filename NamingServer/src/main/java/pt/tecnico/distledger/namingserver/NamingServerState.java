package pt.tecnico.distledger.namingserver;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.print.attribute.HashAttributeSet;
import pt.tecnico.distledger.namingserver.exception.AlreadyRegisteredServerException;
import pt.tecnico.distledger.namingserver.exception.ServerNotRegisteredException;

public class NamingServerState {

    // Set flag to true using the -Ddebug to print debug messages. 
    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);

    /** Helper method to print debug messages. */
    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    private Map<String, ServiceEntry> services = new HashMap<>();

    public NamingServerState() {
        ServiceEntry serviceEntry = new ServiceEntry("DistLedger");
        addServiceEntry("DistLedger", serviceEntry);
    }

    /* addServiceEntry: puts given service and given entry to the Map 'services' */
    public void addServiceEntry(String service, ServiceEntry entry) {
        debug("[addServiceEntry] arguments -> service: " + service + " | entry: "  + entry);

        services.put(service, entry);
    }

    /* removeServiceEntry: removes entry with key given service from the Map 'services' */
    public void removeServiceEntry(String service) {
        debug("[removeServiceEntry] arguments -> service: " + service);
        
        services.remove(service);
    }


    /* addServer: Adds ServerEntry to the ServiceEntry from the Map 'services'
        Function called by registerServer() */
    public void addServer(String service, ServerEntry server) {
        debug("[addServer] arguments-> service: " + service + " | server: "+ server);

        services.get(service).addServerEntry(server);
        debug("Service Entry: " + services.get(service) + "\n");
    }

    /** deleteServer: Deletes ServerEntry from the ServiceEntry in the Map 'services' */
    public synchronized void deleteServer(String service, String host_port) throws ServerNotRegisteredException{
        debug("[deleteServer] arguments -> service: " + service + " | host_port: " + host_port);
    
        services.get(service).removeServerEntry(host_port);
    }

    /** getServers: Returns a list of serves for given service
     * Function called by getAllHosts() and getHosts() */
    public List<ServerEntry> getServers(String service) {
        debug("[getServers] arguments -> service: " + service);

        return services.get(service).getServerEntries();
    }

    /** getAllHosts: Returns a list of all host:port for given service  */
    public List<String> getAllHosts(String service) {
        debug("[getAllHosts] arguments -> service: " + service);

        List<String> hosts = new ArrayList<>();
        getServers(service).forEach(server -> hosts.add(server.getHost()));
        return hosts;
    }

    /** getHostsByQualifier: Returns a list of host:port for given service and qualifier
     * Function called by lookUpServer() */
    public List<String> getHostsByQualifier(String service, String qualifier) {
        debug("[getHostsByQualifier] arguments -> service: " + service + " | qualifier: " + qualifier);

        System.out.println("Registered Servers:" + services.get(service).getServerEntries());

        List<ServerEntry> hosts = services.get(service).getServerEntries();
        List<String> requestedHosts = new ArrayList<>();
        
        for (int i=0; i<hosts.size(); i++) {
            if(hosts.get(i).getQualifier().equals(qualifier)) 
                requestedHosts.add(hosts.get(i).getHost());
        }
        
        return requestedHosts;
    }

    /** getHosts: Returns a list of host:port for given service and qualifier
     *  Function called by lookUpServer() */

    public List<String> getHosts(String serviceName){
        debug("[getHosts] arguments -> serviceName: " + serviceName);

        List<String> requestedServers = new ArrayList<>();

        getServers(serviceName).forEach(server -> requestedServers.add(server.getHost()));
        return requestedServers;
    }

    /** registerServer:  Registers a ServerEntry in the Map 'services' */
    public synchronized void registerServer(String service, String qualifier, String host_port) 
        throws AlreadyRegisteredServerException{
        debug("[registerServer] arguments -> service: " + service + " | qualifier: " + qualifier + " | host_port:" + host_port);

        ServerEntry server = new ServerEntry(host_port, qualifier);

        // Check if the server is already registered
        if (services.get(service).getServerEntries().contains(server)){
            debug("Throwing Exception: AlreadyRegisterdServer: " + server);
            throw new AlreadyRegisteredServerException();
        }

        addServer(service, server);
    }

    /** lookUpServer:  Returns a List<String> of Host:port for given serviceName and qualifier
     * Or for given serviceName if qualifier isn't given
     * Or empty if neither serviceName nor qualifier are given */
    public synchronized List<String> lookUpServer(String serviceName, String qualifier) {
        debug("[lookUpServer] arguments -> serviceName: " + serviceName + " | qualifier: " + qualifier);

        List<String> requestedServers = new ArrayList<>();

        //Check if the serviceName exists in the Map 'services'
        if (services.containsKey(serviceName)){

            if (qualifier != null){
                // gets List<String> of Host:port for given serviceName and qualifier
                requestedServers = getHostsByQualifier(serviceName, qualifier);
            }
            else{
                /* gets List<String> of Host:port for given serviceName, 
                since no qualifier provided*/
                requestedServers = getHosts(serviceName);
            }   
        }
        System.out.println("Requested Servers:" + requestedServers);

        return requestedServers;
    }

    /** getQualifiers: Returns a list of qualifiers for given service name */
     public List<String> getQualifiers(String serviceName){
        debug("[getHosts] arguments -> serviceName: " + serviceName);

        List<String> qualifiers = new ArrayList<>();

        getServers(serviceName).forEach(server -> qualifiers.add(server.getQualifier()));
        return qualifiers;
    }

    /*searchServers: Searches for servers with the given service name 
        returns a list of qualifiers of the searched severs */
    public synchronized List<String> searchServers(String serviceName) {
        List<String> serversQualifiers = new ArrayList<>();

        getServers(serviceName).forEach(server -> serversQualifiers.add(server.getQualifier()));
        return serversQualifiers;
    }
}