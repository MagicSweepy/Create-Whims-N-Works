package magicbook.whimsnworks.module

import com.simibubi.create.foundation.data.CreateRegistrate
import magicbook.whimsnworks.CW2Mod
import magicbook.whimsnworks.api.module.{ModModule, discover}

@discover
object CoreModule extends ModModule {

  override def moduleId: String   = "core"
  override def moduleName: String = s"${CW2Mod.NAME} | Core Module"

  override def onRegister(registrate: CreateRegistrate): Unit = {
  // TODO: Register common blocks, items, creative tab, e.t.c.
  }

  override def onInit(): Unit = {
      // TODO: Common setup
  }

  override def onClientInit(): Unit = {
      // TODO: Client setup
  }
}
