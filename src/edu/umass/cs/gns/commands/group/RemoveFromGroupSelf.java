/*
 * Copyright (C) 2014
 * University of Massachusetts
 * All Rights Reserved 
 *
 * Initial developer(s): Westy.
 */
package edu.umass.cs.gns.commands.group;

import edu.umass.cs.gns.commands.CommandModule;
import static edu.umass.cs.gns.clientprotocol.Defs.*;

/**
 *
 * @author westy
 */
public class RemoveFromGroupSelf extends RemoveFromGroup {

  public RemoveFromGroupSelf(CommandModule module) {
    super(module);
  }

  @Override
  public String[] getCommandParameters() {
    return new String[]{GUID, MEMBER, SIGNATURE, SIGNATUREFULLMESSAGE};
  }

  @Override
  public String getCommandName() {
    return REMOVEFROMGROUP;
  }

  @Override
  public String getCommandDescription() {
    return "Removes the member guid from the group specified by guid.";
  }
}