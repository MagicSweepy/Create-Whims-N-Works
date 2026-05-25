package magicbook.whimsnworks.api.module

import java.lang.annotation.{Retention, RetentionPolicy}
import scala.annotation.StaticAnnotation

/** Marks a [[ModModule]] for automatic discovery in [[ModuleManager]].
  *
  * @example We prefer to use the compat mod or functionality of the module to name it.
 *           {{{
  *             @discover
  *             object ModIdModule extends ModModule {
  *               override def moduleId = "modid-module"
  *               // ...
  *             }
  *          }}}
 *           For example
  */
@Retention(RetentionPolicy.RUNTIME)
class discover extends StaticAnnotation
