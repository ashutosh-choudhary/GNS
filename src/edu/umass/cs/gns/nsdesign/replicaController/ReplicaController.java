package edu.umass.cs.gns.nsdesign.replicaController;

import edu.umass.cs.gns.database.MongoRecords;
import edu.umass.cs.gns.main.GNS;
import edu.umass.cs.gns.nameserver.recordmap.BasicRecordMap;
import edu.umass.cs.gns.nio.GNSNIOTransport;
import edu.umass.cs.gns.nsdesign.GNSNodeConfig;
import edu.umass.cs.gns.packet.AddRecordPacket;
import edu.umass.cs.gns.packet.Packet;
import edu.umass.cs.gns.packet.RemoveRecordPacket;
import edu.umass.cs.gns.packet.RequestActivesPacket;
import edu.umass.cs.gns.replicaCoordination.ReplicaControllerCoordinator;
import edu.umass.cs.gns.replicaCoordination.ReplicaControllerPaxos;
import edu.umass.cs.gns.util.Util;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ScheduledThreadPoolExecutor;

/*** DONT not use any class in package edu.umass.cs.gns.nsdesign ***/

/**
 * Work in progress. Inactive code.
 *
 * Class implements all functionality of a replica controller.
 * We keep a single instance of this class for all names for whom this name server is a replica controller.
 * Created by abhigyan on 2/26/14.
 */
public class ReplicaController implements  ReplicaControllerInterface{

  /** object handles coordination among replicas on a request, if necessary */
  private ReplicaControllerCoordinator rcCoordinator;

  /**ID of this node*/
  private int nodeID;

  /** nio server*/
  private GNSNIOTransport nioServer;

  /** executor service for handling timer tasks*/
  private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;


  /** Object provides interface to the database table storing replica controller records */
  private BasicRecordMap replicaControllerDB;

  private GNSNodeConfig gnsNodeConfig;
  /**
   * constructor object
   */
  public ReplicaController(int nodeID, String configFile, GNSNodeConfig gnsNodeConfig, GNSNIOTransport nioServer,
                           ScheduledThreadPoolExecutor scheduledThreadPoolExecutor) {
    this.nodeID = nodeID;

    this.nioServer = nioServer;

    this.scheduledThreadPoolExecutor = scheduledThreadPoolExecutor;

    String className = "edu.umass.cs.gns.nameserver.recordmap.MongoRecordMap";
    replicaControllerDB = (BasicRecordMap) Util.createObject(className,
            // probably should use something more generic here
            MongoRecords.DBREPLICACONTROLLER);

    // create the activeCoordinator object.
    rcCoordinator = new ReplicaControllerPaxos(nioServer, new edu.umass.cs.gns.nameserver.GNSNodeConfig(), this);
    this.gnsNodeConfig = gnsNodeConfig;

  }


  /**
   * Entry point for all packets sent to replica controller.
   *
   * We are currently implementing a single name server which wont use coordination at all.
   * Only those packet types are handled that will be used by a single name server.
   * @param json json object received at name server
   */
  public void handleIncomingPacket(JSONObject json){

    try {
      Packet.PacketType type = Packet.getPacketType(json);
      switch (type) {

        /** Packets sent from LNS **/
        case ADD_RECORD_LNS:  // add name to GNS
          if(rcCoordinator == null) {
            Add.executeAddRecord(new AddRecordPacket(json), this);
          } else {
            rcCoordinator.handleRequest(json);
          }
          break;
        case REQUEST_ACTIVES:  // lookup actives for name
          if(rcCoordinator == null) {
            LookupActives.executeLookupActives(new RequestActivesPacket(json), this);
          } else {
            rcCoordinator.handleRequest(json);
          }
          break;
        case REMOVE_RECORD_LNS: // remove name from GNS
          if(rcCoordinator == null) {
            Remove.executeRemoveRecord(new RemoveRecordPacket(json), this);
          } else {
            rcCoordinator.handleRequest(json);
          }
          break;
        case NAMESERVER_SELECTION: // stats reported from local name servers
          // we don't expect to use coordination for this packet
          // in future also, we don't expect to use coordination for this packet
          break;

        /**  Packets sent from active replica **/
        case ACTIVE_ADD_CONFIRM:   // confirmation received from active replica that name is added
          Add.executeAddActiveConfirm(new AddRecordPacket(json), this);
          break;
        case ACTIVE_REMOVE_CONFIRM:  // confirmation received from active replica that name is removed
          if(rcCoordinator == null) {
            Remove.executeRemoveActiveConfirm(new RemoveRecordPacket(json), this);
          } else {
            rcCoordinator.handleRequest(json);
          }
          break;
        case ACTIVE_GROUPCHANGE_CONFIRM:  // confirmation received from active replica that group change for a name is complete
          // not implemented yet, we wont be doing group changes with one name server
          break;
        case NAME_RECORD_STATS_RESPONSE: // stats reported from active replicas
          // not implemented yet, we wont be reporting stats with one name server
          // in future also, we don't expect to use coordination for this packet
          break;

        /** packets from coordination modules at replica controller **/
        case REPLICA_CONTROLLER_COORDINATION:
          rcCoordinator.handleRequest(json);
          break;
        default:
          GNS.getLogger().warning("No handler for packet type: " + type.toString());
          break;
      }
    } catch (JSONException e) {
      e.printStackTrace();
    }

  }


  /**
   * ReplicaControllerCoordinator calls this method to locally execute a decision.
   * Depending on packet type, it call other methods in ReplicaController package to execute request.
   */
  public void executeRequestLocal(JSONObject json) {

  }

}