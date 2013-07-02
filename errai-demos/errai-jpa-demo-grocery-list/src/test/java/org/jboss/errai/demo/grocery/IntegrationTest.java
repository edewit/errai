package org.jboss.errai.demo.grocery;

import com.thoughtworks.selenium.DefaultSelenium;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URL;

import static org.junit.Assert.assertTrue;

/**
 * @author edewit@redhat.com
 */
@RunWith(Arquillian.class)
public class IntegrationTest {

  @Deployment(testable = false)
  public static WebArchive createDeployment() {
    return org.jboss.errai.demo.grocery.Deployment.create(
        "org.jboss.errai.demos:errai-jpa-demo-grocery-list:war:3.0-SNAPSHOT");
  }

  @Drone
  DefaultSelenium browser;

  @ArquillianResource
  URL deploymentUrl;


  @Test
  public void testGreetingService() {
    browser.open(deploymentUrl.toString());
    browser.waitForPageToLoad("20000");

    browser.click("link=Items");
    browser.type("id=itemName", "TestItem");
    browser.type("id=itemDept", "bakery");
    browser.type("id=itemComments", "Note");

    browser.click("css=button.btn");


    assertTrue(browser.isTextPresent("TestItem"));
  }
}
