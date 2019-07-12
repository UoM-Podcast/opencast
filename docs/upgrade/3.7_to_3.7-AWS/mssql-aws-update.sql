ALTER TABLE mh_host_registration ADD COLUMN node_name VARCHAR(255) AFTER host;

ALTER TABLE mh_archive_episode ADD COLUMN dublincore_xml VARCHAR(MAX) AFTER access_control;
