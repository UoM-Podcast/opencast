ALTER TABLE mh_host_registration ADD COLUMN node_name VARCHAR(255) AFTER host;

-- add episode dublincore to avoid reading from file when reindexing
ALTER TABLE mh_archive_episode ADD COLUMN dublincore_xml MEDIUMTEXT AFTER access_control;

-- Archive AWS storage
ALTER TABLE mh_archive_episode ADD COLUMN storage_id VARCHAR(255) NOT NULL;
ALTER TABLE mh_archive_asset ADD COLUMN storage_id VARCHAR(255) NOT NULL;

CREATE TABLE mh_aws_asset_mapping (
	id BIGINT(20) NOT NULL PRIMARY KEY,
	deletion_date DATETIME DEFAULT NULL,
	media_package_element VARCHAR(128) NOT NULL,
	media_package VARCHAR(128) NOT NULL,
	object_key VARCHAR(1024) NOT NULL,
	object_version VARCHAR(1024) NOT NULL,
	organization VARCHAR(128) NOT NULL,
	version BIGINT(20) NOT NULL,
  CONSTRAINT UNQ_mh_aws_asset_mapping UNIQUE (organization,media_package,media_package_element,version),
  CONSTRAINT FK_mh_aws_asset_mapping_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;