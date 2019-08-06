ALTER TABLE mh_host_registration ADD COLUMN node_name VARCHAR(255) AFTER host;

-- add episode dublincore to avoid reading from file when reindexing
ALTER TABLE mh_archive_episode ADD COLUMN dublincore_xml MEDIUMTEXT AFTER access_control;
