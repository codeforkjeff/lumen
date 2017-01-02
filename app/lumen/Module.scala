package lumen

import com.google.inject.AbstractModule
import play.{Configuration, Environment}

/**
  * Guice dependency injection configuration
  */
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure() = {
    // bind Config
    val bindingClassName = configuration.getString("lumen.config")
    if(bindingClassName != null) {
      val bindingClass: Class[_ <: Config] =
        environment.classLoader.loadClass(bindingClassName)
          .asSubclass(classOf[Config])
      bind(classOf[Config])
        .to(bindingClass)
    } else {
      bind(classOf[Config])
        .to(classOf[DefaultConfig])
    }

  }

}
