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
package org.jboss.errai.ui.shared.api.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Composite;

/**
 * Indicates that the target Java reference (field, method parameter, or constructor parameter) in a {@link Templated}
 * {@link Composite} component corresponds to an HTML element in the class's companion template.
 * <p>
 * <b>This annotation can only be used in subclasses of {@link Composite} that have been annotated with {@link Templated},
 * or in super-classes of such {@link Composite} types.</b>
 * <p>
 * The matching of Java references to HTML elements is performed as follows:
 * <ol>
 *  <li>A <i>name</i> for the Java reference is determined. If the {@code @DataField} annotation has a {@link #value}
 *      argument, it is used as the reference name. For fields, the default name is the field name. Method and constructor
 *      parameters have no default name, so they must always specify a value.
 *  <li>If there is an element in the HTML template with attribute <tt>data-field=<i>name</i></tt>, the Java reference
 *      will point to this element. If there is more than one such element, the Java reference will point to the first.
 *  <li>Otherwise, if there is an element in the HTML template with attribute <tt>id=<i>name</i></tt>, the Java reference
 *      will point to this element. If there is more than one such element, the Java reference will point to the first.
 *  <li>Otherwise, if there is an element in the HTML template with a CSS style class <tt><i>name</i></tt>, the Java reference
 *      will point to this element. If there is more than one such element, the Java reference will point to the first.
 *      For elements with more than one CSS style, each style name is considered individually. For example:
 * <pre>
 *  &lt;div class="eat drink be-merry"&gt;
 * </pre>
 *      matches Java references named <tt>eat</tt>, <tt>drink</tt>, or <tt>be-merry</tt>.
 *  <li>If no matching element is found by this point, it is an error.
 * </ol>
 * <p>
 * If more than one Java reference matches the same HTML element in the template, it is an error. For example,
 * given a template containing the element <tt>&lt;div class="eat drink be-merry"&gt;</tt>, the following Java code
 * is in error:
 * <pre>
 * @Templated
 * public class ErroneousTemplate extends Composite {
 *  @Inject @DataField
 *  private Label eat;
 * 
 *  @Inject @DataField
 *  private Label drink;
 * }
 * </pre>
 * because both fields <tt>eat</tt> and <tt>drink</tt> refer to the same HTML <tt>div</tt> element.
 * <p>
 * When used on a method or constructor parameter, this must be accompanied by a corresponding {@link Inject}
 * annotation; however, when used on a field, construction may be performed manually or via {@link Inject}.
 * 
 * <p>
 * <b>See also:</b> {@link Templated}, {@link EventHandler}, {@link Bound}
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author Christian Sadilek <csadilek@redhat.com>
 * @author Jonathan Fuerth <jfuerth@redhat.com>
 */
@Inherited
@Documented
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface DataField
{

   /**
    * Specify the name of the <code>data-field</code> in the corresponding HTML template, which the annotated element
    * represents.
    */
   String value() default "";

}
