-- convert script for release 2.5

-----------------------
-- create new schemas |
-----------------------

CREATE SCHEMA settings;
CREATE SCHEMA events;
SET search_path TO public,settings,events;

-----------------------------------
-- move old tables to new schemas |
-----------------------------------

CREATE SEQUENCE settings.hibernate_sequence;
SELECT setval('settings.hibernate_sequence', nextval('public.hibernate_sequence'));
DROP SEQUENCE public.hibernate_sequence;

-- admin_settings

CREATE TABLE settings.admin_settings (
    admin_settings_id,
    summary_period_id)
AS SELECT admin_settings_id, summary_period_id FROM public.admin_settings;

ALTER TABLE settings.admin_settings
    ADD CONSTRAINT admin_settings_pkey PRIMARY KEY (admin_settings_id);
ALTER TABLE settings.admin_settings
    ALTER COLUMN admin_settings_id
    SET NOT NULL;

-- mvvm_user

CREATE TABLE settings.mvvm_user (
    id,
    login,
    password,
    name,
    notes,
    send_alerts,
    admin_setting_id)
AS SELECT id, login, password, name, notes, send_alerts, admin_setting_id
   FROM public.mvvm_user;

ALTER TABLE settings.mvvm_user
    ADD CONSTRAINT mvvm_user_pkey PRIMARY KEY (id);
ALTER TABLE settings.mvvm_user
    ALTER COLUMN id SET NOT NULL;
ALTER TABLE settings.mvvm_user
    ALTER COLUMN login SET NOT NULL;
ALTER TABLE settings.mvvm_user
    ALTER COLUMN password SET NOT NULL;
ALTER TABLE settings.mvvm_user
    ALTER COLUMN name SET NOT NULL;

-- upgrade_settings

CREATE TABLE settings.upgrade_settings (
    upgrade_settings_id,
    auto_upgrade,
    period)
AS SELECT upgrade_settings_id, auto_upgrade, period
   FROM public.upgrade_settings;

ALTER TABLE settings.upgrade_settings
    ADD CONSTRAINT upgrade_settings_pkey PRIMARY KEY (upgrade_settings_id);
ALTER TABLE settings.upgrade_settings
    ALTER COLUMN upgrade_settings_id SET NOT NULL;
ALTER TABLE settings.upgrade_settings
    ALTER COLUMN auto_upgrade SET NOT NULL;
ALTER TABLE settings.upgrade_settings
    ALTER COLUMN period SET NOT NULL;

-- mail_settings

CREATE TABLE settings.mail_settings (
    mail_settings_id,
    report_email,
    smtp_host,
    from_address)
AS SELECT mail_settings_id, report_email, smtp_host, from_address
   FROM public.mail_settings;

ALTER TABLE settings.mail_settings
    ADD CONSTRAINT mail_settings_pkey PRIMARY KEY (mail_settings_id);
ALTER TABLE settings.mail_settings
    ALTER COLUMN mail_settings_id SET NOT NULL;

-- transform_args

CREATE TABLE settings.transform_args (
    tps_id,
    arg,
    position)
AS SELECT tps_id, arg, position FROM public.transform_args;

ALTER TABLE settings.transform_args
    ADD CONSTRAINT transform_args_pkey PRIMARY KEY (tps_id);
ALTER TABLE settings.transform_args
    ALTER COLUMN tps_id SET NOT NULL;
ALTER TABLE settings.transform_args
    ALTER COLUMN arg SET NOT NULL;
ALTER TABLE settings.transform_args
    ALTER COLUMN position SET NOT NULL;

-- transform_manager_state

CREATE TABLE settings.transform_manager_state (
    id,
    last_tid)
AS SELECT id, last_tid FROM public.transform_manager_state;

ALTER TABLE settings.transform_manager_state
    ADD CONSTRAINT transform_manager_state_pkey PRIMARY KEY (id);
ALTER TABLE settings.transform_manager_state
    ALTER COLUMN id SET NOT NULL;

-- uri_rule

CREATE TABLE settings.uri_rule (
    rule_id,
    uri,
    name,
    category,
    description,
    live,
    alert,
    log)
AS SELECT rule_id, uri, name, category, description, live, alert, log
   FROM public.uri_rule;

ALTER TABLE settings.uri_rule
    ADD CONSTRAINT uri_rule_pkey PRIMARY KEY (rule_id);
ALTER TABLE settings.uri_rule
    ALTER COLUMN rule_id SET NOT NULL;

-- period

CREATE TABLE settings.period (
    period_id,
    hour,
    minute,
    sunday,
    monday,
    tuesday,
    wednesday,
    thursday,
    friday,
    saturday)
AS SELECT period_id, hour, minute, sunday, monday, tuesday, wednesday,
          thursday, friday, saturday
   FROM public.period;

ALTER TABLE settings.period
    ADD CONSTRAINT period_pkey PRIMARY KEY (period_id);
ALTER TABLE settings.period
    ALTER COLUMN period_id SET NOT NULL;
ALTER TABLE settings.period
    ALTER COLUMN hour SET NOT NULL;
ALTER TABLE settings.period
    ALTER COLUMN minute SET NOT NULL;

-- transform_preferences

CREATE TABLE settings.transform_preferences (
    id,
    tid,
    red,
    green,
    blue,
    alpha)
AS SELECT id, tid, red, green, blue, alpha
   FROM public.transform_preferences;

ALTER TABLE settings.transform_preferences
    ADD CONSTRAINT transform_preferences_pkey PRIMARY KEY (id);
ALTER TABLE settings.transform_preferences
    ALTER COLUMN id SET NOT NULL;

-- string_rule

CREATE TABLE settings.string_rule (
    rule_id,
    string,
    name,
    category,
    description,
    live,
    alert,
    log)
AS SELECT rule_id, string, name, category, description, live, alert, log
   FROM public.string_rule;

ALTER TABLE settings.string_rule
    ADD CONSTRAINT string_rule_pkey PRIMARY KEY (rule_id);
ALTER TABLE settings.string_rule
    ALTER COLUMN rule_id SET NOT NULL;

-- tid

CREATE TABLE settings.tid (
    id)
AS SELECT id FROM public.tid;

ALTER TABLE settings.tid
    ADD CONSTRAINT tid_pkey PRIMARY KEY (id);
ALTER TABLE settings.tid
    ALTER COLUMN id SET NOT NULL;

-- rule

CREATE TABLE settings.rule (
    rule_id,
    name,
    category,
    description,
    live,
    alert,
    log)
AS SELECT rule_id, name, category, description, live, alert, log
   FROM public.rule;

ALTER TABLE settings.rule
    ADD CONSTRAINT rule_pkey PRIMARY KEY (rule_id);
ALTER TABLE settings.rule
    ALTER COLUMN rule_id SET NOT NULL;

-- transform_persistent_state

CREATE TABLE settings.transform_persistent_state (
    id,
    name,
    tid,
    public_key,
    target_state)
AS SELECT id, name, tid, public_key, target_state
   FROM public.transform_persistent_state;

ALTER TABLE settings.transform_persistent_state
    ADD CONSTRAINT transform_persistent_state_pkey PRIMARY KEY (id);
ALTER TABLE settings.transform_persistent_state
    ALTER COLUMN id SET NOT NULL;
ALTER TABLE settings.transform_persistent_state
    ALTER COLUMN name SET NOT NULL;
ALTER TABLE settings.transform_persistent_state
    ALTER COLUMN public_key SET NOT NULL;
ALTER TABLE settings.transform_persistent_state
    ALTER COLUMN target_state SET NOT NULL;

-- ipmaddr_dir

CREATE TABLE settings.ipmaddr_dir (
    id,
    notes)
AS SELECT id, notes FROM public.ipmaddr_dir;

ALTER TABLE settings.ipmaddr_dir
    ADD CONSTRAINT ipmaddr_dir_pkey PRIMARY KEY (id);
ALTER TABLE settings.ipmaddr_dir
    ALTER COLUMN id SET NOT NULL;

-- mimetype_rule

CREATE TABLE settings.mimetype_rule (
    rule_id,
    mime_type,
    name,
    category,
    description,
    live,
    alert,
    log)
AS SELECT rule_id, mime_type, name, category, description, live, alert, log
   FROM public.mimetype_rule;

ALTER TABLE settings.mimetype_rule
    ADD CONSTRAINT mimetype_rule_pkey PRIMARY KEY (rule_id);
ALTER TABLE settings.mimetype_rule
    ALTER COLUMN rule_id SET NOT NULL;

-- ipmaddr_dir_entries

CREATE TABLE settings.ipmaddr_dir_entries (
    ipmaddr_dir_id,
    rule_id,
    position)
AS SELECT ipmaddr_dir_id, rule_id, position
   FROM public.ipmaddr_dir_entries;

ALTER TABLE settings.ipmaddr_dir_entries
    ADD CONSTRAINT ipmaddr_dir_entries_pkey PRIMARY KEY (ipmaddr_dir_id, position);
ALTER TABLE settings.ipmaddr_dir_entries
    ALTER COLUMN ipmaddr_dir_id SET NOT NULL;
ALTER TABLE settings.ipmaddr_dir_entries
    ALTER COLUMN rule_id SET NOT NULL;
ALTER TABLE settings.ipmaddr_dir_entries
    ALTER COLUMN position SET NOT NULL;

-- ipmaddr_rule

CREATE TABLE settings.ipmaddr_rule (
    rule_id,
    ipmaddr,
    name,
    category,
    description,
    live,
    alert,
    log)
AS SELECT rule_id, ipmaddr, name, category, description, live, alert, log
   FROM public.ipmaddr_rule;

ALTER TABLE settings.ipmaddr_rule
    ADD CONSTRAINT ipmaddr_rule_pkey PRIMARY KEY (rule_id);
ALTER TABLE settings.ipmaddr_rule
    ALTER COLUMN rule_id SET NOT NULL;

-- mvvm_login_evt

CREATE TABLE events.mvvm_login_evt (
    event_id,
    client_addr,
    login,
    local,
    succeeded,
    reason,
    time_stamp)
AS SELECT event_id, client_addr, login, local, succeeded, reason, time_stamp
   FROM public.mvvm_login_evt;

ALTER TABLE events.mvvm_login_evt
    ADD CONSTRAINT mvvm_login_evt_pkey PRIMARY KEY (event_id);
ALTER TABLE events.mvvm_login_evt
    ALTER COLUMN event_id SET NOT NULL;

-------------------------
-- recreate constraints |
-------------------------

-- indeces

CREATE INDEX idx_string_rule ON settings.string_rule (string);

-- foreign key constraints

ALTER TABLE settings.admin_settings
    ADD CONSTRAINT fk_admin_settings
    FOREIGN KEY (summary_period_id) REFERENCES settings.period;

ALTER TABLE settings.mvvm_user
    ADD CONSTRAINT fk_mvvm_user
    FOREIGN KEY (admin_setting_id) REFERENCES settings.admin_settings;

ALTER TABLE settings.upgrade_settings
    ADD CONSTRAINT fk_upgrade_settings
    FOREIGN KEY (period) REFERENCES settings.period;

ALTER TABLE settings.transform_args
    ADD CONSTRAINT fk_transform_args
    FOREIGN KEY (tps_id) REFERENCES settings.transform_persistent_state;

ALTER TABLE settings.transform_preferences
    ADD CONSTRAINT fk_transform_preferences
    FOREIGN KEY (tid) REFERENCES settings.tid;

ALTER TABLE settings.transform_persistent_state
    ADD CONSTRAINT fk_transform_persistent_state
    FOREIGN KEY (tid) REFERENCES settings.tid;

ALTER TABLE settings.ipmaddr_dir_entries
    ADD CONSTRAINT fk_ipmaddr_dir_entries
    FOREIGN KEY (ipmaddr_dir_id) REFERENCES settings.ipmaddr_dir;

-------------------------
-- drop old constraints |
-------------------------

-- indeces

DROP INDEX public.idx_string_rule;

-- foreign key constraints

ALTER TABLE public.admin_settings
    DROP CONSTRAINT fk71b1f7333c031ee0;
ALTER TABLE public.mvvm_user
    DROP CONSTRAINT fkcc5a228acd112c9a;
ALTER TABLE public.upgrade_settings
    DROP CONSTRAINT fk4dc4f2e68c7669c1;
ALTER TABLE public.transform_args
    DROP CONSTRAINT fk1c0835f0a8a3b796;
ALTER TABLE public.transform_preferences
    DROP CONSTRAINT fke8b6ba651446f;
ALTER TABLE public.transform_persistent_state
    drop CONSTRAINT fka67b855c1446f;
ALTER TABLE public.ipmaddr_dir_entries
    DROP CONSTRAINT fkc67de356b5257e75;
ALTER TABLE public.ipmaddr_dir_entries
    DROP CONSTRAINT fkc67de356871aad3e;

-------------------------------------
-- drop constraints from transforms |
-------------------------------------

-- string rule references

ALTER TABLE tr_httpblk_extensions DROP CONSTRAINT fkbc81fbbb871aad3e;
ALTER TABLE tr_spyware_cr DROP CONSTRAINT fkf0bdcb6871aad3e;
ALTER TABLE tr_spyware_ar DROP CONSTRAINT fkf0bdc78871aad3e;
ALTER TABLE tr_httpblk_blocked_urls DROP CONSTRAINT fk804e415e871aad3e;
ALTER TABLE tr_httpblk_passed_urls DROP CONSTRAINT fk6c8c0c8c871aad3e;

-- tid references

ALTER TABLE tr_nat_settings DROP CONSTRAINT fk2f819dc21446f;
ALTER TABLE tr_virus_settings DROP CONSTRAINT fk98f4cb261446f;
ALTER TABLE tr_airgap_settings DROP CONSTRAINT fk7b2ca9f51446f;
ALTER TABLE tr_firewall_settings DROP CONSTRAINT fk23cda1011446f;
ALTER TABLE tr_protofilter_settings DROP CONSTRAINT fk55f095631446f;
ALTER TABLE tr_spyware_settings DROP CONSTRAINT fk33dfef2a1446f;
ALTER TABLE tr_reporting_settings DROP CONSTRAINT fk85b76951446f;
ALTER TABLE tr_email_settings DROP CONSTRAINT fk27c00d671446f;
ALTER TABLE tr_httpblk_settings DROP CONSTRAINT fk3f2d0d8a1446f;

-- ipmaddr_dir references

ALTER TABLE tr_reporting_settings DROP CONSTRAINT fk85b769562c0555c;

-- mimetype_rule

ALTER TABLE tr_httpblk_mime_types DROP CONSTRAINT fkf4ba8c35871aad3e;

-- ipmaddr_rule

ALTER TABLE tr_spyware_sr DROP CONSTRAINT fkf0bdea6871aad3e;
ALTER TABLE tr_httpblk_passed_clients DROP CONSTRAINT fkfb0b6540871aad3e;

--------------------
-- drop old tables |
--------------------

DROP TABLE public.admin_settings;
DROP TABLE public.mvvm_user;
DROP TABLE public.upgrade_settings;
DROP TABLE public.mail_settings;
DROP TABLE public.transform_args;
DROP TABLE public.transform_manager_state;
DROP TABLE public.uri_rule;
DROP TABLE public.period;
DROP TABLE public.transform_preferences;
DROP TABLE public.string_rule;
DROP TABLE public.tid;
DROP TABLE public.rule;
DROP TABLE public.transform_persistent_state;
DROP TABLE public.ipmaddr_dir;
DROP TABLE public.mimetype_rule;
DROP TABLE public.ipmaddr_dir_entries;
DROP TABLE public.ipmaddr_rule;
DROP TABLE public.mvvm_login_evt;

---------------
-- new tables |
---------------

-- shield rejection events

CREATE TABLE events.shield_rejection_evt (
    event_id int8 NOT NULL,
    ip inet,
    reputation float8,
    mode int4,
    limited int4,
    rejected int4,
    dropped int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- shield statistics

CREATE TABLE events.shield_statistic_evt (
    event_id int8 NOT NULL,
    accepted int4,
    limited  int4,
    dropped  int4,
    rejected int4,
    relaxed  int4,
    lax      int4,
    tight    int4,
    closed   int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

-- split pipeline info into pl_endp and pl_stats

DROP TABLE events.pl_endp;
DROP TABLE events.pl_stats;

CREATE TABLE events.pl_endp (
    event_id,
    time_stamp,
    session_id,
    proto,
    create_date,
    client_intf,
    server_intf,
    c_client_addr,
    s_client_addr,
    c_server_addr,
    s_server_addr,
    c_client_port,
    s_client_port,
    c_server_port,
    s_server_port)
AS SELECT id, now(), session_id, proto, create_date, client_intf,
          server_intf, c_client_addr, s_client_addr, c_server_addr,
          s_server_addr, c_client_port, s_client_port, c_server_port,
          s_server_port
   FROM pipeline_info;

ALTER TABLE pl_endp
    ADD CONSTRAINT pl_endp_pkey PRIMARY KEY (event_id);
ALTER TABLE pl_endp
    ALTER COLUMN event_id SET NOT NULL;

CREATE TABLE events.pl_stats (
    event_id,
    time_stamp,
    session_id,
    raze_date,
    c2p_bytes,
    s2p_bytes,
    p2c_bytes,
    p2s_bytes,
    c2p_chunks,
    s2p_chunks,
    p2c_chunks,
    p2s_chunks)
AS SELECT nextval('hibernate_sequence'), now(), session_id, raze_date,
          c2p_bytes, s2p_bytes, p2c_bytes, p2s_bytes, c2p_chunks,
          s2p_chunks, p2c_chunks, p2s_chunks
   FROM pipeline_info;

ALTER TABLE pl_stats
    ADD CONSTRAINT pl_stats_pkey PRIMARY KEY (event_id);
ALTER TABLE pl_stats ALTER COLUMN event_id SET NOT NULL;

CREATE INDEX pl_endp_sid ON events.pl_endp (session_id);
CREATE INDEX pl_endp_cdate_idx ON events.pl_endp (create_date);

DROP TABLE public.pipeline_info;
DROP TABLE public.mvvm_evt_pipeline;

-- Table for shield events

CREATE TABLE events.shield_rejection_evt (
        event_id int8 NOT NULL,
        ip inet,
        reputation float8,
        mode int4,
        limited int4,
        rejected int4,
        dropped int4,
        time_stamp timestamp,
        PRIMARY KEY (event_id));

-- Table for shield statistics

CREATE TABLE events.shield_statistic_evt (
    event_id int8 NOT NULL,
    accepted int4,
    limited  int4,
    dropped  int4,
    rejected int4,
    relaxed  int4,
    lax      int4,
    tight    int4,
    closed   int4,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

VACUUM ANALYZE;