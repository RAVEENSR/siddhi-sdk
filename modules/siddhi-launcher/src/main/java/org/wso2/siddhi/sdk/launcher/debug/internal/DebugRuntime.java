/*
 * Copyright (c)  2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.siddhi.sdk.launcher.debug.internal;

import org.apache.log4j.Logger;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.core.debugger.SiddhiDebugger;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.sdk.launcher.debug.BreakPointInfo;
import org.wso2.siddhi.sdk.launcher.debug.VMDebugManager;
import org.wso2.siddhi.sdk.launcher.exception.InvalidExecutionStateException;
import org.wso2.siddhi.sdk.launcher.exception.NoSuchStreamException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The DebugRuntime which handles the siddhi debugger for each siddhi app.
 */
public class DebugRuntime {

    private static final Logger log = Logger.getLogger(DebugRuntime.class);

    private Mode mode = Mode.STOP;
    private transient String siddhiApp;
    private transient String siddhiAppFileName;
    private transient SiddhiAppRuntime siddhiAppRuntime;
    private transient SiddhiDebugger debugger;

    public DebugRuntime(String siddhiAppFileName, String siddhiApp) {
        this.siddhiApp = siddhiApp;
        this.siddhiAppFileName = siddhiAppFileName;
        createRuntime();
    }

    public SiddhiAppRuntime getSiddhiAppRuntime() {
        return siddhiAppRuntime;
    }

    private void setSiddhiAppRuntime(SiddhiAppRuntime siddhiAppRuntime) {
        this.siddhiAppRuntime = siddhiAppRuntime;
    }

    public String getSiddhiAppFileName() {
        return siddhiAppFileName;
    }

    public SiddhiDebugger getDebugger() {
        return debugger;
    }

    public void debug() {
        if (Mode.STOP.equals(mode)) {
            debugger = siddhiAppRuntime.debug();
            debugger.setDebuggerCallback((event, queryName, queryTerminal, debugger) -> {
                String[] queries = getQueries().toArray(new String[getQueries().size()]);
                int queryIndex = Arrays.asList(queries).indexOf(queryName);
                //Sending message to client on debug hit
                log.info("@Debug: Query: " + queryName + ", Terminal: " + queryTerminal + ", Event: " +
                        event);
                Map<String, Object> queryState = this.debugger.getQueryState(queryName);
                BreakPointInfo breakPointInfo = new BreakPointInfo(siddhiAppFileName, queryIndex, queryTerminal
                        .toString());
                breakPointInfo.setQueryState(queryState);
                breakPointInfo.setQueryName(queryName);
                breakPointInfo.setEventInfo(event);
                VMDebugManager.getInstance().getDebugSession().notifyHalt(breakPointInfo);
            });
            mode = Mode.DEBUG;
        } else if (Mode.FAULTY.equals(mode)) {
            throw new InvalidExecutionStateException("Siddhi App is in faulty state.");
        } else {
            throw new InvalidExecutionStateException("Siddhi App is already running.");
        }
    }

    public void stop() {
        if (debugger != null) {
            debugger.releaseAllBreakPoints();
            debugger.play();
            debugger = null;
        }
        if (siddhiAppRuntime != null) {
            siddhiAppRuntime.shutdown();
            siddhiAppRuntime = null;
        }
        createRuntime();
    }

    public List<String> getStreams() {
        if (!Mode.FAULTY.equals(mode)) {
            return new ArrayList<>(siddhiAppRuntime.getStreamDefinitionMap().keySet());
        } else {
            throw new InvalidExecutionStateException("Siddhi App is in faulty state.");
        }
    }

    public List<String> getQueries() {
        if (!Mode.FAULTY.equals(mode)) {
            return new ArrayList<>(siddhiAppRuntime.getQueryNames());
        } else {
            throw new InvalidExecutionStateException("Siddhi App is in faulty state.");
        }
    }

    public InputHandler getInputHandler(String streamName) {
        if (!Mode.FAULTY.equals(mode)) {
            return siddhiAppRuntime.getInputHandler(streamName);
        } else {
            throw new InvalidExecutionStateException("Siddhi App is in faulty state.");
        }
    }

    public List<Attribute> getStreamAttributes(String streamName) {
        if (!Mode.FAULTY.equals(mode)) {
            if (siddhiAppRuntime.getStreamDefinitionMap().containsKey(streamName)) {
                return siddhiAppRuntime.getStreamDefinitionMap().get(streamName).getAttributeList();
            } else {
                throw new NoSuchStreamException(
                        "Stream definition %s does not exists in Siddhi app " + streamName);
            }
        } else {
            throw new InvalidExecutionStateException("Siddhi App is in faulty state.");
        }
    }

    private void createRuntime() {
        if (siddhiApp != null && !siddhiApp.isEmpty()) {
            siddhiAppRuntime = VMDebugManager.getInstance().getSiddhiManager()
                    .createSiddhiAppRuntime(siddhiApp);
            this.setSiddhiAppRuntime(siddhiAppRuntime);
            mode = Mode.STOP;
        } else {
            mode = Mode.FAULTY;
        }
    }

    private enum Mode { DEBUG, STOP, FAULTY}
}
