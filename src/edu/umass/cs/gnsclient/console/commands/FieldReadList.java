/*
 *
 *  Copyright (c) 2015 University of Massachusetts
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 *  Initial developer(s): Westy, Emmanuel Cecchet
 *
 */
package edu.umass.cs.gnsclient.console.commands;

import java.util.StringTokenizer;

import org.json.JSONArray;

import edu.umass.cs.gnsclient.client.GNSClientCommands;
import edu.umass.cs.gnsclient.console.ConsoleModule;
import edu.umass.cs.gnscommon.exceptions.client.ClientException;
import edu.umass.cs.gnscommon.utils.StringUtil;
import java.io.IOException;

/**
 * Reads a field in the GNS
 */
public class FieldReadList extends ConsoleCommand {

  /**
   * Creates a new <code>FieldReadList</code> object
   *
   * @param module
   */
  public FieldReadList(ConsoleModule module) {
    super(module);
  }

  @Override
  public String getCommandDescription() {
    return "Read all the values of the given field in the target GUID (using the credential of the current GUID/alias)."
            + " Assumes the field is a list. Use in conjunction with field_write_list.";
  }

  @Override
  public String getCommandName() {
    return "field_read_list";
  }

  @Override
  public String getCommandParameters() {
    return "[target_guid_or_alias] field_to_read";
  }

  /**
   * Override execute to check for existing connectivity
   *
   * @throws java.lang.Exception
   */
  @Override
  public void execute(String commandText) throws Exception {
    if (!module.isCurrentGuidSetAndVerified()) {
      return;
    }
    super.execute(commandText);
  }

  @Override
  public void parse(String commandText) throws Exception {
    try {
      GNSClientCommands gnsClient = module.getGnsClient();

      StringTokenizer st = new StringTokenizer(commandText.trim());
      String guid;
      switch (st.countTokens()) {
        case 1:
          guid = module.getCurrentGuid().getGuid();
          break;
        case 2:
          guid = st.nextToken();
          if (!StringUtil.isValidGuidString(guid)) {
            // We probably have an alias, lookup the GUID
            guid = gnsClient.lookupGuid(guid);
          } break;
        default:
          wrongArguments();
          return;
      }

      String field = st.nextToken();

      JSONArray value = gnsClient.fieldReadArray(guid, field, module.getCurrentGuid());
      console.printString(value.toString());
      console.printNewline();
    } catch (IOException | ClientException e) {
      console.printString("Failed to access GNS ( " + e + ")\n");
    }
  }
}
