/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

#ifndef _JMVUTIL_ERROR_H_
#define _JMVUTIL_ERROR_H_

#include "jmvutil.h"

#define THROW_MSG_MAX_LEN 64

typedef struct {
    int current;
    char buffer[THROW_MSG_COUNT][THROW_MSG_MAX_LEN];
} jerror_tls_t;

int _jmvutil_error_init();

int _jmvutil_error_tls_init( jerror_tls_t* tls );

#endif 
