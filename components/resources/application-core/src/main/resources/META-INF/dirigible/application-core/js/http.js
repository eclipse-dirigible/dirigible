class HttpApi {
    constructor({ retries = 0, retryDelay = 300, retryOn = [408, 429, 500, 502, 503, 504] } = {}) {
        this.retries = retries;
        this.retryDelay = retryDelay;
        this.retryOn = retryOn;
        this.csrfToken = null;
    }

    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    async fetch(url, options = {}) {
        const headers = new Headers(options.headers || {});
        if (options.hasOwnProperty('params') && typeof options.params === 'object') {
            let qp = new URLSearchParams();
            for (const [key, value] of Object.entries(options.params)) {
                if (Array.isArray(value)) {
                    for (const val of value) {
                        qp.append(key, val);
                    }
                } else {
                    qp.set(key, value);
                }
            }
            url += `?${qp.toString()}`;
            delete options.params;
        }

        if (this.csrfToken) {
            headers.set('X-Requested-With', 'Fetch');
            headers.set('X-CSRF-Token', this.csrfToken ? this.csrfToken : 'Fetch');
        }

        const attemptFetch = async (attempt) => {
            try {
                const response = await fetch(url, {
                    ...options,
                    headers,
                    credentials: 'include',
                });

                if (!response.ok && this.retryOn.includes(response.status) && attempt < this.retries) {
                    await this.delay(this.retryDelay * (attempt + 1));
                    return attemptFetch(attempt + 1);
                }

                const newToken = response.headers.get('X-CSRF-Token');
                if (newToken) {
                    this.csrfToken = newToken;
                }

                return response;
            } catch (err) {
                if (attempt < this.retries) {
                    await this.delay(this.retryDelay * (attempt + 1));
                    return attemptFetch(attempt + 1);
                }
                throw err;
            }
        };

        return attemptFetch(0);
    }
}