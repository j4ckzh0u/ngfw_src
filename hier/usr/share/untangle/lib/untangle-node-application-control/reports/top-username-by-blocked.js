{
    "uniqueId": "application-control-9cIZhLJzjx",
    "category": "Application Control",
    "conditions": [
        {
            "column": "application_control_blocked",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "=",
            "value": "true"
        }
    ],
    "description": "The number of blocked sessions grouped by username.",
    "displayOrder": 602,
    "enabled": true,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "orderByColumn": "value",
    "orderDesc": true,
    "units": "sessions",
    "pieGroupColumn": "username",
    "pieSumColumn": "count(*)",
    "readOnly": true,
    "table": "sessions",
    "title": "Top Blocked Usernames",
    "type": "PIE_GRAPH"
}
