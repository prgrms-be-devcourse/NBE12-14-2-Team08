'use client';

import { fromRouteId } from '../../../../../lib/routeIds';
import { RouteStateSync } from '../../../../RouteStateSync';
import { AdminPage } from '../../../../../views/AdminPage';

export function AdminRoute({ groupId }: { groupId: string }) {
  return (
    <>
      <RouteStateSync page="admin" roomId={fromRouteId(groupId, 'room')} />
      <AdminPage />
    </>
  );
}
