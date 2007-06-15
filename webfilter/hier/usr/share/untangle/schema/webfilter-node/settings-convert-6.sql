-- settings convert for release 4.0
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

ALTER TABLE settings.tr_httpblk_settings ADD COLUMN fascist_mode bool;
UPDATE settings.tr_httpblk_settings SET fascist_mode = false;
ALTER TABLE settings.tr_httpblk_settings ALTER COLUMN fascist_mode SET NOT NULL;

ALTER TABLE settings.tr_httpblk_blcat ADD COLUMN log_only bool;
UPDATE settings.tr_httpblk_blcat SET log_only = false;
ALTER TABLE settings.tr_httpblk_blcat ALTER COLUMN log_only SET NOT NULL;

ALTER TABLE settings.tr_httpblk_blcat ALTER COLUMN block_domains SET NOT NULL;
ALTER TABLE settings.tr_httpblk_blcat ALTER COLUMN block_urls SET NOT NULL;
ALTER TABLE settings.tr_httpblk_blcat ALTER COLUMN block_expressions SET NOT NULL;
