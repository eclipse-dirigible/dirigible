document.addEventListener('alpine:init', () => {
    if (window === top) {
        const _http = new HttpApi();
        const defaultLanguage = 'en-US';
        const storageKey = `${getBrandingInfo().prefix}.${getConfigData().id}.locale.language`;
        let savedLanguage = localStorage.getItem(storageKey);
        if (!savedLanguage) {
            localStorage.setItem(storageKey, defaultLanguage);
            savedLanguage = defaultLanguage;
        }
        Alpine.store('translations', {
            lang: savedLanguage,
            loading: true,
            namespaces: [],
            t(key, options = {}) {
                return i18next.t(key, options);
            },
            async changeLang(lang) {
                await i18next.changeLanguage(lang);
                this.lang = lang;
            },
            init() {
                _http.fetch('/services/js/platform-core/extension-services/locales.js', {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/json',
                    },
                    params: {
                        extensionPoints: ['platform-locales', 'application-locales']
                    }
                }).then((response) => {
                    return response.json();
                }).then((localeData) => {
                    i18next['locales'] = localeData.locales;
                    i18next.init({
                        lng: savedLanguage,
                        fallbackLng: this.defaultLanguage,
                        load: 'currentOnly',
                        debug: false,
                        defaultNS: 'common',
                        interpolation: {
                            skipOnVariables: false
                        },
                        resources: localeData.translations
                    }).then((_, err) => {
                        if (err) console.error(err);
                        this.loading = false;
                    });
                }).catch((error) => {
                    console.error(error);
                });
            },
        });
        top._translations = Alpine.store('translations');
    }
    Alpine.magic('t', () => {
        return (key, options = {}) => {
            top._translations.lang;
            top._translations.loading;

            return top._translations.t(key, options);
        };
    });
}, { once: true });