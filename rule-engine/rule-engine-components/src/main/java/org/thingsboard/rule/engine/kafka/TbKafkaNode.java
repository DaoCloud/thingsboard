/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "kafka",
        configClazz = TbKafkaNodeConfiguration.class,
        nodeDescription = "Publish messages to Kafka server",
        nodeDetails = "Expects messages with any message type. Will send record via Kafka producer to Kafka server.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeKafkaConfig",
        iconUrl = "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTUzOCIgaGVpZ2h0PSIyNTAwIiB2aWV3Qm94PSIwIDAgMjU2IDQxNiIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiBwcmVzZXJ2ZUFzcGVjdFJhdGlvPSJ4TWlkWU1pZCI+PHBhdGggZD0iTTIwMS44MTYgMjMwLjIxNmMtMTYuMTg2IDAtMzAuNjk3IDcuMTcxLTQwLjYzNCAxOC40NjFsLTI1LjQ2My0xOC4wMjZjMi43MDMtNy40NDIgNC4yNTUtMTUuNDMzIDQuMjU1LTIzLjc5NyAwLTguMjE5LTEuNDk4LTE2LjA3Ni00LjExMi0yMy40MDhsMjUuNDA2LTE3LjgzNWM5LjkzNiAxMS4yMzMgMjQuNDA5IDE4LjM2NSA0MC41NDggMTguMzY1IDI5Ljg3NSAwIDU0LjE4NC0yNC4zMDUgNTQuMTg0LTU0LjE4NCAwLTI5Ljg3OS0yNC4zMDktNTQuMTg0LTU0LjE4NC01NC4xODQtMjkuODc1IDAtNTQuMTg0IDI0LjMwNS01NC4xODQgNTQuMTg0IDAgNS4zNDguODA4IDEwLjUwNSAyLjI1OCAxNS4zODlsLTI1LjQyMyAxNy44NDRjLTEwLjYyLTEzLjE3NS0yNS45MTEtMjIuMzc0LTQzLjMzMy0yNS4xODJ2LTMwLjY0YzI0LjU0NC01LjE1NSA0My4wMzctMjYuOTYyIDQzLjAzNy01My4wMTlDMTI0LjE3MSAyNC4zMDUgOTkuODYyIDAgNjkuOTg3IDAgNDAuMTEyIDAgMTUuODAzIDI0LjMwNSAxNS44MDMgNTQuMTg0YzAgMjUuNzA4IDE4LjAxNCA0Ny4yNDYgNDIuMDY3IDUyLjc2OXYzMS4wMzhDMjUuMDQ0IDE0My43NTMgMCAxNzIuNDAxIDAgMjA2Ljg1NGMwIDM0LjYyMSAyNS4yOTIgNjMuMzc0IDU4LjM1NSA2OC45NHYzMi43NzRjLTI0LjI5OSA1LjM0MS00Mi41NTIgMjcuMDExLTQyLjU1MiA1Mi44OTQgMCAyOS44NzkgMjQuMzA5IDU0LjE4NCA1NC4xODQgNTQuMTg0IDI5Ljg3NSAwIDU0LjE4NC0yNC4zMDUgNTQuMTg0LTU0LjE4NCAwLTI1Ljg4My0xOC4yNTMtNDcuNTUzLTQyLjU1Mi01Mi44OTR2LTMyLjc3NWE2OS45NjUgNjkuOTY1IDAgMCAwIDQyLjYtMjQuNzc2bDI1LjYzMyAxOC4xNDNjLTEuNDIzIDQuODQtMi4yMiA5Ljk0Ni0yLjIyIDE1LjI0IDAgMjkuODc5IDI0LjMwOSA1NC4xODQgNTQuMTg0IDU0LjE4NCAyOS44NzUgMCA1NC4xODQtMjQuMzA1IDU0LjE4NC01NC4xODQgMC0yOS44NzktMjQuMzA5LTU0LjE4NC01NC4xODQtNTQuMTg0em0wLTEyNi42OTVjMTQuNDg3IDAgMjYuMjcgMTEuNzg4IDI2LjI3IDI2LjI3MXMtMTEuNzgzIDI2LjI3LTI2LjI3IDI2LjI3LTI2LjI3LTExLjc4Ny0yNi4yNy0yNi4yN2MwLTE0LjQ4MyAxMS43ODMtMjYuMjcxIDI2LjI3LTI2LjI3MXptLTE1OC4xLTQ5LjMzN2MwLTE0LjQ4MyAxMS43ODQtMjYuMjcgMjYuMjcxLTI2LjI3czI2LjI3IDExLjc4NyAyNi4yNyAyNi4yN2MwIDE0LjQ4My0xMS43ODMgMjYuMjctMjYuMjcgMjYuMjdzLTI2LjI3MS0xMS43ODctMjYuMjcxLTI2LjI3em01Mi41NDEgMzA3LjI3OGMwIDE0LjQ4My0xMS43ODMgMjYuMjctMjYuMjcgMjYuMjdzLTI2LjI3MS0xMS43ODctMjYuMjcxLTI2LjI3YzAtMTQuNDgzIDExLjc4NC0yNi4yNyAyNi4yNzEtMjYuMjdzMjYuMjcgMTEuNzg3IDI2LjI3IDI2LjI3em0tMjYuMjcyLTExNy45N2MtMjAuMjA1IDAtMzYuNjQyLTE2LjQzNC0zNi42NDItMzYuNjM4IDAtMjAuMjA1IDE2LjQzNy0zNi42NDIgMzYuNjQyLTM2LjY0MiAyMC4yMDQgMCAzNi42NDEgMTYuNDM3IDM2LjY0MSAzNi42NDIgMCAyMC4yMDQtMTYuNDM3IDM2LjYzOC0zNi42NDEgMzYuNjM4em0xMzEuODMxIDY3LjE3OWMtMTQuNDg3IDAtMjYuMjctMTEuNzg4LTI2LjI3LTI2LjI3MXMxMS43ODMtMjYuMjcgMjYuMjctMjYuMjcgMjYuMjcgMTEuNzg3IDI2LjI3IDI2LjI3YzAgMTQuNDgzLTExLjc4MyAyNi4yNzEtMjYuMjcgMjYuMjcxeiIvPjwvc3ZnPg=="
)
public class TbKafkaNode implements TbNode {

    private static final String OFFSET = "offset";
    private static final String PARTITION = "partition";
    private static final String TOPIC = "topic";
    private static final String ERROR = "error";

    private TbKafkaNodeConfiguration config;

    private Producer<?, String> producer;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbKafkaNodeConfiguration.class);
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, config.getValueSerializer());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, config.getKeySerializer());
        properties.put(ProducerConfig.ACKS_CONFIG, config.getAcks());
        properties.put(ProducerConfig.RETRIES_CONFIG, config.getRetries());
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, config.getBatchSize());
        properties.put(ProducerConfig.LINGER_MS_CONFIG, config.getLinger());
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, config.getBufferMemory());
        if (config.getOtherProperties() != null) {
            config.getOtherProperties()
                    .forEach((k,v) -> properties.put(k, v));
        }
        try {
            this.producer = new KafkaProducer<>(properties);
        } catch (Exception e) {
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        String topic = TbNodeUtils.processPattern(config.getTopicPattern(), msg.getMetaData());
        try {
            producer.send(new ProducerRecord<>(topic, msg.getData()),
                    (metadata, e) -> {
                        if (metadata != null) {
                            TbMsg next = processResponse(ctx, msg, metadata);
                            ctx.tellNext(next, TbRelationTypes.SUCCESS);
                        } else {
                            TbMsg next = processException(ctx, msg, e);
                            ctx.tellNext(next, TbRelationTypes.FAILURE, e);
                        }
                    });
        } catch (Exception e) {
            ctx.tellError(msg, e);
        }
    }

    @Override
    public void destroy() {
        if (this.producer != null) {
            try {
                this.producer.close();
            } catch (Exception e) {
                log.error("Failed to close producer during destroy()", e);
            }
        }
    }

    private TbMsg processResponse(TbContext ctx, TbMsg origMsg, RecordMetadata recordMetadata) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(OFFSET, String.valueOf(recordMetadata.offset()));
        metaData.putValue(PARTITION, String.valueOf(recordMetadata.partition()));
        metaData.putValue(TOPIC, recordMetadata.topic());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

    private TbMsg processException(TbContext ctx, TbMsg origMsg, Exception e) {
        TbMsgMetaData metaData = origMsg.getMetaData().copy();
        metaData.putValue(ERROR, e.getClass() + ": " + e.getMessage());
        return ctx.transformMsg(origMsg, origMsg.getType(), origMsg.getOriginator(), metaData, origMsg.getData());
    }

}