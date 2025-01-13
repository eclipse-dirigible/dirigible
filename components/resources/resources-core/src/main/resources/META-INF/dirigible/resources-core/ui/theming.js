angular.module('ideTheming', ['ngResource', 'ideMessageHub'])
    .provider('theming', function ThemingProvider() {
        this.$get = ['$resource', '$http', 'messageHub', function editorsFactory($resource, $http, messageHub) {
            let theme = JSON.parse(localStorage.getItem('DIRIGIBLE.theme') || '{}');
            // legacySwitcher is deprecated. Remove once all views have been migrated.
            let legacySwitcher = $resource('/public/js/theme/resources.js?name=:themeId', { themeId: 'default' });
            let themes = [];

            function processThemeResponse(response) {
                themes = response.data;
                if (!theme.version) {
                    setTheme("quartz-light");
                } else {
                    for (let i = 0; i < themes.length; i++) {
                        if (themes[i].id === theme.id) {
                            if (themes[i].version !== theme.version) {
                                setThemeObject(themes[i]);
                                break;
                            }
                        }
                    }
                }
                messageHub.triggerEvent("ide.themesLoaded", true);
            }

            $http.get('/public/js/theme/resources.js/themes?legacy=false')
                .then(function (response) {
                    processThemeResponse(response);
                }, function (response) {
                    console.error("ide-theming: could not get themes", response);
                    if (response.status === 404) {
                        $http.get('/services/js/theme/resources.js/themes?legacy=false')
                            .then(function (response) {
                                processThemeResponse(response);
                            }, function (response) {
                                console.error("ide-theming: could not get themes", response);
                            });
                    }
                });

            function setTheme(themeId, sendEvent = true) {
                for (let i = 0; i < themes.length; i++) {
                    if (themes[i].id === themeId) {
                        setThemeObject(themes[i], sendEvent);
                    }
                }
            }

            function setThemeObject(themeObj, sendEvent = true) {
                localStorage.setItem(
                    'DIRIGIBLE.theme',
                    JSON.stringify(themeObj),
                )
                theme = themeObj;
                // legacySwitcher is deprecated. Remove once all views have been migrated.
                if (themeObj.oldThemeId) legacySwitcher.get({ 'themeId': themeObj.oldThemeId });
                if (sendEvent) messageHub.triggerEvent("ide.themeChange", true);
            }

            return {
                setTheme: setTheme,
                getThemes: function () {
                    return themes.map(
                        function (item) {
                            return {
                                "id": item["id"],
                                "name": item["name"]
                            };
                        }
                    );
                },
                getCurrentTheme: function () {
                    return {
                        id: theme["id"] || 'quartz-light',
                        name: theme["name"] || 'Quartz Light',
                    };
                },
                reset: function () {
                    // setting sendEvent to false because of the reload caused by Golden Layout
                    setTheme("quartz-light", false);
                }
            }
        }];
    })
    .factory('Theme', ['theming', function (_theming) { // Must be injected to set defaults
        let theme = JSON.parse(localStorage.getItem('DIRIGIBLE.theme') || '{}');
        return {
            reload: function () {
                theme = JSON.parse(localStorage.getItem('DIRIGIBLE.theme') || '{}');
            },
            getLinks: function () {
                return theme.links || [];
            },
            getType: function () {
                return theme.type || 'light';
            }
        }
    }]).directive('theme', ['Theme', 'messageHub', '$document', function (Theme, messageHub, $document) {
        return {
            restrict: 'E',
            replace: true,
            transclude: false,
            link: function (scope) {
                scope.links = Theme.getLinks();
                if (Theme.getType() === 'dark') {
                    $document[0].body.classList.add("dg-dark");
                } else $document[0].body.classList.add("dg-light");
                messageHub.onDidReceiveMessage(
                    'ide.themeChange',
                    function () {
                        scope.$apply(function () {
                            Theme.reload();
                            scope.links = Theme.getLinks();
                            if (Theme.getType() === 'dark') {
                                $document[0].body.classList.add("dg-dark");
                                $document[0].body.classList.remove("dg-light");
                            } else {
                                $document[0].body.classList.add("dg-light");
                                $document[0].body.classList.remove("dg-dark");
                            }
                        });
                    },
                    true
                );
            },
            template: '<link type="text/css" rel="stylesheet" ng-repeat="link in links" ng-href="{{ link }}">'
        };
    }]);