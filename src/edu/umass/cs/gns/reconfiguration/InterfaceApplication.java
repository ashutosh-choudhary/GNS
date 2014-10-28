package edu.umass.cs.gns.reconfiguration;

import java.util.Set;

import edu.umass.cs.gns.nio.IntegerPacketType;

/**
@author V. Arun
 */
public interface InterfaceApplication {
	public boolean handleRequest(InterfaceRequest request);

	/* App must support string<-->InterfaceRequest conversion and back. Furthermore,
	 * the conversion to a string and back must preserve the return values of all
	 * InterfaceRequest methods, i.e., 
	 * (app.getRequest(request.toString())).getRequestType = request.getRequestType()
	 * ... and so on
	 */
	public InterfaceRequest getRequest(String stringified) throws RequestParseException;
	
	public Set<IntegerPacketType> getRequestTypes();
}