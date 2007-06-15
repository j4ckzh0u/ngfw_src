-- schema for release 1.4a
-- $HeadURL$
-- Copyright (c) 2003-2007 Untangle, Inc. 
--
-- This program is free software; you can redistribute it and/or modify
-- it under the terms of the GNU General Public License, version 2,
-- as published by the Free Software Foundation.
--
-- This program is distributed in the hope that it will be useful, but
-- AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
-- NONINFRINGEMENT.  See the GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program; if not, write to the Free Software
-- Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
--

CREATE TABLE tr_spyware_evt_activex (
    event_id int8 NOT NULL,
    session_id int4,
    request_id int8,
    ident varchar(255),
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE tr_spyware_settings (
    settings_id int8 NOT NULL,
    tid int8 NOT NULL UNIQUE,
    activex_enabled bool,
    cookie_enabled bool,
    spyware_enabled bool,
    block_all_activex bool,
    activex_details varchar(255),
    cookie_details varchar(255),
    spyware_details varchar(255),
    block_all_activex_details varchar(255),
    PRIMARY KEY (settings_id));

CREATE TABLE tr_spyware_cr (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

CREATE TABLE tr_spyware_evt_access (
    event_id int8 NOT NULL,
    session_id int4,
    request_id int8,
    ipmaddr inet,
    ident varchar(255),
    blocked bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

CREATE TABLE tr_spyware_ar (
    setting_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (setting_id, position));

CREATE TABLE tr_spyware_sr (
    settings_id int8 NOT NULL,
    rule_id int8 NOT NULL,
    position int4 NOT NULL,
    PRIMARY KEY (settings_id, position));

CREATE TABLE tr_spyware_evt_cookie (
    event_id int8 NOT NULL,
    session_id int4,
    request_id int8,
    ident varchar(255),
    to_server bool,
    time_stamp timestamp,
    PRIMARY KEY (event_id));

ALTER TABLE tr_spyware_ar ADD CONSTRAINT FKF0BDC78871AAD3E FOREIGN KEY (rule_id) REFERENCES string_rule;

ALTER TABLE tr_spyware_ar ADD CONSTRAINT FKF0BDC781CAE658A FOREIGN KEY (setting_id) REFERENCES tr_spyware_settings;

ALTER TABLE tr_spyware_settings ADD CONSTRAINT FK33DFEF2A1446F FOREIGN KEY (tid) REFERENCES tid;

ALTER TABLE tr_spyware_cr ADD CONSTRAINT FKF0BDCB61CAE658A FOREIGN KEY (setting_id) REFERENCES tr_spyware_settings;

ALTER TABLE tr_spyware_cr ADD CONSTRAINT FKF0BDCB6871AAD3E FOREIGN KEY (rule_id) REFERENCES string_rule;

ALTER TABLE tr_spyware_sr ADD CONSTRAINT FKF0BDEA6871AAD3E FOREIGN KEY (rule_id) REFERENCES ipmaddr_rule;

ALTER TABLE tr_spyware_sr ADD CONSTRAINT FKF0BDEA679192AB7 FOREIGN KEY (settings_id) REFERENCES tr_spyware_settings;
