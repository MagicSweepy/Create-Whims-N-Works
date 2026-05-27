package magicbook.whimsnworks.module

import com.simibubi.create.foundation.data.CreateRegistrate
import magicbook.whimsnworks.CW2Mod
import magicbook.whimsnworks.api.module.ModModule
import magicbook.whimsnworks.registration.{CW2BlockEntities, CW2Blocks, CW2TransmissionSets}

object CoreModule extends ModModule {

  override def moduleId: String   = "core"
  override def moduleName: String = s"${CW2Mod.NAME} | Core Module"

  override def onRegister(registrate: CreateRegistrate): Unit = {
    CW2Blocks.register()
    CW2BlockEntities.register()
  }

  override def onInit(): Unit = {
      // TODO: Common setup
  }

  override def onClientInit(): Unit = {
    CW2TransmissionSets.register()
  }
}
