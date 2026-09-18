'use client';

import { useApp } from '../../../context/AppContext';
import { fromRouteId } from '../../../lib/routeIds';
import { RouteStateSync } from '../../RouteStateSync';
import { RoomDetailPage } from '../../../views/RoomDetailPage';
import { SettlementPage } from '../../../views/SettlementPage';

export function GroupPage({ roomId }: { roomId: string }) {
  const { currentPage } = useApp();
  const internalRoomId = fromRouteId(roomId, 'room');

  return (
    <>
      <RouteStateSync page={currentPage === 'settlement' ? 'settlement' : 'room'} roomId={internalRoomId} />
      {currentPage === 'settlement' ? <SettlementPage /> : <RoomDetailPage />}
    </>
  );
}
