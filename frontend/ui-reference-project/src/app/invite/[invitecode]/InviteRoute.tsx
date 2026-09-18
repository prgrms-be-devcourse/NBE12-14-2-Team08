'use client';

import { fromRouteId } from '../../../lib/routeIds';
import { RouteStateSync } from '../../RouteStateSync';
import { InvitePage } from '../../../views/InvitePage';

export function InviteRoute({ invitecode }: { invitecode: string }) {
  return (
    <>
      <RouteStateSync page="invite" roomId={fromRouteId(invitecode, 'room')} />
      <InvitePage />
    </>
  );
}
