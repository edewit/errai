package org.jboss.errai.demo.grocery;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;

import java.io.File;

/**
 * @author edewit@redhat.com
 */
public class Deployment {

  public static WebArchive create(String warArtifactName) {
    final MavenDependencyResolver resolver = DependencyResolvers.use(MavenDependencyResolver.class)
        .loadMetadataFromPom("pom.xml").goOffline();

    final File archiveFile = resolver.artifacts(warArtifactName).resolveAsFiles()[0];
    final Archive<?> webArchive = ShrinkWrap.createFromZipFile(WebArchive.class, archiveFile);

    return ShrinkWrap.create(WebArchive.class, "test.war").merge(webArchive);
  }
}
