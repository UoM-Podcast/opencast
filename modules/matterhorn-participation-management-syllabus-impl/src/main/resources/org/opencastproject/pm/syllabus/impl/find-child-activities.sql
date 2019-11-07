SELECT DISTINCT
    child_activity.Id AS ActivityId,
    child_activity.Name AS ActivityName,
    module.Id AS ModuleId,
    module.Name AS ModuleName,
    module.Description AS ModuleDescription,
    module.UserText4 AS CourseKey,
    child_activity.IsJtaParent AS ActivityJtaParent,
    child_activity.IsJtaChild AS ActivityJtaChild,
    child_activity.IsVariantParent AS ActivityVariantParent,
    child_activity.IsVariantChild AS ActivityVariantChild,
    activitytype.Name AS Type,
    GETDATE() AS StartDate,
    GETDATE() AS EndDate
FROM rdowner.V_Activity AS activity
     INNER JOIN rdowner.V_Module AS module
          ON activity.ModuleId=module.Id
     INNER JOIN rdowner.V_Activity_Parents AS activity_parents
          ON activity_parents.parentId=activity.Id
     INNER JOIN rdowner.V_Activity AS child_activity
          ON child_activity.Id = activity_parents.ActivityId
     INNER JOIN rdowner.V_Activitytype AS activitytype
          ON child_activity.ActivityTypeId=activitytype.Id
WHERE activity.Id = ?