'use strict';

var loMod = angular.module('loApp.services', []).value('version', '0.1');

/*
  FileReader service, taken from:
  http://odetocode.com/blogs/scott/archive/2013/07/03/building-a-filereader-service-for-angularjs-the-service.aspx
 */
loMod.factory('FileReader', function($q) {
  var onLoad = function(reader, deferred, scope) {
    return function () {
      scope.$apply(function () {
        deferred.resolve(reader.result);
      });
    };
  };

  var onError = function (reader, deferred, scope) {
    return function () {
      scope.$apply(function () {
        deferred.reject(reader.result);
      });
    };
  };

  var onProgress = function(reader, scope) {
    return function (event) {
      scope.$broadcast('fileProgress',
        {
          total: event.total,
          loaded: event.loaded
        });
    };
  };

  var getReader = function(deferred, scope) {
    var reader = new FileReader();
    reader.onload = onLoad(reader, deferred, scope);
    reader.onerror = onError(reader, deferred, scope);
    reader.onprogress = onProgress(reader, scope);
    return reader;
  };

  var readAsDataURL = function (file, scope) {
    var deferred = $q.defer();

    var reader = getReader(deferred, scope);
    reader.readAsText(file);

    return deferred.promise;
  };

  return {
    readAsDataUrl: readAsDataURL
  };
});

/* Loaders - Loaders are used in the route configuration as resolve parameters */
loMod.factory('Loader', function($q) {
  var loader = {};
  var methods = [ 'get', 'getList', 'query'];

  angular.forEach(methods, function(method){
    loader[method] = function(service, id) {
      return function() {
        var i = id && id();
        var delay = $q.defer();
        service[method](i, function(entry) {
          // If this is loResource, put loaded data into $live property and make it available for callback methods updates
          if (service.$live){
            service.$live = entry;
            entry.$live = service.$live;
          }
          delay.resolve(entry);
        }, function() {
          delay.reject('Unable to fetch ' + i);
        });

        return delay.promise;
      };
    };
  });

  return loader;
});

loMod.factory('LoStorage', function($resource) {
  return $resource('/admin/applications/:appId/resources/:storageId', {
    appId : '@appId',
    storageId : '@storageId'
  }, {
    get : {
      method : 'GET'
    },
    getList : {
      method : 'GET',
      params: { fields : '*(*)' }
    },
    create : {
      method : 'POST',
      params : { appId : '@appId'}
    },
    update : {
      method : 'PUT',
      params : { appId : '@appId', storageId : '@storageId'}
    },
    delete : {
      method : 'DELETE',
      params : { appId : '@appId', storageId : '@storageId'}
    }
  });
});

loMod.factory('LoCollection', function($resource) {
  return $resource('/:appId/:storageId/:collectionId?fields=*(*)', {
    appId : '@appId',
    storageId : '@storageId',
    collectionId : '@collectionId'
  }, {
    get : {
      method : 'GET',
      params: { appId : '@appId', storageId : '@storageId', collectionId: '@collectionId'}
    },
    getList : {
      method : 'GET'
    },
    create : {
      method : 'POST',
      params : { appId : '@appId', storageId : '@storageId'}
    },
    update : {
      method : 'PUT',
      params : { appId : '@appId', storageId : '@storageId', collectionId: '@collectionId'}
    },
    delete : {
      method : 'DELETE',
      params : { appId : '@appId', storageId : '@storageId', collectionId: '@collectionId'}
    }
  });
});

loMod.factory('LoCollectionItem', function($resource) {
  return $resource('/:appId/:storageId/:collectionId/:itemId', {
    appId : '@appId',
    storageId : '@storageId',
    collectionId : '@collectionId'
  }, {
    get : {
      method : 'GET',
      params: { appId : '@appId', storageId : '@storageId', collectionId: '@collectionId', itemId: '@itemId' }
    },
    getList : {
      method : 'GET',
      params: { appId : '@appId', storageId : '@storageId', collectionId: '@collectionId', fields : '*(*)' }
    },
    create : {
      method : 'POST',
      params : { appId : '@appId', storageId : '@storageId', collectionId: '@collectionId'}
    },
    update : {
      method : 'PUT',
      params : { appId : '@appId', storageId : '@storageId', collectionId: '@collectionId', itemId: '@itemId'}
    },
    delete : {
      method : 'DELETE',
      params : { appId : '@appId', storageId : '@storageId', collectionId: '@collectionId', itemId: '@itemid'}
    }
  });
});

loMod.factory('LoApp', function($resource) {
  return $resource('/admin/applications/:appId', {
    appId : '@appId'
  }, {
    get : {
      method : 'GET'
    },
    getList : {
      method : 'GET',
      params: { fields : '*(*)' }
    },
    create : {
      method : 'POST',
      url: '/admin/applications/'
    },
    save : {
      method : 'PUT',
      url: '/admin/applications/:appId'
    },
    addResource : {
      method : 'PUT',
      url: '/admin/applications/:appId/resources/:resourceId'
    }
  });
});

loMod.factory('loSubscribe', function($resource, LiveOak, $rootScope) {
  return function(url, callback){

    var _callback = function(){
      LiveOak.subscribe(url + '', function (data, action) {
        $rootScope.$apply(function(){
          callback[action](data);
        });
      });
    };

    LiveOak.auth.updateToken(5).success(function() {
      LiveOak.connect('Bearer', LiveOak.auth.token, _callback);
    }).error(function() {
      LiveOak.connect(_callback);
    });
  };
});

loMod.factory('loResource', function(loSubscribe){
  return function(res, url, callback) {
    res.$live = {};
    loSubscribe(url, callback);
    return res;
  };
});

loMod.factory('LoLiveAppListLoader', function(Loader, LoLiveAppList) {
  return Loader.getList(LoLiveAppList);
});

loMod.factory('LoLiveAppList', function($resource, LiveOak, $rootScope, loResource) {

  var url = '/admin/applications/';

  var res = $resource(url, {}, {
    getList: {
      method: 'GET',
      params: { fields: '*(*)' }
    }
  });

  var loRes = loResource(res, url, {
      create: function (data) {
        if(!res.$live.members){
          res.$live.members = [];
        }
        res.$live.members.push(data);
      },
      delete: function (data) {
        if(!res.$live.members){
          return;
        }

        for(var i = 0; i < res.$live.members.length; i++){
          if (data.id === res.$live.members[i].id) {
            res.$live.members.splice(i, 1);
            break;
          }
        }
      }
    });

  return loRes;
});

loMod.factory('LoStorageLoader', function(Loader, LoStorage, $route) {
  return Loader.get(LoStorage, function() {
    return {
      appId : $route.current.params.appId,
      storageId: $route.current.params.storageId
    };
  });
});

loMod.factory('LoStorageListLoader', function(Loader, LoStorage, $route) {
  return Loader.getList(LoStorage, function() {
    return {
      appId : $route.current.params.appId
    };
  });
});

loMod.factory('LoCollectionListLoader', function(Loader, LoCollection, $route) {
  return Loader.get(LoCollection, function() {
    return {
      appId : $route.current.params.appId,
      storageId : $route.current.params.storageId
    };
  });
});

loMod.factory('LoPushLoader', function(Loader, LoPush, $route) {
  return Loader.get(LoPush, function() {
      return {
        appId : $route.current.params.appId
      };
    },
    function(httpResponse) {
      console.log(httpResponse);
      return {
        appId : $route.current.params.appId
      };
    }
  );
});

loMod.factory('LoAppLoader', function(Loader, LoApp, $route) {
  return Loader.get(LoApp, function() {
    return {
      appId : $route.current.params.appId
    };
  });
});

loMod.factory('LoAppListLoader', function(Loader, LoApp) {
  return Loader.getList(LoApp);
});

loMod.factory('LoCollectionLoader', function(Loader, LoCollection) {
  return Loader.get(LoCollection);
});

loMod.factory('LoPush', function($resource) {
  return $resource('/admin/applications/:appId/resources/push', {
    appId : '@appId'
  }, {
    get : {
      method : 'GET',
      params : { appId : '@appId'}
    },
    update : {
      method : 'PUT',
      params : { appId : '@appId'}
    },
    create: {
      method : 'POST',
      url: '/admin/applications/:appId/resources/',
      params : { appId : '@appId'}
    },
    delete : {
      method : 'DELETE',
      params : { appId : '@appId'}
    }
  });
});

loMod.factory('LoRealmApp', function($resource, LiveOak) {
  return $resource(LiveOak.getAuthServerUrl() + '/admin/realms/:realmId/applications/:appId', {
    realmId : 'liveoak-apps',
    appId: '@appId'
  }, {
    save: {
      method: 'PUT'
    },
    create: {
      method: 'POST'
    },
    delete: {
      method: 'DELETE'
    }
  });
});

loMod.factory('LoRealmAppRoles', function($resource, LiveOak) {
  return $resource(LiveOak.getAuthServerUrl() + '/admin/realms/:realmId/applications/:appId/roles/:roleName', {
    realmId : 'liveoak-apps',
    appId: '@appId',
    roleName: '@roleName'
  });
});

loMod.factory('LoRealmRoles', function($resource, LiveOak) {
  return $resource(LiveOak.getAuthServerUrl() + '/admin/realms/:realmId/roles', {
    realmId : 'liveoak-apps'
  });
});

loMod.factory('LoRealmClientRoles', function($resource, LiveOak) {
  return $resource(LiveOak.getAuthServerUrl() + '/admin/realms/:realmId/applications/:appId/scope-mappings/realm', {
    realmId: 'liveoak-apps',
    appId: '@appId'
  });
});

loMod.factory('LoRealmAppClientScopeMapping', function($resource, LiveOak) {
  return $resource(LiveOak.getAuthServerUrl() + '/admin/realms/:realmId/applications/:clientId/scope-mappings/applications/:appId', {
    realmId: 'liveoak-apps',
    appId : '@appId',
    clientId : '@clientId'
  });
});

loMod.factory('LoRealmAppClientScopeMappingLoader', function(Loader, LoRealmAppClientScopeMapping, $route) {
  return Loader.query(LoRealmAppClientScopeMapping, function() {
    return {
      realmId: 'liveoak-apps',
      appId: $route.current.params.appId,
      clientId: $route.current.params.clientId
    };
  });
});

loMod.factory('LoRealmAppLoader', function(Loader, LoRealmApp, $route) {
  return Loader.get(LoRealmApp, function() {
    return {
      realmId: 'liveoak-apps',
      appId : $route.current.params.appId
    };
  });
});

loMod.factory('LoRealmRolesLoader', function(Loader, LoRealmRoles) {
  return Loader.query(LoRealmRoles, function() {
    return {
      realmId: 'liveoak-apps'
    };
  });
});

loMod.factory('LoRealmAppListLoader', function(Loader, LoRealmApp) {
  return Loader.query(LoRealmApp, function() {
    return {
      realmId: 'liveoak-apps'
    };
  });
});

loMod.factory('LoRealmAppRolesLoader', function(Loader, LoRealmAppRoles, $route) {
  return Loader.query(LoRealmAppRoles, function() {
    return {
      realmId: 'liveoak-apps',
      appId : $route.current.params.appId
    };
  });
});

loMod.factory('LoRealmClientRolesLoader', function(Loader, LoRealmClientRoles, $route) {
  return Loader.query(LoRealmClientRoles, function() {
    return {
      realmId: 'liveoak-apps',
      appId : $route.current.params.appId
    };
  });
});


loMod.factory('LoSecurityCollections', function($resource) {
  return $resource('/:appId', {
    appId : '@appId'
  }, {
    get : {
      method: 'GET',
      params: { fields : '*(*)' }
    }
  });
});

loMod.factory('LoSecurityCollectionsLoader', function(Loader, LoSecurityCollections, $route) {
  return Loader.get(LoSecurityCollections, function() {
    return {
      appId : $route.current.params.appId
    };
  });
});

loMod.factory('LoSecurity', function($resource) {
  return $resource('/admin/applications/:appId/resources/uri-policy', {
    appId : '@appId'
  }, {
    create : {
      method: 'PUT'
    },
    save : {
      method: 'PUT'
    }
  });
});

loMod.factory('LoSecurityLoader', function(Loader, LoSecurity, $route) {
  return Loader.get(LoSecurity, function() {
    return {
      appId : $route.current.params.appId
    };
  });
});

loMod.factory('LoACL', function($resource) {
  return $resource('/admin/applications/:appId/resources/acl-policy', {
    appId : '@appId'
  }, {
    create : {
      method: 'PUT'
    },
    save : {
      method: 'PUT'
    }
  });
});

loMod.factory('LoACLLoader', function(Loader, LoACL, $route) {
  return Loader.get(LoACL, function() {
    return {
      appId : $route.current.params.appId
    };
  });
});

loMod.factory('LoRealmUsers', function($resource, LiveOak) {
  return $resource(LiveOak.getAuthServerUrl() + '/admin/realms/:realmId/users/:userId', {
    realmId : 'liveoak-apps',
    userId : '@userId'
  }, {
    resetPassword : {
      method: 'PUT',
      url: LiveOak.getAuthServerUrl() + '/admin/realms/:realmId/users/:userId/reset-password'
    },
    addRoles : {
      method: 'POST',
      url: LiveOak.getAuthServerUrl() + '/admin/realms/:realmId/users/:userId/role-mappings/applications/:appId'
    },
    deleteRoles : {
      method: 'DELETE',
      url: LiveOak.getAuthServerUrl() + '/admin/realms/:realmId/users/:userId/role-mappings/applications/:appId'
    },
    getRoles: {
      method: 'GET',
      url: LiveOak.getAuthServerUrl() + '/admin/realms/:realmId/users/:userId/role-mappings/applications/:appId/composite',
      isArray: true
    },
    update: {
      method: 'PUT'
    }
  });
});

loMod.factory('LoRealmUserLoader', function(Loader, LoRealmUsers, $route) {
  return Loader.get(LoRealmUsers, function() {
    return {
      userId : $route.current.params.userId
    };
  });
});

loMod.factory('LoAppExamples', function($resource) {
  return $resource('/admin/console/resources/example-applications.json',
    {},
    {
      get: {
        method: 'GET',
        url: '/admin/console/resources/liveoak-examples/:parentId/:exampleId/application.json'
      },
      install: {
        method : 'POST',
        url: '/admin/applications/',
        headers: {
          'Content-Type':'application/vnd.liveoak.local-app+json'
        }
      }
    });
});

loMod.factory('LoClient', function($resource) {
  return $resource('/admin/applications/:appId/resources/application-clients/:clientId', {
    appId : '@appId',
    clientId: '@clientId'
  }, {
    get : {
      method : 'GET'
    },
    getList : {
      method : 'GET',
      params : { fields: '*(*)' }
    },
    update : {
      method : 'PUT'
    },
    create : {
      method : 'POST',
      params : { appId : '@appId' }
    },
    delete : {
      method : 'DELETE'
    }
  });
});

loMod.factory('LoBusinessLogicScripts', function($resource) {
  return $resource('/admin/applications/:appId/resources/scripts', {
    appId : '@appId',
    clientId: '@clientId'
  }, {
    get : {
      method : 'GET',
      params: { fields : '*(*)' }
    },
    getTriggered: {
      method: 'GET',
      url: '/admin/applications/:appId/resources/scripts/resource-triggered-scripts',
      params: { fields : '*(*)' }
    },
    getEndpoint: {
      method: 'GET',
      url: '/admin/applications/:appId/resources/scripts/endpoint-scripts/:scriptId',
      params: { fields : '*(*)' }
    },
    getScheduled: {
      method: 'GET',
      url: '/admin/applications/:appId/resources/scripts/scheduled-scripts/:scriptId',
      params: { fields : '*(*)' }
    },
    getLibraries: {
      method: 'GET',
      url: '/admin/applications/:appId/resources/scripts/libraries/:scriptId',
      params: { fields : '*(*)' }
    }
  });
});
