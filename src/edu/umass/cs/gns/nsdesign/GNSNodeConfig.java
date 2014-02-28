package edu.umass.cs.gns.nsdesign;


import com.google.common.collect.ImmutableSet;
import edu.umass.cs.gns.main.GNS;
import edu.umass.cs.gns.nio.NodeConfig;
import edu.umass.cs.gns.util.BestServerSelection;
import edu.umass.cs.gns.util.HostInfo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/*** DONT not use any class in package edu.umass.cs.gns.nsdesign ***/

/**
 * **
 * This is a utility class that parses configuration files to gather information about each name server/local name server
 * in the system.
 *
 * To use the nio package, GNS implements <code>NodeConfig</code> interface in this class.
 *
 * Earlier implementation of this class was static. We have made this class non-static to be able to test
 * with multiple nodes within a single JVM.
 *
 *
 * @author Abhigyan
 */
public class GNSNodeConfig implements NodeConfig{

  /**
   * Contains information about each name server. <Key = HostID, Value = HostInfo>
   *
   */
  private  ConcurrentMap<Integer, HostInfo> hostInfoMapping =
          new ConcurrentHashMap<Integer, HostInfo>(16, 0.75f, 8);
  /**
   * A subset of the above with just the ids of the nameservers, not the LNSs
   */
  private  ConcurrentMap<Integer, Integer> nameServerMapping =
          new ConcurrentHashMap<Integer, Integer>(16, 0.75f, 8);
  /**
   * Number of name server in the system *
   */
  private  int numberOfNameServers;
  /**
   * Id of the closestNameServer to this node *
   */
  private  int closestNameServer;

  /**
   * @return the numberOfNameServer
   */
  public  int getNumberOfNameServers() {
    return numberOfNameServers;
  }

  public  int getNumberOfHosts() {
    return hostInfoMapping.size();
  }

  /**
   * @return the closestNameServer
   */
  public  int getClosestNameServer() {
    return closestNameServer;
  }

  /**
   * ONLY TO BE USED FOR TESTING PURPOSES!!
   *
   * @param numberOfNameServers
   */
  public  void setNumberOfNameServers(int numberOfNameServers) {
    this.numberOfNameServers = numberOfNameServers;
  }

  /**
   * **
   * Parse the host's information file to create a mapping of node information for name servers and local name severs
   *
   * @param nodeInfoFile Format: HostID IsNS? IPAddress StartingPort Ping-Latency Latitude Longitude
   * @throws java.net.UnknownHostException
   * @throws NumberFormatException
   * @throws java.io.IOException *
   */
  public GNSNodeConfig(String nodeInfoFile, int nameServerID) {
    // Reads in data from a text file containing information about each name server
    // in the system.
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(nodeInfoFile));
    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
      System.exit(0);
    }

    int nameServerCount = 0;
    double smallestPingLatency = Double.MAX_VALUE;

    try {
      while (br.ready()) {
        String line = br.readLine();
        if (line == null || line.equals("") || line.equals(" ")) {
          continue;
        }

        String[] tokens = line.split("\\s+");

        // Check for file's format
        if (tokens.length != 7) {
          System.err.println("Error: File " + nodeInfoFile + " formated incorrectly");
          System.err.println("HostId: " + nameServerID);
          System.err.println("Token Length: " + tokens.length + " ");
          for (String str : tokens) {
            System.err.println(str);
          }
          System.err.println("Format:\nHostID IsNS? IPAddress [StartingPort | - ] Ping-Latency Latitude Longitude");
          System.err.println("\nwhere IsNS? is yes or no\n and StartingPort is a port number, but can also be a dash \"-\""
                  + " (or the word \"default\") to indicate that the default port can be used in the distributed case.");
          //System.err.println("Format:\nNameServerID IPAddress Ping-Latency Latitude Longitude");
          System.exit(0);
        }

        int id = Integer.parseInt(tokens[0]);
        String isNameServerString = tokens[1];
        String ipAddressString = tokens[2];
        String startingPortString = tokens[3];
        double pingLatency = Double.parseDouble(tokens[4]);
        double latitude = Double.parseDouble(tokens[5]);
        double longitude = Double.parseDouble(tokens[6]);
        boolean isNameServer;

        if (isNameServerString.startsWith("yes")
                || isNameServerString.startsWith("Yes")
                || isNameServerString.startsWith("YES")
                || isNameServerString.startsWith("X")
                || isNameServerString.startsWith("true")
                || isNameServerString.startsWith("True")
                || isNameServerString.startsWith("TRUE")) {
          isNameServer = true;
          nameServerMapping.put(id, id);
          nameServerCount++;
          //Update the id closest name server
          if (pingLatency >= 0 && pingLatency < smallestPingLatency) {
            smallestPingLatency = pingLatency;
            closestNameServer = id;
          }
        } else {
          isNameServer = false;
        }
        InetAddress ipAddress = null;
        try {
          ipAddress = InetAddress.getByName(ipAddressString);
        } catch (UnknownHostException e) {
          System.err.println("Problem parsing IP address for NS " + nameServerID + " :" + e);
        }
        int startingPort;
        if (startingPortString.startsWith("-") || startingPortString.startsWith("default")) {
          startingPort = GNS.startingPort;
        } else {
          startingPort = Integer.parseInt(startingPortString);
        }


        addHostInfo(id, ipAddress, startingPort, pingLatency, latitude, longitude);
      }
      br.close();
    } catch (NumberFormatException e) {
      System.err.println("Problem parsing number in host config for NS " + nameServerID + " :" + e);
    } catch (IOException e) {
      System.err.println("Problem reading host config for NS " + nameServerID + " :" + e);
    }

    GNS.getLogger().fine("Number of name servers is : " + nameServerCount);
    numberOfNameServers = nameServerCount;
    GNS.getLogger().fine("Closest name server is " + closestNameServer);
    //    System.exit(2);
  }

  public  void addHostInfo(int id, InetAddress ipAddress, int startingPort, double pingLatency, double latitude, double longitude) {
    HostInfo nodeInfo = new HostInfo(id, ipAddress, startingPort, pingLatency, latitude, longitude);
    GNS.getLogger().fine(nodeInfo.toString());
    hostInfoMapping.put(id, nodeInfo);
  }

  public  Set<Integer> getAllHostIDs() {
    return ImmutableSet.copyOf(hostInfoMapping.keySet());
  }

  public  Set<Integer> getAllNameServerIDs() {
    //      HashSet<Integer> x  = new HashSet<Integer>();
    //    x.add(1);x.add(2);return x;
    return ImmutableSet.copyOf(nameServerMapping.keySet());
  }

  public  boolean isNameServer(int id) {
    if (nameServerMapping.containsKey(id)) {
      return true;
    }
    return false;
  }

  /**
   * **
   * Returns the HostInfo structure for a host
   *
   * @param id
   * @return NameServerInfo *
   */
  public  HostInfo getHostInfo(int id) {
    return hostInfoMapping.get(id);
  }

  /**
   * Returns the TCP port of a nameserver
   *
   * @param id Nameserver id
   * @return the stats port for a nameserver
   */
  public  int getNSTcpPort(int id) {
    HostInfo nodeInfo = hostInfoMapping.get(id);
    return (nodeInfo == null) ? -1 : nodeInfo.getStartingPortNumber() + GNS.PortType.NS_TCP_PORT.getOffset();
  }

  /**
   * Returns the TCP port of a Local Nameserver
   *
   * @param id Nameserver id
   * @return the stats port for a nameserver
   */
  public  int getLNSTcpPort(int id) {
    HostInfo nodeInfo = hostInfoMapping.get(id);
    return (nodeInfo == null) ? -1 : nodeInfo.getStartingPortNumber() + GNS.PortType.LNS_TCP_PORT.getOffset();
  }

  /**
   * Returns the UDP port of a Local Nameserver
   *
   * @param id
   * @return
   */
  public  int getLNSUdpPort(int id) {
    HostInfo nodeInfo = hostInfoMapping.get(id);
    return (nodeInfo == null) ? -1 : nodeInfo.getStartingPortNumber() + GNS.PortType.LNS_UDP_PORT.getOffset();
  }

  public  int getNSUdpPort(int id) {
    HostInfo nodeInfo = hostInfoMapping.get(id);
    return (nodeInfo == null) ? -1 : nodeInfo.getStartingPortNumber() + GNS.PortType.NS_UDP_PORT.getOffset();
  }

  /**
   * Returns the Admin port of a Nameserver
   *
   * @param id Nameserver id
   * @return the active nameserver information port of a nameserver. *
   */
  public  int getNSAdminRequestPort(int id) {
    HostInfo nodeInfo = hostInfoMapping.get(id);
    return (nodeInfo == null) ? -1 : nodeInfo.getStartingPortNumber() + GNS.PortType.NS_ADMIN_PORT.getOffset();
  }

  /**
   * Returns the Admin port of a Local Nameserver
   *
   * @param id
   * @return
   */
  public  int getLNSAdminRequestPort(int id) {
    HostInfo nodeInfo = hostInfoMapping.get(id);
    return (nodeInfo == null) ? -1 : nodeInfo.getStartingPortNumber() + GNS.PortType.LNS_ADMIN_PORT.getOffset();
  }

  /**
   * Returns the response port of a Local nameserver
   *
   * @param id
   * @return
   */
  public  int getLNSAdminResponsePort(int id) {
    HostInfo nodeInfo = hostInfoMapping.get(id);
    return (nodeInfo == null) ? -1 : nodeInfo.getStartingPortNumber() + GNS.PortType.LNS_ADMIN_RESPONSE_PORT.getOffset();
  }

  /**
   * Returns the dump response port of a Local nameserver
   *
   * @param id
   * @return
   */
  public  int getLNSAdminDumpReponsePort(int id) {
    HostInfo nodeInfo = hostInfoMapping.get(id);
    return (nodeInfo == null) ? -1 : nodeInfo.getStartingPortNumber() + GNS.PortType.LNS_ADMIN_DUMP_RESPONSE_PORT.getOffset();
  }

  /**
   * Returns the IP address of a name server.
   *
   * @param id Nameserver id
   * @return IP address of a nameserver
   */
  public  InetAddress getIPAddress(int id) {
    HostInfo nodeInfo = hostInfoMapping.get(id);
    return (nodeInfo == null) ? null : nodeInfo.getIpAddress();
  }

  /**
   * Returns the ping latency between a local namserver and a nameserver.
   *
   * @param id Nameserver id
   */
  public  double getPingLatency(int id) {
    HostInfo nodeInfo = hostInfoMapping.get(id);
    return (nodeInfo == null) ? -1 : nodeInfo.getPingLatency();
  }

  public  void updatePingLatency(int id, long responseTime) {
    HostInfo nodeInfo = hostInfoMapping.get(id);
    if (nodeInfo != null) {
      nodeInfo.updatePingLatency(responseTime);
    }
    //    return (nodeInfo == null) ? -1 : nodeInfo.getPingLatency();
  }

  /**
   * Tests *
   */
  public  void main(String[] args) throws Exception {
    //		ConfigFileInfo.readNameServerInfoLocal( "/Users/hardeep/Desktop/Workspace/GNRS/ns1" );
    //		System.out.println( ConfigFileInfo.nameServerInfoMapping.toString() );
    //		System.out.println( ConfigFileInfo.numberOfNameServer );
    //		System.out.println( ConfigFileInfo.closestNameServer );

    GNSNodeConfig GNSNodeConfig = new GNSNodeConfig("/Users/hardeep/Desktop/Workspace/PlanetlabScripts/src/Ping/name_server_ssh_local", 44);
    GNSNodeConfig GNSNodeConfig1 = new GNSNodeConfig("/Users/hardeep/Desktop/Workspace/PlanetlabScripts/src/Ping/name_server_ssh_local", 44);
    System.out.println(GNSNodeConfig.hostInfoMapping.toString());
    System.out.println(GNSNodeConfig.getNumberOfNameServers());
    System.out.println(GNSNodeConfig.getClosestNameServer() + "\t" + getPingLatency(getClosestNameServer()));

    Set<Integer> nameservers = new HashSet<Integer>();
    nameservers.add(1);
    nameservers.add(45);
    nameservers.add(48);
    nameservers.add(36);
    nameservers.add(59);
    Set<Integer> nameserverQueried = new HashSet<Integer>();
    nameserverQueried.add(8);
    System.out.println(BestServerSelection.getSmallestLatencyNS(nameservers, null));
  }

  @Override
  public boolean containsNodeInfo(int ID) {
    return ID < this.getNumberOfHosts();
  }

  @Override
  public int getNodeCount() {
    return this.getNumberOfHosts();
  }

  @Override
  public InetAddress getNodeAddress(int ID) {
    return this.getIPAddress(ID);
  }

  @Override
  public int getNodePort(int ID) {
    if (this.isNameServer(ID)) return  this.getNSTcpPort(ID);
    return this.getLNSTcpPort(ID);
  }
}