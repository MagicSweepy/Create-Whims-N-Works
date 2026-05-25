package magicbook.whimsnworks.api.module

import com.simibubi.create.foundation.data.CreateRegistrate

trait ModModule {

  /** The unique id for the module.
   *
   *  It is different with the mod id, please use unique id which different the mod id.
   */
  def moduleId: String

  /** The human-readable module name.
   *
   *  It is useful for our understanding. We use [[moduleId]] by default name of the module.
   */
  def moduleName: String = moduleId

  /** All mod ids that must be loaded for this module to activate.
   *
   *  If any of these mods are missing, the module is silently skipped.
   */
  def requiredMods: Set[String] = Set.empty

  /** All module ids that must be loaded before this one.
   *
    * If any dependency is not loaded this module is skipped. Will determines init order.
    */
  def requiredModules: Set[String] = Set.empty

  /** Common setup, e.g. event listeners, network packets and recipes, e.t.c.
    *
    * Called during mod construction, after (mod and module) dependencies checks pass.
    */
  def onInit(): Unit = ()

  /** Client setup, e.g. renderers, models, screen registration, e.t.c.
    *
    * Called only on the client side, after [[onInit]].
    */
  def onClientInit(): Unit = ()

  /** [[Registrate]] context setup.
    * 
    * Called before [[onInit]] in dependency order.
    *
    * [[Registrate]] handles the underlying NF registry events internally, so 
    * registration calls here are safe to make eagerly during mod construction.
    */
  def onRegister(registrate: CreateRegistrate): Unit = ()
}
