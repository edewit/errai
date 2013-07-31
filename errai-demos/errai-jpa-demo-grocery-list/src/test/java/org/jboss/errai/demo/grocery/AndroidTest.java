package org.jboss.errai.demo.grocery;

import org.arquillian.droidium.container.api.AndroidDevice;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.io.File;

/**
 * @author edewit@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AndroidTest {

  @Deployment(name = "android")
  @TargetsContainer("android")
  public static org.jboss.shrinkwrap.api.Archive<?> createDeployment() {
    File archiveFile = new File("target/template/platforms/android/bin/HelloCordova-debug.apk");
    return ShrinkWrap.createFromZipFile(JavaArchive.class, archiveFile);
  }

  @Test
  @OperateOnDeployment("android")
  public void dumbTest(@ArquillianResource AndroidDevice android,
                       @Drone WebDriver driver) {
    android.takeScreenshot();
    driver.switchTo().window("WEBVIEW");

    driver.findElement(By.linkText("Items")).click();
    driver.findElement(By.id("itemName")).sendKeys("TestItem");
    driver.findElement(By.id("itemDept")).sendKeys("bakery");
    driver.findElement(By.id("itemComments")).sendKeys("Note");

    driver.findElement(By.className("button.btn")).click();

    driver.getPageSource().contains("TestItem");
  }
}