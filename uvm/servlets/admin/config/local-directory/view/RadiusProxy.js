Ext.define('Ung.config.local-directory.view.RadiusProxy', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-local-directory-radius-proxy',
    itemId: 'radius-proxy',
    title: 'RADIUS Proxy',
    scrollable: true,
    viewModel: true,

    bodyPadding: 10,

    items: [{
        xtype: 'component',
        margin: '0 0 10 0',
        html: 'The RADIUS Proxy can be enabled to allow wireless clients to authenticate using account credentials stored in an Active Directory server.'.t()
    },{
        xtype: 'checkbox',
        reference: 'activeProxy',
        padding: '5 0',
        boxLabel: 'Enable Active Directory Proxy'.t(),
        bind: {
            value: '{systemSettings.radiusProxyEnabled}',
            disabled: '{!systemSettings.radiusServerEnabled}'
        }
    }, {
        xtype: 'fieldset',
        padding: '10 20',
        itemId: 'radius-proxy',
        width: 600,
        title: 'Active Directory Server'.t(),
        items: [{
            xtype: 'textfield',
            fieldLabel: 'AD Server'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: false,
            bind: {
                value: '{systemSettings.radiusProxyServer}',
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'AD Workgroup'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: false,
            bind: {
                value: '{systemSettings.radiusProxyWorkgroup}',
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'AD Domain'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: false,
            bind: {
                value: '{systemSettings.radiusProxyRealm}',
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'AD Admin Username'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: false,
            bind: {
                value: '{systemSettings.radiusProxyUsername}',
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'AD Admin Password'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: false,
            bind: {
                value: '{systemSettings.radiusProxyPassword}',
                disabled: '{!activeProxy.checked}'
            }
        }],
    }, {
        xtype: 'fieldset',
        padding: '10 20',
        itemId: 'radius-account',
        width: 600,
        title: 'Active Directory Computer Account'.t(),
        items: [{
            xtype: 'button',
            iconCls: 'fa fa-link',
            text: 'Create AD Computer Account',
            margin: '10, 0',
            bind: {
                disabled: '{!systemSettings.radiusProxyEnabled}',
            },
            handler: 'createComputerAccount'
        }, {
            xtype: 'button',
            iconCls: 'fa fa-refresh',
            text: 'Refresh AD Account Status',
            margin: '10, 10',
            bind: {
                disabled: '{!systemSettings.radiusProxyEnabled}',
            },
            target: 'radiusProxyStatus',
            handler: 'refreshRadiusProxyStatus'
        }, {
            xtype: 'textarea',
            fieldLabel: 'AD Account Status'.t(),
            labelWidth: 120,
            width: '100%',
            allowBlank: true,
            readOnly: true,
            bind: {
                disabled: '{!activeProxy.checked}'
            }
        }]
    }, {
        xtype: 'fieldset',
        padding: '10 20',
        itemId: 'radius-test',
        width: 600,
        title: 'Active Directory Test'.t(),
        items: [{
            xtype: 'textfield',
            fieldLabel: 'Test Username'.t(),
            fieldIndex: 'testUsername',
            labelWidth: 120,
            width: '100%',
            allowBlank: true,
            _neverDirty: true,
            bind: {
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'Test Password'.t(),
            fieldIndex: 'testPassword',
            labelWidth: 120,
            width: '100%',
            allowBlank: true,
            _neverDirty: true,
            bind: {
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'textfield',
            fieldLabel: 'Test Domain'.t(),
            fieldIndex: 'testDomain',
            labelWidth: 120,
            width: '100%',
            allowBlank: true,
            _neverDirty: true,
            bind: {
                disabled: '{!activeProxy.checked}'
            }
        }, {
            xtype: 'button',
            iconCls: 'fa fa-cogs',
            text: 'Test Authentication',
            margin: '10, 10',
            bind: {
                disabled: '{!systemSettings.radiusProxyEnabled}',
            },
            handler: 'testRadiusProxyLogin'
        }]
    }]
});
