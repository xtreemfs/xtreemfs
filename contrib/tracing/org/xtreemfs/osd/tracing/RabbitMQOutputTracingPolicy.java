/*
 * Copyright (c) 2008-2015 by Christoph Kleineweber,
 *               Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

package org.xtreemfs.osd.tracing;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.xtreemfs.foundation.logging.Logging;
import org.xtreemfs.osd.OSDRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Christoph Kleineweber <kleineweber@zib.de>
 */
public class RabbitMQOutputTracingPolicy implements TracingPolicy {
    public static final short   POLICY_ID = 6003;

    private static final String QUEUE_NAME = "xtreemfs-trace";
    private static Map<String, Channel> channels = null;

    @Override
    public void traceRequest(OSDRequest req, TraceInfo traceInfo) {
        try {
            Channel channel = getChannel(traceInfo.getPolicyConfig());
            channel.basicPublish("", QUEUE_NAME, null, traceInfo.toString().getBytes());
        } catch (Exception ex) {
            Logging.logError(Logging.LEVEL_ERROR, this, ex);
        }
    }

    private Channel getChannel(String target) throws Exception {
        if(this.channels == null) {
           this.channels = new HashMap<String, Channel>();
        }

        if (this.channels.containsKey(target)) {
            return this.channels.get(target);
        } else {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(target);
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            if(channel != null) {
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                this.channels.put(target, channel);
                return channel;
            } else {
                throw new Exception("Cannot create RabbitMQ channel");
            }

        }
    }
}
