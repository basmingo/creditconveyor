package ru.neoflex.credit.conveyor.config

import ru.neoflex.credit.conveyor.ScoringProtoService.Position

object PositionFilter {
  val middleManagers: Set[Position] = Set(
    Position.MANAGER,
    Position.SUPERVISOR,
    Position.TEAM_LEAD
  )

  val topManagers: Set[Position] = Set(
    Position.TOP_MANAGER,
    Position.CEO)

}
