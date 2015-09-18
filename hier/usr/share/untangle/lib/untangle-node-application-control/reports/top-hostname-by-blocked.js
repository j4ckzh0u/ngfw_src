{
    "uniqueId": "application-control-EgAcRJQCcV",
    "category": "Application Control",
    "conditions": [
        {
            "column": "application_control_blocked",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "description": "The number of blocked sessions grouped by hostname.",
    "displayOrder": 402,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "hostname",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Blocked Hostnames",
    "type": "PIE_GRAPH"
}
