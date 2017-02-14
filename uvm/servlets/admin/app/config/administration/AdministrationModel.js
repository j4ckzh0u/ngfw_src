Ext.define('Ung.config.administration.AdministrationModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config.administration',

    data: {
        adminSettings: null,
        systemSettings: null,
        skinSettings: null,

        serverCertificates: null,
        rootCertificateInformation: null,
        serverCertificateVerification: null,
        skinsList: null
    },
    stores: {
        accounts: { data: '{adminSettings.users.list}' },
        certificates: { data: '{serverCertificates.list}' },
        skins: { data: '{skinsList.list}' }
    }
});