package backend.services

import common.models.values.Source
import common.models.event.GenericEvent

object GenericEventImport {
  def makeDiff(oldElts: List[GenericEvent], newElts: List[GenericEvent]): (List[GenericEvent], List[GenericEvent], List[(GenericEvent, GenericEvent)]) = {
    val createdElts = newElts.filter { ne => oldElts.find(oe => hasMatch(ne.sources, oe.sources)).isEmpty }
    val deletedElts = oldElts.filter { oe => newElts.find(ne => hasMatch(oe.sources, ne.sources)).isEmpty }
    val updatedElts = oldElts
      .map(oe => newElts.find(ne => hasMatch(oe.sources, ne.sources)).map(ne => (oe, ne))).flatten
      .map { case (oe, ne) => (oe, merge(oe, ne)) }
      .filter { case (oe, ne) => !equals(oe, ne) }
    (createdElts, deletedElts, updatedElts)
  }

  private def hasMatch(sources1: List[Source], sources2: List[Source]): Boolean = sources1.find(s1 => sources2.contains(s1)).isDefined
  private def equals(oldElt: GenericEvent, newElt: GenericEvent): Boolean = oldElt.copy(uuid = "") == newElt.copy(uuid = "")
  // TODO : real merge for GenericEvent !!!
  private def merge(oldElt: GenericEvent, newElt: GenericEvent): GenericEvent = newElt.copy(
    sources = (oldElt.sources ++ newElt.sources).distinct,
    uuid = oldElt.uuid,
    status = oldElt.status)
}
