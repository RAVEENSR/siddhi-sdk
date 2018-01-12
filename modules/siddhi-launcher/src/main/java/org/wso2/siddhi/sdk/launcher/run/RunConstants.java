/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/

package org.wso2.siddhi.sdk.launcher.run;

/**
 * {@code DebugConstants} define debugger constants.
 */
public class RunConstants {

    // commands sent by client
    public static final String CMD_START = "START";
    public static final String CMD_STOP = "STOP";

    // messages sent back to client
    public static final String CODE_INVALID = "INVALID";
    public static final String MSG_INVALID = "Invalid Command";
    public static final String CODE_ACK = "ACK";
    public static final String CODE_COMPLETE = "COMPLETE";
    public static final String MSG_COMPLETE = "Run session completed.";
    public static final String CODE_EXIT = "EXIT";
    public static final String MSG_EXIT = "Exiting from runner.";

    //startup message.
    public static final String RUN_MESSAGE = "Siddhi runner server is activated on port : ";
    public static final String ERROR_JSON = "{ \"error\": true }";
    public static final String RUN_SERVER_ERROR = "Runner Server Error. Closing client connection.";
    public static final String CMD_SEND_EVENT = "SEND_EVENT";

    //default run port where websocket server will listen
    public static final String DEFAULT_RUN_PORT = "5006";

    //runner web-socket path.
    public static final String RUN_WEBSOCKET_PATH = "/run";
}
