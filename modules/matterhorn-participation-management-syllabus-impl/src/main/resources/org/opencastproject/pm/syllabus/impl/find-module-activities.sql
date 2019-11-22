SELECT DISTINCT
    activity.Id AS ActivityId,
    activity.Name AS ActivityName,
    activity.ModuleId AS ModuleId,
    module."Name" AS ModuleName,
    module.Description AS ModuleDescription,
    module.UserText4 AS CourseKey,
    activity.IsJtaParent AS ActivityJtaParent,
    activity.IsJtaChild AS ActivityJtaChild,
    activity.IsVariantParent AS ActivityVariantParent,
    activity.IsVariantChild AS ActivityVariantChild,
    activitytype.Name AS Type,
    GETDATE() AS StartDate,
    GETDATE() AS EndDate
FROM rdowner.V_Activity AS activity
     INNER JOIN rdowner.V_Module AS module
        ON activity.ModuleId=module.Id
     INNER JOIN rdowner.V_Activitytype AS activitytype
        ON activity.ActivityTypeId=activitytype.Id
WHERE module.Name LIKE ?