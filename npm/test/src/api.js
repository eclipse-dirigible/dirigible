// Thin REST client over the generated Java controllers described by the manifest.
export function makeApi(request, manifest) {
  const url = (entity, suffix = '') => manifest.restBase + entity.api + suffix;

  async function asJson(response) {
    if (!response.ok()) {
      throw new Error(`${response.request().method()} ${response.url()} -> ${response.status()} ${await response.text()}`);
    }
    const text = await response.text();
    return text ? JSON.parse(text) : undefined;
  }

  return {
    list: (entity, limit = 20) => request.get(url(entity, `?$limit=${limit}`)).then(asJson),
    count: (entity) => request.get(url(entity, '/count')).then(asJson).then((body) => (typeof body === 'number' ? body : body.count)),
    get: (entity, id) => request.get(url(entity, '/' + id)).then(asJson),
    getResponse: (entity, id) => request.get(url(entity, '/' + id)),
    create: (entity, data) => request.post(url(entity), { data }).then(asJson),
    update: (entity, id, data) => request.put(url(entity, '/' + id), { data }).then(asJson),
    remove: async (entity, id) => {
      const response = await request.delete(url(entity, '/' + id));
      if (!response.ok()) {
        throw new Error(`DELETE ${response.url()} -> ${response.status()} ${await response.text()}`);
      }
    },
  };
}
