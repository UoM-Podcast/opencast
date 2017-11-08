# AddEpisodeAclWorkflowOperationHandler

## Description
AddEpisodeAclWorkflowOperation adds an episode ACL xml file as attachment to the media package given a list of Roles.
The list of roles can be provided as comma separated via a configuration key or via the audience tag of the episode. 

## Parameter Table

|configuration keys|example|description|default value|
|------------------|-------|-----------|-------------|
|roles|"ROLE_1,ROLE_2,ROLE_3"|List of roles as comma separated|EMPTY|


## Operation Example

    <operation
      id="add-episode-acl"
      fail-on-error="true"
      exception-handler-workflow="error"
      description="Add episode ACL from configuration key roles and/or audience">
      <configurations>
        <configuration key="roles">ROLE_1,ROLE_2</configuration>
      </configurations>
    </operation>

## Episode with audience tag example

    <?xml version="1.0"?>
    <dublincore xmlns="http://www.opencastproject.org/xsd/1.0/dublincore/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:dcterms="http://purl.org/dc/terms/" xmlns:oc="http://www.opencastproject.org/matterhorn/">

      <dcterms:title xml:lang="en">
        Land and Vegetation: Key players on the Climate Scene
        </dcterms:title>
      <dcterms:subject>
        climate, land, vegetation
        </dcterms:subject>
      <dcterms:description xml:lang="en">
        Introduction lecture from the Institute for
        Atmospheric and Climate Science.
        </dcterms:description>
      <dcterms:publisher>
        University of Opencast
        </dcterms:publisher>
      <dcterms:identifier>
        10.0000/5819
        </dcterms:identifier>
      <dcterms:audience>
        ROLE_3,ROLE_4
      </dcterms:audience>
      <dcterms:modified xsi:type="dcterms:W3CDTF">
        2007-12-05
        </dcterms:modified>
      <dcterms:format xsi:type="dcterms:IMT">
        video/x-dv
        </dcterms:format>
    </dublincore>
