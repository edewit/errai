/*
 * Copyright 2012 JBoss, by Red Hat, Inc
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

package org.jboss.errai.bus.server.servlet;

import static org.jboss.errai.bus.server.io.MessageFactory.createCommandMessage;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.errai.bus.client.api.QueueSession;
import org.jboss.errai.bus.client.api.base.DefaultErrorCallback;
import org.jboss.errai.bus.client.framework.ClientMessageBus;
import org.jboss.errai.bus.client.framework.MarshalledMessage;
import org.jboss.errai.bus.server.api.MessageQueue;
import org.jboss.errai.bus.server.api.QueueActivationCallback;
import org.jboss.errai.bus.server.io.OutputStreamWriteAdapter;
import org.mortbay.jetty.RetryRequest;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

/**
 * The <tt>JettyContinuationsServlet</tt> provides the HTTP-protocol gateway between the server bus and the client buses,
 * using Jetty Continuations.
 */
public class JettyContinuationsServlet extends AbstractErraiServlet {
  /**
   * Called by the server (via the <tt>service</tt> method) to allow a servlet to handle a GET request by supplying
   * a response
   *
   * @param httpServletRequest  - object that contains the request the client has made of the servlet
   * @param httpServletResponse - object that contains the response the servlet sends to the client
   * @throws IOException      - if an input or output error is detected when the servlet handles the GET request
   * @throws ServletException - if the request for the GET could not be handled
   */
  @Override
  protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
          throws ServletException, IOException {
    pollForMessages(sessionProvider.createOrGetSession(httpServletRequest.getSession(),
            httpServletRequest.getHeader(ClientMessageBus.REMOTE_QUEUE_ID_HEADER)),
            httpServletRequest, httpServletResponse, true);
  }

  /**
   * Called by the server (via the <code>service</code> method) to allow a servlet to handle a POST request, by
   * sending the request.
   *
   * @param httpServletRequest  - object that contains the request the client has made of the servlet
   * @param httpServletResponse - object that contains the response the servlet sends to the client
   * @throws IOException      - if an input or output error is detected when the servlet handles the request
   * @throws ServletException - if the request for the POST could not be handled
   */
  @Override
  protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
          throws ServletException, IOException {

    final QueueSession session = sessionProvider.createOrGetSession(httpServletRequest.getSession(),
            httpServletRequest.getHeader(ClientMessageBus.REMOTE_QUEUE_ID_HEADER));

    try {
      service.store(createCommandMessage(session, httpServletRequest));
    }
    catch (Exception e) {
      if (!e.getMessage().contains("expired")) {
        writeExceptionToOutputStream(httpServletResponse, e);
      }
    }
  }

  private void pollForMessages(QueueSession session, HttpServletRequest httpServletRequest,
                               HttpServletResponse httpServletResponse, boolean wait) throws IOException {
    final OutputStream stream = httpServletResponse.getOutputStream();

    try {
      final MessageQueue queue = service.getBus().getQueue(session);
      if (queue == null) {
        switch (getConnectionPhase(httpServletRequest)) {
          case CONNECTING:
          case DISCONNECTING:
            return;
        }

        sendDisconnectDueToSessionExpiry(httpServletResponse.getOutputStream());

        return;
      }
      queue.heartBeat();

      if (wait) {
        synchronized (queue.getActivationLock()) {
          final Continuation cont = ContinuationSupport.getContinuation(httpServletRequest, queue);

          if (!cont.isResumed() && !queue.messagesWaiting()) {
            queue.setActivationCallback(new JettyQueueActivationCallback(cont));
            if (cont.suspend(30 * 1000)) {
              return;
            }
          }
        }
      }

      pollQueue(queue, stream, httpServletResponse);
    }
    catch (RetryRequest r) {
      /**
       * This *must* be caught and re-thrown to work property with Jetty.
       */

      throw r;
    }
    catch (final Throwable t) {
      t.printStackTrace();

      httpServletResponse.setHeader("Cache-Control", "no-cache");
      httpServletResponse.setHeader("Pragma", "no-cache");
      httpServletResponse.setHeader("Expires", "-1");
      httpServletResponse.addHeader("Payload-Size", "1");
      httpServletResponse.setContentType("application/json");

      stream.write('[');

      writeToOutputStream(stream, new MarshalledMessage() {
        @Override
        public String getSubject() {
          return DefaultErrorCallback.CLIENT_ERROR_SUBJECT;
        }

        @Override
        public Object getMessage() {
          StringBuilder b = new StringBuilder("{Error" +
                  "Message:\"").append(t.getMessage()).append("\",AdditionalDetails:\"");
          for (StackTraceElement e : t.getStackTrace()) {
            b.append(e.toString()).append("<br/>");
          }

          return b.append("\"}").toString();
        }
      });

      stream.write(']');

    }
  }

  private static boolean pollQueue(MessageQueue queue, OutputStream stream,
                                   HttpServletResponse httpServletResponse) throws IOException {
    if (queue == null) return false;
    queue.heartBeat();

    httpServletResponse.setHeader("Cache-Control", "no-cache");
    httpServletResponse.setHeader("Pragma", "no-cache");
    httpServletResponse.setHeader("Expires", "-1");
    httpServletResponse.setContentType("application/json");
    return queue.poll(false, new OutputStreamWriteAdapter(stream));
  }

  private static class JettyQueueActivationCallback implements QueueActivationCallback {
    private final Continuation cont;

    private JettyQueueActivationCallback(Continuation cont) {
      this.cont = cont;
    }

    @Override
    public void activate(MessageQueue queue) {
      queue.setActivationCallback(null);
      cont.resume();
    }
  }
}
