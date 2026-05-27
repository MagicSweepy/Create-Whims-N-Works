package magicbook.whimsnworks.registration

import com.simibubi.create.api.registrate.CreateRegistrateRegistrationCallback
import com.simibubi.create.foundation.data.CreateRegistrate
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent

object CW2Registrate {
    def create(modid: String): CW2Registrate = {
        val registrate = CW2Registrate(modid)
        CreateRegistrateRegistrationCallback.provideRegistrate(registrate)
        registrate
    }
}

class CW2Registrate(modid: String) extends CreateRegistrate(modid) {

    override def onBuildCreativeModeTabContents(event: BuildCreativeModeTabContentsEvent): Unit = {
        // ...
    }
}
