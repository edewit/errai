/*
 * Copyright 2013 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.bus.client.framework;

import org.jboss.errai.bus.client.api.messaging.MessageBus;
import org.jboss.errai.bus.client.api.builder.RemoteCallSendable;
import org.jboss.errai.common.client.api.ErrorCallback;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.common.client.framework.RpcBatch;
import org.jboss.errai.common.client.framework.RpcStub;

import java.lang.annotation.Annotation;

/**
 * Base class of all RPC proxies managed by the shared {@see RemoteServiceProxyFactory}. The concrete implementations
 * of this class are generated at compile time.
 * 
 * @author Christian Sadilek <csadilek@redhat.com>
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractRpcProxy implements RpcStub {

  protected RemoteCallback remoteCallback;
  protected ErrorCallback errorCallback;
  protected Annotation[] qualifiers;
  protected RpcBatch batch;

  @Override
  public void setErrorCallback(ErrorCallback errorCallback) {
    this.errorCallback = errorCallback;
  }

  @Override
  public void setRemoteCallback(RemoteCallback remoteCallback) {
    this.remoteCallback = remoteCallback;
  }

  @Override
  public void setQualifiers(Annotation[] qualifiers) {
    this.qualifiers = qualifiers;
  }

  @Override
  public void setBatch(RpcBatch batch) {
    this.batch = batch;
  }

  @SuppressWarnings("unchecked")
  public void sendRequest(final MessageBus bus, final RemoteCallSendable sendable) {
    if (batch != null) {
      batch.addRequest(sendable);
    }
    else {
      sendable.sendNowWith(bus);
    }
  }
}
