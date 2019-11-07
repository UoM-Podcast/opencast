SELECT
    act.Id AS ActivityId,
    act.Name AS ActivityName,
    act.ModuleId AS ModuleId,
    mo.Name AS ModuleName,
    mo.Description AS ModuleDescription,
    mo.UserText4 AS CourseKey,
    act.IsJtaParent AS ActivityJtaParent,
    act.IsJtaChild AS ActivityJtaChild,
    act.IsVariantParent AS ActivityVariantParent,
    act.IsVariantChild AS ActivityVariantChild,
    atp.Name AS Type,
    adt.StartDateTime AS StartDate,
    adt.EndDateTime AS EndDate
      FROM rdowner.V_ACTIVITY act
        LEFT JOIN rdowner.V_Module mo
          ON act.ModuleId=mo.Id
        INNER JOIN rdowner.V_ACTIVITYTYPE atp
          ON act.ActivityTypeId=atp.Id
        INNER JOIN rdowner.V_ACTIVITY_LOCATION alo
          ON alo.ActivityId=act.Id
        INNER JOIN rdowner.V_ACTIVITY_DATETIME adt
          ON adt.ActivityId=act.Id
      WHERE alo.LocationId = ?
        AND adt.StartDateTime
          BETWEEN ? AND ?