// The lint Jenkins gets the same tool name as the real one ("jdk-21", used by "tools { jdk 'jdk-21' }"),
// because the declarative validator rejects a Jenkinsfile that names a tool installation that does not exist.
import hudson.model.JDK
import jenkins.model.Jenkins

def descriptor = Jenkins.get().getDescriptorByType(JDK.DescriptorImpl)
descriptor.setInstallations(new JDK('jdk-21', '/opt/java/openjdk'))
descriptor.save()
