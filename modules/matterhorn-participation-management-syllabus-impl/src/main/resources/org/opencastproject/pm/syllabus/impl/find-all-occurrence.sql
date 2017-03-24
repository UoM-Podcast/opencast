SELECT
  act.Id, 
  act.Name, 
  act.LastChanged,
  act_dat.LastChanged,
  act_loc.LastChanged,
  loc.LastChanged,
  act_dat.StartDateTime,
  act_dat.EndDateTime, 
  loc.Id, 
  loc.Name, 
  loc.HostKey 
FROM V_ACTIVITY AS act
  JOIN V_ACTIVITY_DATETIME act_dat
    ON act.Id = act_dat.ActivityId
  JOIN V_ACTIVITY_LOCATION act_loc
    ON act.Id = act_loc.ActivityId
  JOIN V_LOCATION loc
    ON act_loc.LocationId = loc.Id
WHERE
  (
    act.LastChanged > ?0
    OR act_dat.LastChanged > ?0
    OR loc.LastChanged > ?0
  )
  AND act_dat.StartDateTime < ?1
  AND act.IsScheduled = 1 -- 1 == true

