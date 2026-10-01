export const toRouteId = (id: string) => id.match(/\d+$/)?.[0] || id;

export const fromRouteId = (id: string, prefix: string) =>
  /^\d+$/.test(id) ? `${prefix}-${id}` : id;
