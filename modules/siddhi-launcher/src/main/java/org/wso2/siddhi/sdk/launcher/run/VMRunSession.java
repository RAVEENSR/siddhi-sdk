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

import io.netty.channel.Channel;
import org.wso2.siddhi.core.SiddhiAppRuntime;
import org.wso2.siddhi.sdk.launcher.exception.RunException;


/**
 * {@code VMRunSession} The Run Session class will be used to hold context for each client.
 * Each client will get its own instance of Run session.
 */
public class VMRunSession {

    private Channel channel = null;

    private SiddhiAppRuntime siddhiAppRuntime = null;

    public SiddhiAppRuntime getSiddhiSAppRuntime() {
        return siddhiAppRuntime;
    }

    public void setsiddhiAppRuntime(SiddhiAppRuntime siddhiAppRuntime) {
        this.siddhiAppRuntime = siddhiAppRuntime;
    }

    /**
     * Gets channel.
     *
     * @return the channel
     */
    public synchronized Channel getChannel() {
        return channel;
    }

    public synchronized void setChannel(Channel channel) throws RunException {
        if (this.channel != null) {
            throw new RunException("Run session already exist");
        }
        this.channel = channel;
    }

    /**
     * Method to start running process in all the threads.
     */
    public void startSiddhiApp() {
        siddhiAppRuntime.start();
    }

    /**
     * Method to stop running process in all the threads.
     */
    public void stopSiddhiApp() {
        siddhiAppRuntime.shutdown();
    }

    /**
     * Method to clear the channel so that another run session can connect.
     */
    public synchronized void clearSession() {
        this.channel.close();
        this.channel = null;
    }

    public void notifyComplete() {
        VMRunManager runManager = VMRunManager.getInstance();
        runManager.notifyComplete(this);
    }

    public void notifyExit() {
        VMRunManager runManager = VMRunManager.getInstance();
        runManager.notifyExit(this);
    }
}

