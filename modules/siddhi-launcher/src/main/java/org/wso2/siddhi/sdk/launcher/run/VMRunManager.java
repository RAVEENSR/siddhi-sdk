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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.Channel;
import org.apache.log4j.Logger;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.sdk.launcher.exception.RunException;
import org.wso2.siddhi.sdk.launcher.run.dto.CommandDTO;
import org.wso2.siddhi.sdk.launcher.run.dto.MessageDTO;
import org.wso2.siddhi.sdk.launcher.server.VMRunDebugServer;
import org.wso2.siddhi.sdk.launcher.util.InputFeeder;

import java.io.IOException;

/**
 * {@code VMRunManager} Manages run sessions and handle run related actions.
 */
public class VMRunManager {

    private static final Logger log = Logger.getLogger(VMRunManager.class);

    private static VMRunManager runManagerInstance = null;
    private static SiddhiManager siddhiManager = new SiddhiManager();
    private VMRunDebugServer runServer;

    /**
     * Object to hold run session related context.
     */
    private VMRunSession runSession;
    private boolean runManagerInitialized = false;
    private InputFeeder inputFeeder = null;

    /**
     * Instantiates a new Run manager.
     */
    private VMRunManager() {
        runServer = new VMRunDebugServer();
        runServer.setDebugMode(false);
        runSession = new VMRunSession();
    }

    /**
     * Run manager singleton.
     *
     * @return RunManager instance
     */
    public static VMRunManager getInstance() {
        if (runManagerInstance != null) {
            return runManagerInstance;
        }
        return initialize();
    }

    private static synchronized VMRunManager initialize() {
        if (runManagerInstance == null) {
            runManagerInstance = new VMRunManager();
        }
        return runManagerInstance;
    }

    public VMRunSession getRunSession() {
        return runSession;
    }

    public SiddhiManager getSiddhiManager() {
        return siddhiManager;
    }

    /**
     * Initializes the run manager single instance.
     */
    public void mainInit(String siddhiAppPath, String siddhiApp, String inputFile) {
        if (runManagerInitialized) {
            throw new RunException("Runner instance already initialized");
        }
//        File f = new File(siddhiAppPath);
//        String fileName = f.getName();
        //Generating runtime
        SiddhiAppRuntime siddhiAppRuntime = siddhiManager.createSiddhiAppRuntime(siddhiApp);
        runSession.setsiddhiAppRuntime(siddhiAppRuntime);
        if (!(inputFile == null || inputFile.equalsIgnoreCase(""))) {
            inputFeeder = new InputFeeder(siddhiAppRuntime, inputFile);
        }
        // start the runner server if it is not started yet.
        runServer.startServer();
        runManagerInitialized = true;
        //Starting event processing
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        siddhiAppRuntime.start();
        if (inputFeeder != null) {
            inputFeeder.start();
            //sendAcknowledge(runSession, "Input feeder started.");
        } else {
            log.info("Input file is empty or null");
        }
    }

    /**
     * Process runner command.
     *
     * @param json the json
     */
    public void processRunCommand(String json) {
        try {
            processCommand(json);
        } catch (Exception e) {
            MessageDTO message = new MessageDTO();
            message.setCode(RunConstants.CODE_INVALID);
            message.setMessage(e.getMessage());
            runServer.pushMessageToClient(runSession, message);
        }
    }

    private void processCommand(String json) {
        ObjectMapper mapper = new ObjectMapper();
        CommandDTO command = null;
        try {
            command = mapper.readValue(json, CommandDTO.class);
        } catch (IOException e) {
            //invalid message will be passed
            throw new RunException(RunConstants.MSG_INVALID);
        }
        switch (command.getCommand()) {
            case RunConstants.CMD_STOP:
                if (inputFeeder != null) {
                    inputFeeder.stop();
                }
                runSession.stopSiddhiApp();
                runSession.clearSession();
                break;
//            case RunConstants.CMD_SEND_EVENT:
//                if (inputFeeder != null) {
//                    inputFeeder.start();
//                    sendAcknowledge(runSession, "Input feeder started.");
//                } else {
//                    log.info("Input file is empty or null");
//                }
//                break;
            default:
                throw new RunException(RunConstants.MSG_INVALID);
        }
    }

    /**
     * Set run channel.
     *
     * @param channel the channel
     */
    public void addRunSession(Channel channel) throws RunException {
        runSession.setChannel(channel);
        sendAcknowledge(runSession, "Channel registered.");
    }

    private boolean isRunSessionActive() {
        return (runSession.getChannel() != null);
    }

    /**
     * Notify client when run has finish execution.
     *
     * @param runSession current running session
     */
    public void notifyComplete(VMRunSession runSession) {
        MessageDTO message = new MessageDTO();
        message.setCode(RunConstants.CODE_COMPLETE);
        message.setMessage(RunConstants.MSG_COMPLETE);
        runServer.pushMessageToClient(runSession, message);
    }

    /**
     * Notify client when the runner is exiting.
     *
     * @param runSession current running session
     */
    public void notifyExit(VMRunSession runSession) {
        if (!isRunSessionActive()) {
            return;
        }
        MessageDTO message = new MessageDTO();
        message.setCode(RunConstants.CODE_EXIT);
        message.setMessage(RunConstants.MSG_EXIT);
        runServer.pushMessageToClient(runSession, message);
    }

    /**
     * Send a generic acknowledge message to the client.
     *
     * @param runSession current running session
     * @param messageText  message to send to the client
     */
    private void sendAcknowledge(VMRunSession runSession, String messageText) {
        MessageDTO message = new MessageDTO();
        message.setCode(RunConstants.CODE_ACK);
        message.setMessage(messageText);
        runServer.pushMessageToClient(runSession, message);
    }
}
