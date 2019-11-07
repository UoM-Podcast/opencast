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
    INNER JOIN rdowner.V_Activity_Staff activity_staff
        ON activity.Id=activity_staff.ActivityId
    INNER JOIN rdowner.V_Activitytype activitytype
        ON activity.ActivityTypeId=activitytype.Id
    INNER JOIN rdowner.V_Module module
          ON module.id = activity.ModuleId
WHERE activity_staff.StaffId = ?