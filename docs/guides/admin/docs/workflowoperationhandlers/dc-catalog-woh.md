# DC Catalog Workflow Operation

## Description
The DC Catalog Workflow Operation allows the editing of the Dublin Core catalogs.

Currently it is only possible to edit set the *dcterm:available* from value to now.

## Parameter Table
Tags and flavors can be used in combination.

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|source-tags       |"engage,atom,rss,-publish"|Tag any media package elements with one of these (comma separated) tags. If a source-tag starts with a '-', media package elements with this tag will be excluded.|EMPTY|
|source-flavors    |"dublincore/episode"      |Edit and tag any dublincore catalogs with one of these (comma separated) flavors.|EMPTY|
|set-available-date|"true" or "false"         |Set the available from value to the current date and time|false|
|target-tags       |"tagged,+rss" / "-rss,+tagged"|Apply these (comma separated) tags to any media package elements. If a target-tag starts with a '-', it will be removed from preexisting tags, if a target-tag starts with a '+', it will be added to preexisting tags. If there is no prefix, all preexisting tags are removed and replaced by the target-tags.|EMPTY|
|target-flavor     |"presentation/tagged"     |Apply these flavor to any media package elements|EMPTY|
|copy              |"true" or "false"         |Indicates if matching elements will be cloned before tagging is applied or whether tagging is applied to the original element. Set to "true" to create a copy first, "false" otherwise.|FALSE|
 
Note: see [TagWorkflowOperationHandler](tag-woh.md) for further explanation of the source/target-flavour/tags

### set-available-date
Set the start value of the *dcterm:available* period to the current date and time i.e. now. This will either add an
available element or modify an existing one, so that any end date is preserved.

## Operation Example
    <operation
      id="dc-catalog"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Edit the available ">
      <configurations>
        <configuration key="source-flavors">dublincore/episode</configuration>
        <configuration key="set-available-date">true</configuration>
      </configurations>
    </operation>
