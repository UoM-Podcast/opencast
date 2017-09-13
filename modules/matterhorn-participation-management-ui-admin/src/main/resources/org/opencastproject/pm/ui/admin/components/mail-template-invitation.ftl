Dear Colleague,

Please read all the following information as it is important. 

I am writing to make you aware that the following unit(s) on which you teach have been scheduled to be recorded by the University’s automated lecture capture system; also know as the Podcasting system. If a lecture event is scheduled in an equipped location, the Podcasting system will automatically detect it and schedule it for recording in accordance with the University's Lecture Recording Policy[1]. Further essential information about the use of microphones and avoiding the recording of private conversations can be found on the following website:

http://www.mypodcasts.manchester.ac.uk/essential_information/

The following units are currently scheduled to be recorded:

<#list modules as module>
* ${module.name}<#if module.description?? && module.description != "">, ${module.description}</#if>
<#if module.lecturesChanged gt 0>- ${module.lecturesChanged} lectures have changed in the timetable and need to reviewed</#if>
<#if module.requirementChange>- <#if module.required>This unit requires recording for students registered with DASS<#else>This unit is no longer requires recording for students registered with DASS</#if></#if>

</#list>

Recorded lectures can be managed by clicking the following link and using the opt-out instructions[2].

${optOutLink}

<#list modules as module><#if module.required>Please note that one or more of your units require recording for a student who is registered with the Disability Advisory Support Service (DASS) and access to such a recording is considered to be a reasonable adjustment. If recording for this unit is opted-out, or set for editing, the teaching activities will still be recorded and published. Only students defined by DASS as required access to these recordings and staff on the unit will be able to view these recordings.<#break></#if>

</#list>

All recordings can be accessed at the following website, if you wish to link this in your Blackboard course please use this web address:

https://video.manchester.ac.uk/lectures

Lastly, if you are the unit coordinator for the above unit(s), please forward this message to all staff teaching on your unit as, in some cases, they may not be always be identified in the timetable system and therefore may not be sent this message directly.

[1] http://documents.manchester.ac.uk/DocuInfo.aspx?DocID=16559
[2] http://www.mypodcasts.manchester.ac.uk/staff-faqs#collapse3