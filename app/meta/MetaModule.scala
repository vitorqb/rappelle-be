package meta

import com.google.inject.AbstractModule

class MetaModule extends AbstractModule {

  override def configure() = {
    bind(classOf[ApplicationStart]).asEagerSingleton()
  }

}
