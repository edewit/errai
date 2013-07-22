package org.jboss.errai.demo.grocery;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.TextBox;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.gwt.RunAsGwtClient;
import org.jboss.arquillian.gwt.client.ArquillianGwtTestCase;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Collection;

/**
 * @author edewit@redhat.com
 */
@RunWith(Arquillian.class)
public class GwtIntegraionTest extends ArquillianGwtTestCase {

  @Deployment
  public static WebArchive createDeployment() {
    final Collection<GenericArchive> lib = DependencyResolvers.use(MavenDependencyResolver.class)
            .artifact("org.jboss.errai:errai-javaee-all:3.0-SNAPSHOT").resolveAs(GenericArchive.class);
    return ShrinkWrap.create(WebArchive.class, "test.war")
            .addAsLibraries(lib)
//        .addClass(GreetingService.class)
//        .addClass(GreetingServiceImpl.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"));
  }


  @Test
  @RunAsGwtClient(moduleName = "org.jboss.errai.demo.grocery.Test")
  public void testGreetingService() {
    final TextBox textBox1 = new TextBox();
    final TextBox textBox2 = new TextBox();
    textBox1.addValueChangeHandler(new ValueChangeHandler<String>() {
      @Override
      public void onValueChange(ValueChangeEvent<String> newValueEvent) {
        textBox2.setText(newValueEvent.getValue());
      }
    });
    textBox1.setValue("Hello, Earthling!", true);
    assertEquals("TextBox values do not match!", textBox1.getText(), textBox2.getText());
  }
}

