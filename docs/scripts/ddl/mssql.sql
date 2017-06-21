CREATE TABLE SEQUENCE (
  SEQ_NAME VARCHAR(50) NOT NULL,
  SEQ_COUNT DECIMAL(38),
  PRIMARY KEY (SEQ_NAME)
);

INSERT INTO SEQUENCE(SEQ_NAME, SEQ_COUNT) values ('SEQ_GEN', 0);

CREATE TABLE mh_bundleinfo (
  id BIGINT NOT NULL,
  bundle_name VARCHAR(128) NOT NULL,
  build_number VARCHAR(128) DEFAULT NULL,
  host VARCHAR(128) NOT NULL,
  bundle_id BIGINT NOT NULL,
  bundle_version VARCHAR(128) NOT NULL,
  db_schema_version VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_bundleinfo UNIQUE (host, bundle_name, bundle_version)
);

CREATE TABLE mh_organization (
  id VARCHAR(128) NOT NULL,
  anonymous_role VARCHAR(255),
  name VARCHAR(255),
  admin_role VARCHAR(255),
  PRIMARY KEY (id)
);

CREATE TABLE mh_organization_node (
  organization VARCHAR(128) NOT NULL,
  port INT,
  name VARCHAR(255),
  PRIMARY KEY (organization, port, name),
  CONSTRAINT FK_mh_organization_node_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_organization_node_pk ON mh_organization_node (organization);
CREATE INDEX IX_mh_organization_node_name ON mh_organization_node (name);
CREATE INDEX IX_mh_organization_node_port ON mh_organization_node (port);

CREATE TABLE mh_organization_property (
  organization VARCHAR(128) NOT NULL,
  name VARCHAR(255) NOT NULL,
  value VARCHAR(MAX),
  PRIMARY KEY (organization, name),
  CONSTRAINT FK_mh_organization_property_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_organization_property_pk ON mh_organization_property (organization);

CREATE TABLE mh_annotation (
  id BIGINT NOT NULL,
  inpoint INTEGER,
  outpoint INTEGER,
  mediapackage VARCHAR(128),
  session VARCHAR(128),
  created DATETIME,
  user_id VARCHAR(255),
  length INTEGER,
  type VARCHAR(128),
  value VARCHAR(MAX),
  private bit DEFAULT 0,
  PRIMARY KEY (id)
);

CREATE INDEX IX_mh_annotation_created ON mh_annotation (created);
CREATE INDEX IX_mh_annotation_inpoint ON mh_annotation (inpoint);
CREATE INDEX IX_mh_annotation_outpoint ON mh_annotation (outpoint);
CREATE INDEX IX_mh_annotation_mediapackage ON mh_annotation (mediapackage);
CREATE INDEX IX_mh_annotation_private ON mh_annotation (private);
CREATE INDEX IX_mh_annotation_user ON mh_annotation (user_id);
CREATE INDEX IX_mh_annotation_session ON mh_annotation (session);
CREATE INDEX IX_mh_annotation_type ON mh_annotation (type);

CREATE TABLE mh_capture_agent_role (
  id VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  role VARCHAR(255),
  PRIMARY KEY (id, organization, role),
  CONSTRAINT FK_mh_capture_agent_role_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_capture_agent_role_pk ON mh_capture_agent_role (id, organization);

CREATE TABLE mh_capture_agent_state (
  id VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  configuration VARCHAR(MAX),
  state VARCHAR(MAX) NOT NULL,
  last_heard_from BIGINT NOT NULL,
  url VARCHAR(MAX),
  PRIMARY KEY (id, organization),
  CONSTRAINT FK_mh_capture_agent_state_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE TABLE mh_host_registration (
  id BIGINT NOT NULL,
  host VARCHAR(255) NOT NULL,
  address VARCHAR(39) NOT NULL,
  memory BIGINT NOT NULL,
  cores INTEGER NOT NULL,
  maintenance bit DEFAULT 0 NOT NULL,
  online bit DEFAULT 1 NOT NULL,
  active bit DEFAULT 1 NOT NULL,
  max_load FLOAT NOT NULL DEFAULT '1.0',
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_host_registration UNIQUE (host)
);

CREATE INDEX IX_mh_host_registration_online ON mh_host_registration (online);
CREATE INDEX IX_mh_host_registration_active ON mh_host_registration (active);

CREATE TABLE mh_service_registration (
  id BIGINT NOT NULL,
  path VARCHAR(255) NOT NULL,
  job_producer bit DEFAULT 0 NOT NULL,
  service_type VARCHAR(255) NOT NULL,
  online bit DEFAULT 1 NOT NULL,
  active bit DEFAULT 1 NOT NULL,
  online_from DATETIME,
  service_state int NOT NULL,
  state_changed DATETIME,
  warning_state_trigger BIGINT,
  error_state_trigger BIGINT,
  host_registration BIGINT,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_service_registration UNIQUE (host_registration, service_type),
  CONSTRAINT FK_mh_service_registration_host_registration FOREIGN KEY (host_registration) REFERENCES mh_host_registration (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_service_registration_service_type ON mh_service_registration (service_type);
CREATE INDEX IX_mh_service_registration_service_state ON mh_service_registration (service_state);
CREATE INDEX IX_mh_service_registration_active ON mh_service_registration (active);
CREATE INDEX IX_mh_service_registration_host_registration ON mh_service_registration (host_registration);

CREATE TABLE mh_job (
  id BIGINT NOT NULL,
  status INTEGER,
  payload VARCHAR(MAX),
  date_started DATETIME,
  run_time BIGINT,
  creator VARCHAR(MAX) NOT NULL,
  instance_version BIGINT,
  date_completed DATETIME,
  operation VARCHAR(128),
  dispatchable bit DEFAULT 1,
  organization VARCHAR(128) NOT NULL,
  date_created DATETIME,
  queue_time BIGINT,
  creator_service BIGINT,
  processor_service BIGINT,
  parent BIGINT,
  root BIGINT,
  job_load FLOAT NOT NULL DEFAULT 1.0,
  blocking_job BIGINT DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_job_creator_service FOREIGN KEY (creator_service) REFERENCES mh_service_registration (id),
  CONSTRAINT FK_mh_job_processor_service FOREIGN KEY (processor_service) REFERENCES mh_service_registration (id),
  CONSTRAINT FK_mh_job_parent FOREIGN KEY (parent) REFERENCES mh_job (id),
  CONSTRAINT FK_mh_job_root FOREIGN KEY (root) REFERENCES mh_job (id),
  CONSTRAINT FK_mh_job_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_job_parent ON mh_job (parent);
CREATE INDEX IX_mh_job_root ON mh_job (root);
CREATE INDEX IX_mh_job_creator_service ON mh_job (creator_service);
CREATE INDEX IX_mh_job_processor_service ON mh_job (processor_service);
CREATE INDEX IX_mh_job_status ON mh_job (status);
CREATE INDEX IX_mh_job_date_created ON mh_job (date_created);
CREATE INDEX IX_mh_job_date_completed ON mh_job (date_completed);
CREATE INDEX IX_mh_job_dispatchable ON mh_job (dispatchable);
CREATE INDEX IX_mh_job_operation ON mh_job (operation);
CREATE INDEX IX_mh_job_statistics ON mh_job (processor_service, status, queue_time, run_time);

CREATE TABLE mh_job_argument (
  id BIGINT NOT NULL,
  argument VARCHAR(MAX),
  argument_index INTEGER,
  CONSTRAINT UNQ_job_argument_0 UNIQUE (id, argument_index),
  CONSTRAINT FK_mh_job_argument_id FOREIGN KEY (id) REFERENCES mh_job (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_job_argument_id ON mh_job_argument (id);

CREATE TABLE mh_blocking_job (
  id BIGINT NOT NULL,
  blocking_job_list BIGINT,
  job_index INTEGER,
  CONSTRAINT FK_blocking_job_id FOREIGN KEY (id) REFERENCES mh_job (id) ON DELETE CASCADE
);

CREATE TABLE mh_job_context (
  id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  value VARCHAR(MAX),
  CONSTRAINT UNQ_mh_job_context UNIQUE (id, name),
  CONSTRAINT FK_mh_job_context_id FOREIGN KEY (id) REFERENCES mh_job (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_job_context_id ON mh_job_context (id);

CREATE TABLE mh_job_mh_service_registration (
  Job_id BIGINT NOT NULL,
  servicesRegistration_id BIGINT NOT NULL,
  PRIMARY KEY (Job_id, servicesRegistration_id),
  CONSTRAINT FK_mh_job_mh_service_registration_Job_id FOREIGN KEY (Job_id) REFERENCES mh_job (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_job_mh_service_registration_servicesRegistration_id FOREIGN KEY (servicesRegistration_id) REFERENCES mh_service_registration (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_job_mh_service_registration_servicesRegistration_id ON mh_job_mh_service_registration (servicesRegistration_id);

CREATE TABLE mh_incident (
  id BIGINT NOT NULL,
  jobid BIGINT,
  timestamp DATETIME,
  code VARCHAR(255),
  severity INTEGER,
  parameters VARCHAR(MAX),
  details VARCHAR(MAX),
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_incident_jobid FOREIGN KEY (jobid) REFERENCES mh_job (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_incident_jobid ON mh_incident (jobid);
CREATE INDEX IX_mh_incident_severity ON mh_incident (severity);

CREATE TABLE mh_incident_text (
  id VARCHAR(255) NOT NULL,
  text VARCHAR(2038) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE mh_scheduled_event (
  id BIGINT NOT NULL,
  mediapackage_id VARCHAR(128),
  dublin_core VARCHAR(MAX),
  capture_agent_metadata VARCHAR(MAX),
  access_control VARCHAR(MAX),
  opt_out bit NOT NULL DEFAULT '0',
  blacklisted bit NOT NULL DEFAULT '0',
  review_status VARCHAR(255) DEFAULT NULL,
  review_date DATETIME DEFAULT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE mh_search (
  id VARCHAR(128) NOT NULL,
  series_id VARCHAR(128),
  organization VARCHAR(128) DEFAULT NULL,
  deletion_date DATETIME DEFAULT NULL,
  access_control VARCHAR(MAX),
  mediapackage_xml VARCHAR(MAX),
  modification_date DATETIME DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_search_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_search_organization ON mh_search (organization);

CREATE TABLE mh_series (
  id VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  access_control VARCHAR(MAX),
  dublin_core VARCHAR(MAX),
  opt_out bit NOT NULL DEFAULT '0',
  PRIMARY KEY (id, organization),
  CONSTRAINT FK_mh_series_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE TABLE mh_oaipmh (
  id VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  repo_id VARCHAR(255) NOT NULL,
  series_id VARCHAR(128),
  deleted bit DEFAULT '0',
  modification_date DATETIME DEFAULT NULL,
  mediapackage_xml VARCHAR(MAX) NOT NULL,
  series_dublincore_xml VARCHAR(MAX),
  episode_dublincore_xml VARCHAR(MAX),
  series_acl_xml VARCHAR(MAX),
  PRIMARY KEY (id, repo_id, organization),
  CONSTRAINT UNQ_mh_oaipmh UNIQUE (modification_date)
);

CREATE INDEX IX_mh_oaipmh_modification_date ON mh_oaipmh (modification_date);

-- set to current date and time on insert
CREATE TRIGGER mh_init_oaipmh_date
  ON "mh_oaipmh"
  AFTER INSERT, UPDATE
AS BEGIN
  UPDATE mh_oaipmh
    SET mh_oaipmh.modification_date = GETDATE() FROM mh_oaipmh mh_oaipmh
    JOIN inserted i ON i.id = mh_oaipmh.id
END;

CREATE TABLE mh_user_session (
  session_id VARCHAR(50) NOT NULL,
  user_ip VARCHAR(255),
  user_agent VARCHAR(255),
  user_id VARCHAR(255),
  PRIMARY KEY (session_id)
);

CREATE INDEX IX_mh_user_session_user_id ON mh_user_session (user_id);

CREATE TABLE mh_user_action (
  id BIGINT NOT NULL,
  inpoint INTEGER,
  outpoint INTEGER,
  mediapackage VARCHAR(128),
  session_id VARCHAR(50) NOT NULL,
  created DATETIME,
  length INTEGER,
  type VARCHAR(128),
  playing bit DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_user_action_session_id FOREIGN KEY (session_id) REFERENCES mh_user_session (session_id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_user_action_created ON mh_user_action (created);
CREATE INDEX IX_mh_user_action_inpoint ON mh_user_action (inpoint);
CREATE INDEX IX_mh_user_action_outpoint ON mh_user_action (outpoint);
CREATE INDEX IX_mh_user_action_mediapackage_id ON mh_user_action (mediapackage);
CREATE INDEX IX_mh_user_action_type ON mh_user_action (type);

CREATE TABLE mh_oaipmh_harvesting (
  url VARCHAR(255) NOT NULL,
  last_harvested DATETIME,
  PRIMARY KEY (url)
);

CREATE TABLE mh_archive_asset (
  id BIGINT NOT NULL,
  mediapackageelement VARCHAR(128) NOT NULL,
  mediapackage VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  checksum VARCHAR(255) NOT NULL,
  uri VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_archive_asset UNIQUE (organization, mediapackage, mediapackageelement, version),
  CONSTRAINT FK_mh_archive_asset_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_archive_asset_mediapackage on mh_archive_asset (mediapackage);
CREATE INDEX IX_mh_archive_asset_checksum on mh_archive_asset (checksum);
CREATE INDEX IX_mh_archive_asset_uri on mh_archive_asset (uri);

CREATE TABLE mh_archive_episode (
  id VARCHAR(128) NOT NULL,
  version BIGINT NOT NULL,
  organization VARCHAR(128) NOT NULL DEFAULT '',
  deleted bit NOT NULL DEFAULT '0',
  access_control VARCHAR(MAX),
  mediapackage_xml VARCHAR(MAX),
  modification_date DATETIME DEFAULT NULL,
  PRIMARY KEY (id,version,organization),
  CONSTRAINT FK_mh_archive_episode_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_archive_episode_id on mh_archive_episode (id);
CREATE INDEX IX_mh_archive_episode_organization on mh_archive_episode (organization);
CREATE INDEX IX_mh_archive_episode_version on mh_archive_episode (version);
CREATE INDEX IX_mh_archive_episode_deleted on mh_archive_episode (deleted);

CREATE TABLE mh_archive_version_claim (
 mediapackage VARCHAR(128) NOT NULL,
 last_claimed BIGINT NOT NULL,
 PRIMARY KEY (mediapackage)
);

CREATE INDEX IX_mh_archive_version_claim_mediapackage on mh_archive_version_claim (mediapackage);
CREATE INDEX IX_mh_archive_version_claim_last_claimed on mh_archive_version_claim (last_claimed);

 
CREATE TABLE mh_acl_managed_acl (
  pk BIGINT NOT NULL,
  acl VARCHAR(MAX) NOT NULL,
  name VARCHAR(128) NOT NULL,
  organization_id VARCHAR(128) NOT NULL,
  PRIMARY KEY (pk),
  CONSTRAINT UNQ_mh_acl_managed_acl UNIQUE (name, organization_id)
);

CREATE TABLE mh_acl_episode_transition (
  pk BIGINT NOT NULL,
  workflow_params VARCHAR(255) DEFAULT NULL,
  application_date DATETIME DEFAULT NULL,
  workflow_id VARCHAR(128) DEFAULT NULL,
  done bit DEFAULT 0,
  episode_id VARCHAR(128) DEFAULT NULL,
  organization_id VARCHAR(128) DEFAULT NULL,
  managed_acl_fk BIGINT DEFAULT NULL,
  PRIMARY KEY (pk),
  CONSTRAINT UNQ_mh_acl_episode_transition UNIQUE (episode_id, organization_id, application_date),
  CONSTRAINT FK_mh_acl_episode_transition_managed_acl_fk FOREIGN KEY (managed_acl_fk) REFERENCES mh_acl_managed_acl (pk)
);

CREATE TABLE mh_acl_series_transition (
  pk BIGINT NOT NULL,
  workflow_params VARCHAR(255) DEFAULT NULL,
  application_date DATETIME DEFAULT NULL,
  workflow_id VARCHAR(128) DEFAULT NULL,
  override bit DEFAULT 0,
  done bit DEFAULT 0,
  organization_id VARCHAR(128) DEFAULT NULL,
  series_id VARCHAR(128) DEFAULT NULL,
  managed_acl_fk BIGINT DEFAULT NULL,
  PRIMARY KEY (pk),
  CONSTRAINT UNQ_mh_acl_series_transition UNIQUE (series_id, organization_id, application_date),
  CONSTRAINT FK_mh_acl_series_transition_managed_acl_fk FOREIGN KEY (managed_acl_fk) REFERENCES mh_acl_managed_acl (pk)
);

CREATE TABLE mh_role (
  id BIGINT NOT NULL,
  description VARCHAR(255) DEFAULT NULL,
  name VARCHAR(128) DEFAULT NULL,
  organization VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_role UNIQUE (name, organization),
  CONSTRAINT FK_mh_role_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE TABLE mh_group (
  id BIGINT NOT NULL,
  group_id VARCHAR(128) DEFAULT NULL,
  description VARCHAR(255) DEFAULT NULL,
  name VARCHAR(128) DEFAULT NULL,
  role VARCHAR(255) DEFAULT NULL,
  organization VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_group UNIQUE (group_id, organization),
  CONSTRAINT FK_mh_group_organization FOREIGN KEY (organization) REFERENCES mh_organization (id)
);

CREATE TABLE mh_group_member (
  group_id BIGINT NOT NULL,
  member VARCHAR(255) DEFAULT NULL
);

CREATE TABLE mh_group_role (
  group_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (group_id,role_id),
  CONSTRAINT UNQ_mh_group_role UNIQUE (group_id, role_id),
  CONSTRAINT FK_mh_group_role_group_id FOREIGN KEY (group_id) REFERENCES mh_group (id),
  CONSTRAINT FK_mh_group_role_role_id FOREIGN KEY (role_id) REFERENCES mh_role (id)
);

CREATE TABLE mh_user (
  id BIGINT NOT NULL,
  username VARCHAR(128) DEFAULT NULL,
  password VARCHAR(MAX),
  name VARCHAR(256) DEFAULT NULL,
  email VARCHAR(256) DEFAULT NULL,
  organization VARCHAR(128) DEFAULT NULL,
  manageable bit NOT NULL DEFAULT '1',
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_user UNIQUE (username, organization),
  CONSTRAINT FK_mh_user_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_role_pk ON mh_role (name, organization);

CREATE TABLE mh_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id,role_id),
  CONSTRAINT UNQ_mh_user_role UNIQUE (user_id, role_id),
  CONSTRAINT FK_mh_user_role_role_id FOREIGN KEY (role_id) REFERENCES mh_role (id),
  CONSTRAINT FK_mh_user_role_user_id FOREIGN KEY (user_id) REFERENCES mh_user (id)
);

CREATE TABLE mh_user_ref (
  id BIGINT NOT NULL,
  username VARCHAR(128) DEFAULT NULL,
  last_login DATETIME DEFAULT NULL,
  email VARCHAR(255) DEFAULT NULL,
  name VARCHAR(255) DEFAULT NULL,
  login_mechanism VARCHAR(255) DEFAULT NULL,
  organization VARCHAR(128) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_user_ref UNIQUE (username, organization),
  CONSTRAINT FK_mh_user_ref_organization FOREIGN KEY (organization) REFERENCES mh_organization (id)
);

CREATE TABLE mh_user_ref_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT UNQ_mh_user_ref_role UNIQUE (user_id, role_id),
  CONSTRAINT FK_mh_user_ref_role_role_id FOREIGN KEY (role_id) REFERENCES mh_role (id),
  CONSTRAINT FK_mh_user_ref_role_user_id FOREIGN KEY (user_id) REFERENCES mh_user_ref (id)
);

CREATE TABLE mh_user_settings (
  id BIGINT NOT NULL,
  setting_key VARCHAR(255) NOT NULL,
  setting_value VARCHAR(MAX) NOT NULL,
  username VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_user_settings UNIQUE (username, organization)
);

CREATE TABLE mh_email_configuration (
  id BIGINT NOT NULL,
  organization VARCHAR(128) NOT NULL,
  port INT DEFAULT NULL,
  transport VARCHAR(255) DEFAULT NULL,
  username VARCHAR(255) DEFAULT NULL,
  server VARCHAR(255) NOT NULL,
  ssl_enabled bit NOT NULL DEFAULT '0',
  password VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_email_configuration UNIQUE (organization),
  CONSTRAINT FK_mh_email_configuration_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_email_configuration_organization ON mh_email_configuration (organization);

CREATE TABLE mh_message_signature (
  id BIGINT NOT NULL,
  organization VARCHAR(128) NOT NULL,
  name VARCHAR(255) NOT NULL,
  creation_date DATETIME NOT NULL,
  sender VARCHAR(255) NOT NULL,
  sender_name VARCHAR(255) NOT NULL,
  reply_to VARCHAR(255) DEFAULT NULL,
  reply_to_name VARCHAR(255) DEFAULT NULL,
  signature VARCHAR(255) NOT NULL,
  creator_username VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_message_signature UNIQUE (organization, name),
  CONSTRAINT FK_mh_message_signature_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_message_signature_organization ON mh_message_signature (organization);
CREATE INDEX IX_mh_message_signature_name ON mh_message_signature (name);

CREATE TABLE mh_message_template (
  id BIGINT NOT NULL,
  organization VARCHAR(128) NOT NULL,
  body VARCHAR(MAX) NOT NULL,
  creation_date DATETIME NOT NULL,
  subject VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  template_type VARCHAR(255) DEFAULT NULL,
  creator_username VARCHAR(255) NOT NULL,
  hidden bit NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_message_template UNIQUE (organization, name),
  CONSTRAINT FK_mh_message_template_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_message_template_organization ON mh_message_template (organization);
CREATE INDEX IX_mh_message_template_name ON mh_message_template (name);

CREATE TABLE mh_event_comment (
  id BIGINT NOT NULL,
  organization VARCHAR(128) NOT NULL,
  event VARCHAR(128) NOT NULL,
  creation_date DATETIME NOT NULL,
  author VARCHAR(255) NOT NULL,
  text VARCHAR(MAX) NOT NULL,
  reason VARCHAR(255) DEFAULT NULL,
  modification_date DATETIME NOT NULL,
  resolved_status bit NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);

CREATE TABLE mh_event_comment_reply (
  id BIGINT NOT NULL,
  event_comment_id BIGINT NOT NULL,
  creation_date DATETIME NOT NULL,
  author VARCHAR(255) NOT NULL,
  text VARCHAR(MAX) NOT NULL,
  modification_date DATETIME NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_event_comment_reply_mh_event_comment FOREIGN KEY (event_comment_id) REFERENCES mh_event_comment (id)
);
 
CREATE TABLE mh_series_elements (
  series VARCHAR(128) NOT NULL,
  organization VARCHAR(128) NOT NULL,
  type VARCHAR(128) NOT NULL,
  data VARBINARY(MAX),
  PRIMARY KEY (series, organization, type)
);

CREATE TABLE mh_series_property (
  organization VARCHAR(128) NOT NULL,
  series VARCHAR(128) NOT NULL,
  name VARCHAR(255) NOT NULL,
  value VARCHAR(MAX),
  PRIMARY KEY (organization, series, name),
  CONSTRAINT FK_mh_series_property_organization_series FOREIGN KEY (series, organization) REFERENCES mh_series (id, organization) ON DELETE CASCADE
);

CREATE INDEX IX_mh_series_property_pk ON mh_series_property (series);

CREATE TABLE mh_themes (
  id BIGINT NOT NULL,
  organization VARCHAR(128) NOT NULL,
  creation_date DATETIME NOT NULL,
  username VARCHAR(128) NOT NULL,
  name VARCHAR(255) NOT NULL,
  isDefault bit NOT NULL DEFAULT '0',
  description VARCHAR(255),
  bumper_active bit NOT NULL DEFAULT '0',
  bumper_file VARCHAR(128),
  license_slide_active bit NOT NULL DEFAULT '0',
  license_slide_background VARCHAR(128),
  license_slide_description VARCHAR(255),
  title_slide_active bit NOT NULL DEFAULT '0',
  title_slide_background VARCHAR(128),
  title_slide_metadata VARCHAR(255),
  trailer_active bit NOT NULL DEFAULT '0',
  trailer_file VARCHAR(128),
  watermark_active bit NOT NULL DEFAULT '0',
  watermark_position VARCHAR(255),
  watermark_file VARCHAR(128),
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_themes_organization FOREIGN KEY (organization) REFERENCES mh_organization (id) ON DELETE CASCADE
);

--
-- Participation Management
--

CREATE TABLE mh_pm_action (
  id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  handler VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE mh_pm_blacklist (
  id BIGINT NOT NULL,
  type VARCHAR(255) NOT NULL,
  blacklisted BIGINT NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE mh_pm_building (
  id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_pm_building_0 UNIQUE (name)
);
CREATE INDEX IX_mh_pm_building_name ON mh_pm_building (name);

CREATE TABLE mh_pm_room (
  id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_pm_room_0 UNIQUE (name)
);

CREATE INDEX "IX_mh_pm_room_name" ON "mh_pm_room" ("name");

CREATE TABLE mh_pm_capture_agent (
  id BIGINT NOT NULL,
  mh_agent VARCHAR(255) NOT NULL,
  room BIGINT DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_pm_capture_agent_0 UNIQUE (mh_agent),
  CONSTRAINT FK_mh_pm_capture_agent_room FOREIGN KEY (room) REFERENCES mh_pm_room (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_pm_capture_agent_agent ON mh_pm_capture_agent (mh_agent);

CREATE TABLE mh_pm_building_mh_pm_room (
  Building_id BIGINT NOT NULL,
  rooms_id BIGINT NOT NULL,
  PRIMARY KEY (Building_id,rooms_id),
  CONSTRAINT FK_mh_pm_building_mh_pm_room_Building_id FOREIGN KEY (Building_id) REFERENCES mh_pm_building (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_pm_building_mh_pm_room_rooms_id FOREIGN KEY (rooms_id) REFERENCES mh_pm_room (id) ON DELETE CASCADE
);

CREATE TABLE mh_pm_scheduling_source (
  id BIGINT NOT NULL,
  source VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_pm_scheduling_source UNIQUE (source)
);

CREATE TABLE mh_pm_course (
  id BIGINT NOT NULL,
  opted_out bit NOT NULL DEFAULT '0',
  description VARCHAR(255) DEFAULT NULL,
  external_course_key VARCHAR(512) DEFAULT NULL,
  name VARCHAR(255) DEFAULT NULL,
  course VARCHAR(255) NOT NULL,
  series VARCHAR(255) DEFAULT NULL,
  source BIGINT DEFAULT NULL,
  fingerprint VARCHAR(32) DEFAULT NULL,
  emailstatus VARCHAR(45) DEFAULT NULL,
  requirements VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_pm_course_0 UNIQUE (course),
  CONSTRAINT FK_mh_pm_course_scheduling_source FOREIGN KEY (source) REFERENCES mh_pm_scheduling_source(id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_pm_course_course ON mh_pm_course (course);
CREATE INDEX IX_mh_pm_course_series ON mh_pm_course (series);
CREATE INDEX IX_mh_pm_course_opted_out ON mh_pm_course (opted_out);

CREATE TABLE mh_pm_error (
  id BIGINT NOT NULL,
  source VARCHAR(MAX),
  description VARCHAR(255) DEFAULT NULL,
  type VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE mh_pm_period (
  id BIGINT NOT NULL,
  start_date DATETIME NOT NULL,
  end_date DATETIME NOT NULL,
  purpose VARCHAR(255) DEFAULT NULL,
  comment VARCHAR(MAX),
  PRIMARY KEY (id)
);

CREATE TABLE mh_pm_person (
  id BIGINT NOT NULL,
  email VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
CONSTRAINT UNQ_mh_pm_person_0 UNIQUE (email)
);

CREATE INDEX IX_mh_pm_person_email ON mh_pm_person (email);

CREATE TABLE mh_pm_person_type (
  id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  function_name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_pm_person_type_0 UNIQUE (name)
);

CREATE INDEX IX_mh_pm_person_type_name ON mh_pm_person_type (name);

CREATE TABLE mh_pm_message (
  id BIGINT NOT NULL,
  creation_date DATETIME NOT NULL,
  signature BIGINT DEFAULT NULL,
  template BIGINT DEFAULT NULL,
  creator BIGINT DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_message_signature FOREIGN KEY (signature) REFERENCES mh_message_signature (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_message_template FOREIGN KEY (template) REFERENCES mh_message_template (id),
  CONSTRAINT FK_mh_pm_message_creator FOREIGN KEY (creator) REFERENCES mh_pm_person (id) ON DELETE CASCADE,
);

CREATE INDEX IX_mh_pm_message_creation_date ON mh_pm_message (creation_date);

CREATE TABLE mh_pm_recording (
  id BIGINT NOT NULL,
  end_date DATETIME NOT NULL,
  REVIEWSTATUS VARCHAR(255) NOT NULL DEFAULT 'UNCONFIRMED',
  review_date DATETIME DEFAULT NULL,
  deleted bit NOT NULL DEFAULT '0',
  EMAILSTATUS VARCHAR(255) NOT NULL DEFAULT 'UNSENT',
  blacklisted bit NOT NULL DEFAULT '0',
  activity VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  event_id BIGINT DEFAULT NULL,
  start_date DATETIME NOT NULL,
  modification_date DATETIME NOT NULL,
  capture_agent BIGINT DEFAULT NULL,
  course BIGINT DEFAULT NULL,
  room BIGINT DEFAULT NULL,
  source BIGINT DEFAULT NULL,
  fingerprint VARCHAR(32) DEFAULT NULL,
  trim bit NOT NULL DEFAULT '0',
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_pm_recording_0 UNIQUE (activity,start_date,end_date,room),
  CONSTRAINT FK_mh_pm_recording_capture_agent FOREIGN KEY (capture_agent) REFERENCES mh_pm_capture_agent (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_pm_recording_course FOREIGN KEY (course) REFERENCES mh_pm_course (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_pm_recording_room FOREIGN KEY (room) REFERENCES mh_pm_room (id),
  CONSTRAINT FK_mh_pm_recording_scheduling_source FOREIGN KEY (source) REFERENCES mh_pm_scheduling_source(id)
);

CREATE INDEX IX_mh_pm_recording_deleted ON mh_pm_recording (deleted);
CREATE INDEX IX_mh_pm_recording_room ON mh_pm_recording (room);
CREATE INDEX IX_mh_pm_recording_start_date ON mh_pm_recording (start_date);
CREATE INDEX IX_mh_pm_recording_end_date ON mh_pm_recording (end_date);
CREATE INDEX IX_mh_pm_recording_blacklisted ON mh_pm_recording (blacklisted);
CREATE INDEX IX_mh_pm_recording_review_status ON mh_pm_recording (REVIEWSTATUS);
CREATE INDEX IX_mh_pm_recording_review_date ON mh_pm_recording (review_date);

CREATE TABLE mh_pm_recording_action (
  recording_id BIGINT NOT NULL,
  action_id BIGINT NOT NULL,
  PRIMARY KEY (recording_id,action_id),
  CONSTRAINT FK_mh_pm_recording_action_action_id FOREIGN KEY (action_id) REFERENCES mh_pm_action (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_pm_recording_action_recording_id FOREIGN KEY (recording_id) REFERENCES mh_pm_recording (id) ON DELETE CASCADE
);

CREATE TABLE mh_pm_recording_messages (
  recording_id BIGINT NOT NULL,
  message_id BIGINT NOT NULL,
  PRIMARY KEY (recording_id,message_id),
  CONSTRAINT FK_mh_pm_recording_messages_message_id FOREIGN KEY (message_id) REFERENCES mh_pm_message (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_pm_recording_messages_recording_id FOREIGN KEY (recording_id) REFERENCES mh_pm_recording (id) ON DELETE CASCADE
);

CREATE TABLE mh_pm_recording_participation (
  recording_id BIGINT NOT NULL,
  person_id BIGINT NOT NULL,
  PRIMARY KEY (recording_id,person_id),
  CONSTRAINT FK_mh_pm_recording_participation_person_id FOREIGN KEY (person_id) REFERENCES mh_pm_person (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_pm_recording_participation_recording_id FOREIGN KEY (recording_id) REFERENCES mh_pm_recording (id) ON DELETE CASCADE
);

CREATE TABLE mh_pm_recording_staff (
  recording_id BIGINT NOT NULL,
  person_id BIGINT NOT NULL,
  PRIMARY KEY (recording_id,person_id),
  CONSTRAINT FK_mh_pm_recording_staff_person_id FOREIGN KEY (person_id) REFERENCES mh_pm_person (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_pm_recording_staff_recording_id FOREIGN KEY (recording_id) REFERENCES mh_pm_recording (id) ON DELETE CASCADE
);

CREATE TABLE mh_pm_synchronization (
  id BIGINT NOT NULL,
  date DATETIME NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_pm_synchronization_0 UNIQUE (date)
);

CREATE INDEX IX_mh_pm_synchronization_date ON mh_pm_synchronization (date);

CREATE TABLE mh_pm_synchronized_recording (
  id BIGINT NOT NULL,
  STATUS VARCHAR(255) DEFAULT NULL,
  recording BIGINT DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT FK_mh_pm_synchronized_recording_recording FOREIGN KEY (recording) REFERENCES mh_pm_recording (id) ON DELETE CASCADE
);

CREATE TABLE mh_pm_synchronization_mh_pm_synchronized_recording (
  Synchronization_id BIGINT NOT NULL,
  synchronizedRecordings_id BIGINT NOT NULL,
  PRIMARY KEY (Synchronization_id,synchronizedRecordings_id),
  CONSTRAINT mhpmsynchrnztnmhpmsynchrnzdrcrdngsynchrnzdRcrdngsd FOREIGN KEY (synchronizedRecordings_id) REFERENCES mh_pm_synchronized_recording (id) ON DELETE CASCADE,
  CONSTRAINT mhpmsynchrnztnmhpmsynchronizedrecordingSynchrnztnd FOREIGN KEY (Synchronization_id) REFERENCES mh_pm_synchronization (id) ON DELETE CASCADE
);

CREATE TABLE mh_pm_blacklist_mh_pm_period (
  Blacklist_id BIGINT NOT NULL,
  periods_id BIGINT NOT NULL,
  PRIMARY KEY (Blacklist_id,periods_id),
  CONSTRAINT FK_mh_pm_blacklist_mh_pm_period_Blacklist_id FOREIGN KEY (Blacklist_id) REFERENCES mh_pm_blacklist (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_pm_blacklist_mh_pm_period_periods_id FOREIGN KEY (periods_id) REFERENCES mh_pm_period (id) ON DELETE CASCADE
);

CREATE TABLE mh_pm_message_signature (
  id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  creation_date DATETIME NOT NULL,
  sender VARCHAR(255) NOT NULL,
  sender_name VARCHAR(255) NOT NULL,
  reply_to VARCHAR(255) DEFAULT NULL,
  replay_to_name VARCHAR(255) DEFAULT NULL,
  signature VARCHAR(255) NOT NULL,
  creator BIGINT DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_pm_message_signature_0 UNIQUE (name),
  CONSTRAINT FK_mh_pm_message_signature_creator FOREIGN KEY (creator) REFERENCES mh_pm_person (id) ON DELETE CASCADE
);

CREATE INDEX IX_mh_pm_message_signature_name ON mh_pm_message_signature (name);

CREATE TABLE mh_pm_message_template (
  id BIGINT NOT NULL,
  body VARCHAR(MAX) NOT NULL,
  creation_date DATETIME NOT NULL,
  subject VARCHAR(255) NOT NULL,
  name VARCHAR(255) NOT NULL,
  TYPE VARCHAR(255) DEFAULT NULL,
  creator BIGINT DEFAULT NULL,
  signature BIGINT DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT UNQ_mh_pm_message_template_0 UNIQUE (name),
  CONSTRAINT FK_mh_pm_message_template_creator FOREIGN KEY (creator) REFERENCES mh_pm_person (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_pm_message_template_signature FOREIGN KEY (signature) REFERENCES mh_pm_message_signature (id)
);

CREATE INDEX IX_mh_pm_message_template_name ON mh_pm_message_template (name);

CREATE TABLE mh_pm_person_mh_pm_person_type (
  Person_id BIGINT NOT NULL,
  personTypes_id BIGINT NOT NULL,
  PRIMARY KEY (Person_id,personTypes_id),
  CONSTRAINT FK_mh_pm_person_mh_pm_person_type_personTypes_id FOREIGN KEY (personTypes_id) REFERENCES mh_pm_person_type (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_pm_person_mh_pm_person_type_Person_id FOREIGN KEY (Person_id) REFERENCES mh_pm_person (id) ON DELETE CASCADE
);

CREATE TABLE mh_pm_message_mh_pm_error (
  Message_id BIGINT NOT NULL,
  errors_id BIGINT NOT NULL,
  PRIMARY KEY (Message_id,errors_id),
  CONSTRAINT FK_mh_pm_message_mh_pm_error_errors_id FOREIGN KEY (errors_id) REFERENCES mh_pm_error (id) ON DELETE CASCADE,
  CONSTRAINT FK_mh_pm_message_mh_pm_error_Message_id FOREIGN KEY (Message_id) REFERENCES mh_pm_message (id) ON DELETE CASCADE
);

CREATE TABLE mh_pm_synchronization_mh_pm_error (
  Synchronization_id BIGINT NOT NULL,
  errors_id BIGINT NOT NULL,
  PRIMARY KEY (Synchronization_id,errors_id),
  CONSTRAINT FK_mh_pm_synchronization_mh_pm_error_errors_id FOREIGN KEY (errors_id) REFERENCES mh_pm_error (id) ON DELETE CASCADE,
  CONSTRAINT mhpm_synchronization_mh_pm_errorSynchronization_id FOREIGN KEY (Synchronization_id) REFERENCES mh_pm_synchronization (id) ON DELETE CASCADE
);