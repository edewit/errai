/*
 * Copyright 2011 JBoss, by Red Hat, Inc
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

package org.jboss.errai.codegen;

import static org.jboss.errai.codegen.util.Stmt.loadVariable;
import static org.jboss.errai.codegen.util.Stmt.throw_;

import org.jboss.errai.codegen.builder.BlockBuilder;
import org.jboss.errai.codegen.builder.ClassStructureBuilder;
import org.jboss.errai.codegen.builder.impl.ClassBuilder;
import org.jboss.errai.codegen.exception.UnproxyableClassException;
import org.jboss.errai.codegen.meta.MetaClass;
import org.jboss.errai.codegen.meta.MetaClassFactory;
import org.jboss.errai.codegen.meta.MetaMethod;
import org.jboss.errai.codegen.meta.MetaParameter;
import org.jboss.errai.codegen.meta.MetaParameterizedType;
import org.jboss.errai.codegen.meta.MetaType;
import org.jboss.errai.codegen.meta.MetaTypeVariable;
import org.jboss.errai.codegen.meta.impl.build.BuildMetaClass;
import org.jboss.errai.codegen.util.GenUtil;
import org.jboss.errai.codegen.util.If;
import org.jboss.errai.codegen.util.PrivateAccessUtil;
import org.jboss.errai.codegen.util.Refs;
import org.jboss.errai.codegen.util.Stmt;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Mike Brock
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ProxyMaker {
  public static final String PROXY_BIND_METHOD = "__$setProxiedInstance$";

  private final Map<MetaMethod, Map<WeaveType, Collection<Statement>>> weavingStatements;
  private final Map<String, ProxyProperty> proxyProperties;

  private ProxyMaker(final Map<String, ProxyProperty> proxyProperties,
                     final Map<MetaMethod, Map<WeaveType, Collection<Statement>>> weavingStatements) {
    this.proxyProperties = proxyProperties;
    this.weavingStatements = weavingStatements;
  }

  public static BuildMetaClass makeProxy(final String proxyClassName, final Class cls) {
    return makeProxy(proxyClassName, cls, "reflection");
  }

  public static BuildMetaClass makeProxy(final String proxyClassName,
                                         final Class cls,
                                         final String privateAccessorType) {
    return makeProxy(proxyClassName, MetaClassFactory.get(cls), privateAccessorType);
  }


  public static BuildMetaClass makeProxy(final String proxyClassName,
                                         final MetaClass toProxy) {
    return makeProxy(proxyClassName, toProxy, "reflection");
  }

  public static BuildMetaClass makeProxy(final String proxyClassName,
                                         final MetaClass toProxy,
                                         final String privateAccessorType) {
    return makeProxy(proxyClassName, toProxy, privateAccessorType, Collections.<String, ProxyProperty>emptyMap(),
        Collections.<MetaMethod, Map<WeaveType, Collection<Statement>>>emptyMap());
  }

  public static BuildMetaClass makeProxy(final MetaClass toProxy,
                                         final String privateAccessorType,
                                         final Map<MetaMethod, Map<WeaveType, Collection<Statement>>> weavingStatements) {
    return makeProxy(toProxy, privateAccessorType, Collections.<String, ProxyProperty>emptyMap(), weavingStatements);

  }

  public static BuildMetaClass makeProxy(final MetaClass toProxy,
                                         final String privateAccessorType,
                                         final Map<String, ProxyProperty> proxyProperties,
                                         final Map<MetaMethod, Map<WeaveType, Collection<Statement>>> weavingStatements) {
    return makeProxy(
        PrivateAccessUtil.condensify(toProxy.getPackageName()) + "_" + toProxy.getName() + "_proxy",
        toProxy,
        privateAccessorType, proxyProperties, weavingStatements);
  }

  public static BuildMetaClass makeProxy(final String proxyClassName,
                                         final MetaClass toProxy,
                                         final String privateAccessorType,
                                         final Map<String, ProxyProperty> proxyProperties,
                                         final Map<MetaMethod, Map<WeaveType, Collection<Statement>>> weavingStatements) {
    return new ProxyMaker(proxyProperties, weavingStatements).make(proxyClassName, toProxy, privateAccessorType);
  }


  BuildMetaClass make(final String proxyClassName,
                      final MetaClass toProxy,
                      final String privateAccessorType) {
    final ClassStructureBuilder builder;

    final Override override = new Override() {
      @Override
      public Class<? extends Annotation> annotationType() {
        return Override.class;
      }
    };

    final boolean renderEqualsAndHash;
    if (!toProxy.isInterface()) {
      renderEqualsAndHash = true;
      if (toProxy.isFinal()) {
        throw new UnproxyableClassException(toProxy, toProxy.getFullyQualifiedName()
            + " is an unproxiable class because it is final");
      }
      if (!toProxy.isDefaultInstantiable()) {
        throw new UnproxyableClassException(toProxy, toProxy.getFullyQualifiedName() + " must have a default " +
            "no-arg constructor");
      }

      builder = ClassBuilder.define(proxyClassName, toProxy).publicScope().body();
    }
    else {
      renderEqualsAndHash = false;
      builder = ClassBuilder.define(proxyClassName).publicScope().implementsInterface(toProxy).body();
    }

    final String proxyVar = "$$_proxy_$$";

    final Set<String> renderedMethods = new HashSet<String>();

    final Map<String, MetaType> typeVariableMap = new HashMap<String, MetaType>();
    final MetaParameterizedType metaParameterizedType = toProxy.getParameterizedType();

    if (metaParameterizedType != null) {
      int i = 0;
      for (final MetaTypeVariable metaTypeVariable : toProxy.getTypeParameters()) {
        typeVariableMap.put(metaTypeVariable.getName(), metaParameterizedType.getTypeParameters()[i++]);
      }
    }

    builder.privateField(proxyVar, toProxy).finish();

    final Set<Map.Entry<String, ProxyProperty>> entries = proxyProperties.entrySet();
    for (final Map.Entry<String, ProxyProperty> entry : entries) {
      builder.privateField(entry.getValue().getEncodedProperty(), entry.getValue().getType()).finish();
      builder.packageMethod(void.class, "$set_" + entry.getKey(), Parameter.of(entry.getValue().getType(), "o"))
          .append(Stmt.loadVariable(entry.getValue().getEncodedProperty()).assignValue(Refs.get("o")))
          .finish();
    }

    for (final MetaMethod method : toProxy.getMethods()) {
      final String methodString = GenUtil.getMethodString(method);
      if (renderedMethods.contains(methodString) || method.getName().equals("hashCode")
          || (method.getName().equals("equals") && method.getParameters().length == 1
          && method.getParameters()[0].getType().getFullyQualifiedName().equals(Object.class.getName()))) continue;

      renderedMethods.add(methodString);

      if ((!method.isPublic() && !method.isProtected()) ||
          method.isSynthetic() ||
          method.isFinal() ||
          method.isStatic() ||
          method.getDeclaringClass().getFullyQualifiedName().equals(Object.class.getName()))
        continue;

      final List<Parameter> methodParms = new ArrayList<Parameter>();
      final MetaParameter[] parameters = method.getParameters();

      for (int i = 0; i < parameters.length; i++) {
        methodParms.add(Parameter.of(parameters[i].getType().getErased(), "a" + i));
      }

      final DefParameters defParameters = DefParameters.fromParameters(methodParms);
      final BlockBuilder methBody = builder.publicMethod(method.getReturnType(), method.getName())
          .annotatedWith(override)
          .parameters(defParameters)
          .throws_(method.getCheckedExceptions());

      methBody.appendAll(getAroundInvokeStatements(method));
      methBody.appendAll(getBeforeStatements(method));

      final List<Parameter> parms = defParameters.getParameters();

      final Statement[] statementVars = new Statement[parms.size()];
      for (int i = 0; i < parms.size(); i++) {
        statementVars[i] = loadVariable(parms.get(i).getName());
      }

      if (!method.isPublic()) {
        PrivateAccessUtil.addPrivateAccessStubs(privateAccessorType, builder, method, new Modifier[0]);

        final Statement[] privateAccessStmts = new Statement[statementVars.length + 1];
        privateAccessStmts[0] = Refs.get(proxyVar);
        System.arraycopy(statementVars, 0, privateAccessStmts, 1, statementVars.length);

        if (method.getReturnType().isVoid()) {
          methBody._(loadVariable("this").invoke(PrivateAccessUtil.getPrivateMethodName(method), privateAccessStmts));
        }
        else {
          methBody._(loadVariable("this").invoke(PrivateAccessUtil.getPrivateMethodName(method), privateAccessStmts).returnValue());
        }
      }
      else {
        if (method.getReturnType().isVoid()) {
          methBody._(loadVariable(proxyVar).invoke(method, statementVars));
        }
        else {
          methBody._(loadVariable(proxyVar).invoke(method, statementVars).returnValue());
        }
      }

      methBody.appendAll(getAfterStatements(method));
      methBody.appendAll(getAroundInvokeStatements(method));

      methBody.finish();
    }

    if (renderEqualsAndHash) {
      // implement hashCode()
      builder.publicMethod(int.class, "hashCode")
          .annotatedWith(override)
          .body()
          ._(
              If.isNull(loadVariable(proxyVar))
                  ._(throw_(IllegalStateException.class, "call to hashCode() on an unclosed proxy."))
                  .finish()
                  .else_()
                  ._(Stmt.loadVariable(proxyVar).invoke("hashCode").returnValue())
                  .finish()
          )
          .finish();

      // implements equals()
      builder.publicMethod(boolean.class, "equals", Parameter.of(Object.class, "o"))
          .annotatedWith(override)
          .body()
          ._(
              If.isNull(loadVariable(proxyVar))
                  ._(throw_(IllegalStateException.class, "call to equals() on an unclosed proxy."))
                  .finish()
                  .else_()
                  ._(Stmt.loadVariable(proxyVar).invoke("equals", Refs.get("o")).returnValue())
                  .finish()
          )
          .finish();
    }

    builder.publicMethod(void.class, PROXY_BIND_METHOD).parameters(DefParameters.of(Parameter.of(toProxy, "proxy")))
        ._(loadVariable(proxyVar).assignValue(loadVariable("proxy"))).finish();

    return builder.getClassDefinition();
  }

  private Collection<Statement> getWeavingStatements(final MetaMethod method, final WeaveType type) {
    final Map<WeaveType, Collection<Statement>> weaveTypeListMap = weavingStatements.get(method);
    if (weaveTypeListMap == null) {
      return Collections.emptyList();
    }
    else {
      final Collection<Statement> statementList = weaveTypeListMap.get(type);
      if (statementList == null) {
        return Collections.emptyList();
      }
      else {
        return Collections.unmodifiableCollection(statementList);
      }
    }
  }

  public Collection<Statement> getAroundInvokeStatements(final MetaMethod method) {
    return getWeavingStatements(method, WeaveType.AroundInvoke);
  }

  public Collection<Statement> getBeforeStatements(final MetaMethod method) {
    return getWeavingStatements(method, WeaveType.BeforeInvoke);
  }

  public Collection<Statement> getAfterStatements(final MetaMethod method) {
    return getWeavingStatements(method, WeaveType.AfterInvoke);
  }

  public static Statement closeProxy(final Statement proxyReference, final Statement beanInstance) {
    return Stmt.nestedCall(proxyReference).invoke(PROXY_BIND_METHOD, beanInstance);
  }

  public static Collection<Statement> createAllPropertyBindings(final Statement proxyRef,
                                                                final Map<String, ProxyProperty> proxyProperties) {
    final List<Statement> statementList = new ArrayList<Statement>();
    for (final Map.Entry<String, ProxyProperty> entry : proxyProperties.entrySet()) {
      statementList.add(Stmt.nestedCall(proxyRef).invoke("$set_" + entry.getKey(), entry.getValue().getOriginalValueReference()));
    }
    return statementList;
  }

  public static class ProxyProperty implements Statement {
    private final String propertyName;
    private final MetaClass type;
    private final Statement valueReference;

    public ProxyProperty(final String propertyName, final MetaClass type, final Statement valueReference) {
      this.propertyName = propertyName;
      this.type = type;
      this.valueReference = valueReference;
    }

    public MetaClass getType() {
      return type;
    }

    public Statement getOriginalValueReference() {
      return valueReference;
    }

    public Statement getProxiedValueReference() {
      return Refs.get(getEncodedProperty());
    }

    private String getEncodedProperty() {
      return "$P_" + propertyName + "_P$";
    }

    @Override
    public String generate(Context context) {
      return getEncodedProperty();
    }
  }

}
