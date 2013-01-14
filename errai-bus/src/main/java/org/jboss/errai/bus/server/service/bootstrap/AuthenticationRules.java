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
package org.jboss.errai.bus.server.service.bootstrap;

import org.jboss.errai.bus.server.security.auth.rules.RolesRequiredRule;
import org.jboss.errai.bus.server.service.ErraiServiceConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;

/**
 * @author: Heiko Braun <hbraun@redhat.com>
 * @date: May 3, 2010
 */
class AuthenticationRules implements BootstrapExecution {
  private Logger log = LoggerFactory.getLogger(AuthenticationRules.class);

  public void execute(BootstrapContext context) {
    String requireAuthenticationForAll = "errai.require_authentication_for_all";

    final ErraiServiceConfigurator config = context.getConfig();

    if (config.hasProperty(requireAuthenticationForAll) && "true".equals(config.getProperty(requireAuthenticationForAll))) {
      log.debug("authentication for all requests required, adding rule ... ");
      context.getBus().addRule("AuthorizationService", new RolesRequiredRule(new HashSet<Object>(), context.getBus()));
    }
  }
}
